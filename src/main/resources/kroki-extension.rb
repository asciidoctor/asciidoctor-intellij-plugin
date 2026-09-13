# frozen_string_literal: true
# Original source: the asciidoctor-kroki Ruby gem, version 2.0.0.rc.3
# https://github.com/asciidoctor/asciidoctor-kroki/tree/v2.0.0-rc.3/ruby/lib/asciidoctor/extensions/asciidoctor_kroki
#
# This file concatenates the gem's version.rb, preprocess.rb, cache.rb and extension.rb (the
# `require_relative` lines dropped) and appends the extension registration, so the plugin can load
# it as a single script. Local changes are limited to this header, the "-intellij" suffix on the
# HTTP referer, and the registration block at the end (the gem registers in asciidoctor_kroki.rb). To update: re-run the concatenation from the
# gem sources and re-apply those three changes.

module Asciidoctor
  module AsciidoctorKroki
    VERSION = '2.0.0.rc.3'
  end
end

require 'pathname'
require 'uri'

module AsciidoctorExtensions
  # Resolves PlantUML `!include` directives (and friends) in diagram text before it is sent to
  # the Kroki server, mirroring the JavaScript/Node.js extension's preprocessor (src/preprocess.js)
  # so both language bindings behave the same way. See http://plantuml.com/en/preprocessing.
  #
  # Unlike the JS extension (which also supports Antora resource IDs and a pluggable virtual
  # filesystem), this Ruby port only needs to resolve plain local paths and http(s) URLs, since
  # the Ruby gem is not used with Antora.
  # rubocop:disable Metrics/ModuleLength
  module PlantUmlPreprocessor
    PLANTUML_BLOCK_RX = /@startuml\r?\n([\s\S]*?)\r?\n@enduml/.freeze
    INCLUDE_LINE_RX = /^\s*!(include(?:_many|_once|url|sub)?)\s+(.*)/.freeze

    class << self
      # @param diagram_text [String] the raw diagram source
      # @param resource_path [String, nil] absolute path (or URL) of the file the diagram text was
      #   read from, used to resolve relative !include directives; nil for a diagram written
      #   directly in the AsciiDoc source (resolved relative to the current working directory,
      #   like plain !include paths without a match in kroki-plantuml-include-paths)
      # @param include_paths [String, nil] the kroki-plantuml-include-paths attribute value
      #   (a list of directories separated by File::PATH_SEPARATOR)
      # @param logger [#info] logger used to report includes that were skipped, not raised
      # @return [String] the diagram text with !include directives resolved
      def preprocess(diagram_text, resource_path, include_paths, logger)
        resource = resource_path ? parse_resource(resource_path) : { dir: '', path: nil }
        paths = include_paths.to_s.empty? ? [] : include_paths.split(::File::PATH_SEPARATOR)
        diagram_text = preprocess_includes(diagram_text, resource, [], [], paths, logger)
        remove_tags(diagram_text)
      end

      private

      def preprocess_includes(diagram_text, resource, include_once, include_stack, include_paths, logger)
        inside_comment_block = false
        # -1 keeps trailing empty segments (a diagram ending in \n must not lose it), matching
        # JavaScript's String#split, which (unlike Ruby's default) never drops trailing empties.
        lines = diagram_text.split("\n", -1).map do |line|
          result = line
          if !inside_comment_block && (match = INCLUDE_LINE_RX.match(line))
            substituted = process_include_line(match, resource, include_once, include_stack, include_paths, logger)
            result = substituted unless substituted.nil?
          end
          inside_comment_block = true if line.include?("/'")
          inside_comment_block = false if inside_comment_block && line.include?("'/")
          result
        end
        lines.join("\n")
      end

      def process_include_line(match, resource, include_once, include_stack, include_paths, logger)
        include_directive = match[1].downcase
        target = parse_target(match[2])
        # Intentionally unlimited split (matches the JS split(!) semantics): a second '!' beyond
        # the sub name is silently discarded, same as upstream.
        url_sub = target[:url].split('!')
        trailing_content = target[:comment]
        url = url_sub[0].gsub('\\ ', ' ').sub(/\s+\z/, '')
        sub = url_sub[1]

        read_result = read_include(url, resource, include_paths, include_stack, logger)
        return nil if read_result[:skip]

        check_include_once(read_result[:file_path], include_once) if include_directive == 'include_once'

        text = extract_text(read_result[:text], include_directive, sub)
        include_stack.push(read_result[:file_path])
        text = preprocess_includes(text, parse_resource(read_result[:file_path]), include_once, include_stack, include_paths, logger)
        include_stack.pop

        trailing_content.empty? ? text : "#{text} #{trailing_content}"
      end

      def extract_text(text, include_directive, sub)
        return text_or_first_block(text) if sub.nil? || sub.empty?
        return text_from_sub(text, sub) if include_directive == 'includesub'

        index = /\A\d+\z/.match?(sub) ? sub.to_i : nil
        index ? text_from_index(text, index) : text_from_id(text, sub)
      end

      # Splits the rest of an !include line into the target path/URL and any trailing PlantUML
      # comment (# line comment or /' block comment), which must be preserved verbatim.
      def parse_target(value)
        (3...value.length).each do |i|
          char = value[i]
          return { url: value[0...(i - 1)].strip, comment: value[i..] } if char == '#' && value[i - 1] == ' ' && value[i - 2] != '\\'
          return { url: value[0...(i - 1)].strip, comment: value[(i - 1)..] } if char == "'" && value[i - 1] == '/' && value[i - 2] != '\\'
        end
        { url: value, comment: '' }
      end

      def read_include(url, resource, include_paths, include_stack, logger)
        if url.start_with?('<')
          # A standard library include cannot be resolved locally but might be resolved by the Kroki server.
          logger.info("Skipping preprocessing of PlantUML standard library include '#{url}'")
          return { skip: true, text: '', file_path: url }
        end
        raise "Preprocessing of PlantUML include failed, because recursive reading already included referenced file '#{url}'" if include_stack.include?(url)

        if remote_url?(url)
          read_and_rescue(url, url, 'remote', logger)
        else
          file_path = resolve_include_file(url, resource, include_paths)
          raise "Preprocessing of PlantUML include failed, because recursive reading already included referenced file '#{file_path}'" if include_stack.include?(file_path)

          read_and_rescue(file_path, file_path, 'local', logger)
        end
      end

      def read_and_rescue(path, file_path, kind, logger)
        { skip: false, text: read_resource(path), file_path: file_path }
      rescue StandardError => e
        # Includes a file that cannot be found but might be resolved by the Kroki server (see #60).
        logger.info("Skipping preprocessing of PlantUML include, because reading the referenced #{kind} file '#{file_path}' caused an error:\n#{e}")
        { skip: true, text: '', file_path: file_path }
      end

      def resolve_include_file(include_file, resource, include_paths)
        # When the including file was itself fetched from a remote URL, resolve a relative
        # include against that URL so it can be fetched remotely as well, instead of being
        # (incorrectly) looked up on the local file system.
        return ::URI.join(resource[:path], include_file).to_s if resource[:path].is_a?(::String) && remote_url?(resource[:path])

        ([resource[:dir]] + include_paths).each do |dir|
          candidate = join_paths(dir, include_file)
          return candidate if ::File.exist?(candidate)
        end
        include_file
      end

      def check_include_once(file_path, include_once)
        if include_once.include?(file_path)
          raise "Preprocessing of PlantUML include failed, because including multiple times referenced file '#{file_path}' with '!include_once' guard"
        end

        include_once.push(file_path)
      end

      def text_from_sub(text, sub)
        regex = /!startsub\s+#{::Regexp.escape(sub)}(?:\r\n|\n)([\s\S]*?)(?:\r\n|\n)!endsub/
        text.scan(regex).flatten.join("\n")
      end

      def text_from_id(text, id)
        regex = /@startuml\(id=#{::Regexp.escape(id)}\)(?:\r\n|\n)([\s\S]*?)(?:\r\n|\n)@enduml/
        text.scan(regex).flatten.join("\n")
      end

      def text_from_index(text, index)
        blocks = text.scan(PLANTUML_BLOCK_RX).flatten
        blocks[index] || ''
      end

      def text_or_first_block(text)
        match = PLANTUML_BLOCK_RX.match(text)
        match ? match[1] : text
      end

      # Removes all plantuml tags (@startuml/@enduml) from the diagram. It's possible to have more
      # than one diagram in a single file in the cli version of plantuml. This does not work for
      # the server, so recent plantuml versions remove the tags before processing the diagram. We
      # don't want to rely on the server to handle this, so we remove the tags here before sending
      # the diagram to the server.
      #
      # Some diagrams have special tags (i.e. @startmindmap for mindmap) - these are mandatory, so
      # we can't do much about them.
      def remove_tags(diagram_text)
        return diagram_text if diagram_text.nil? || diagram_text.empty?

        diagram_text.gsub(/^\s*@(?:startuml|enduml).*\n?/, '')
      end

      def remote_url?(str)
        str.start_with?('http://', 'https://')
      end

      def read_resource(path)
        if remote_url?(path)
          require 'open-uri'
          ::OpenURI.open_uri(path, &:read)
        else
          ::File.read(path, mode: 'rb:utf-8:utf-8')
        end
      end

      def parse_resource(file_path)
        if remote_url?(file_path)
          { dir: nil, path: file_path }
        else
          { dir: ::File.dirname(file_path), path: file_path }
        end
      end

      # Joins path segments the way Node's path.posix.join does: empty segments contribute
      # nothing (so a blank resource dir doesn't force everything into an absolute path), and the
      # result is lexically normalized (. and .. segments resolved) so that two different textual
      # routes to the same file compare equal for cycle detection.
      def join_paths(*parts)
        segments = parts.compact.map(&:to_s).reject(&:empty?)
        return '' if segments.empty?

        ::Pathname.new(segments.join('/')).cleanpath.to_s
      end
    end
  end
  # rubocop:enable Metrics/ModuleLength
end

require 'digest'
require 'fileutils'
require 'json'

module AsciidoctorExtensions
  # Persistent, content-addressed cache for fetched diagrams, independent of the output
  # directory. Ports src/cache.js so both language bindings behave the same way: the cache
  # key is derived from the diagram request itself (server URL, type, format, encoded source,
  # options), not from the output file name, so it also survives builds that wipe the output
  # directory between runs (e.g. Antora) and correctly detects unchanged content for diagrams
  # with a stable, user-defined name (see #90, #113).
  module KrokiCache
    VALID_CACHE_MODES = ['', 'true', 'false', 'refresh'].freeze

    class << self
      # Resolves the persistent cache directory.
      # Uses the `kroki-cache-dir` attribute when set; otherwise defaults to the XDG cache
      # directory (`$XDG_CACHE_HOME/kroki` or `~/.cache/kroki`).
      def resolve_cache_dir(doc)
        configured = doc.attr('kroki-cache-dir')
        return configured if configured && !configured.empty?

        xdg_cache_home = ENV['XDG_CACHE_HOME'] || File.join(Dir.home, '.cache')
        File.join(xdg_cache_home, 'kroki')
      end

      # Resolves the `kroki-cache` attribute into a cache mode.
      #
      # Recognised values are: unset (defaults to enabled), `` (set with no value, e.g.
      # `:kroki-cache:`) and `true` (both enabled), `false` (disabled), and `refresh` (enabled,
      # but bypasses cached reads and re-fetches + updates the cache). Any other value is
      # invalid: it is logged and treated as if unset.
      #
      # @return [Hash{Symbol => Boolean}] with keys :enabled and :refresh
      def resolve_cache_mode(doc, logger)
        raw = doc.attr('kroki-cache')
        return { enabled: true, refresh: false } if raw.nil?

        value = raw.to_s.strip.downcase
        unless VALID_CACHE_MODES.include?(value)
          logger.warn "Invalid value '#{raw}' for kroki-cache attribute. The value must be either: " \
                      "'true', 'false' or 'refresh'. Proceeding using: 'true'."
          return { enabled: true, refresh: false }
        end
        { enabled: value != 'false', refresh: value == 'refresh' }
      end

      # Computes the content-addressed cache key for a diagram.
      #
      # Deliberately host-dependent: the server URL is part of the key because two Kroki
      # servers are not guaranteed to render the same source identically (they may run
      # different versions of the underlying diagram libraries). Options are sorted so the
      # key does not depend on their insertion order.
      def content_key(kroki_diagram, server_url)
        sorted_opts = kroki_diagram.opts.sort_by { |k, _| k.to_s }
        material = [server_url, kroki_diagram.type, kroki_diagram.format, kroki_diagram.encode, sorted_opts.to_json].join('/')
        Digest::SHA256.hexdigest(material)
      end

      # Whether a diagram is already present in the cache.
      def exists_in_cache?(cache_dir, key, format)
        File.exist?(cache_file_path(cache_dir, key, format))
      end

      # Reads a cached diagram.
      def read_from_cache(cache_dir, key, format)
        File.read(cache_file_path(cache_dir, key, format), mode: 'rb')
      end

      # Writes a diagram to the cache, creating the cache directory if needed.
      def write_to_cache(cache_dir, key, format, contents)
        FileUtils.mkdir_p(cache_dir)
        File.write(cache_file_path(cache_dir, key, format), contents, mode: 'wb')
      end

      private

      def cache_file_path(cache_dir, key, format)
        File.join(cache_dir, "#{key}.#{format}")
      end
    end
  end
end

require 'cgi'
require 'pathname'
require 'asciidoctor/extensions' unless RUBY_ENGINE == 'opal'

# Asciidoctor extensions
#
module AsciidoctorExtensions
  include Asciidoctor

  # A block extension that converts a diagram into an image.
  #
  class KrokiBlockProcessor < Extensions::BlockProcessor
    include Asciidoctor::Logging
    use_dsl

    on_context :listing, :literal
    name_positional_attributes 'target', 'format'

    # @param name [String] name of the block macro (optional)
    # @param config [Hash] a config hash (optional)
    #   - :logger a logger used to log warning and errors (optional)
    #
    def initialize(name = nil, config = {})
      @logger = (config || {}).delete(:logger) { ::Asciidoctor::LoggerManager.logger }
      super
    end

    def process(parent, reader, attrs)
      diagram_type = @name
      role = attrs['role']
      source_location = reader.cursor
      diagram_text = reader.string
      KrokiProcessor.process(self, parent, attrs, diagram_type, diagram_text, @logger)
    rescue => e # rubocop:disable Style/RescueStandardError
      # Matches the JavaScript/Node.js extension: a failure talking to the Kroki server (network
      # error, non-2xx response, unexpected content-type) shouldn't abort the whole document
      # conversion, so it's degraded to a warning and the raw diagram source is kept visible.
      logger.warn message_with_context "Skipping #{diagram_type} block: #{e.message}", source_location: source_location
      attrs['role'] = role ? "#{role} kroki-error" : 'kroki-error'
      create_block(parent, attrs['cloaked-context'], diagram_text, attrs)
    end

    protected

    attr_reader :logger
  end

  # A block macro extension that converts a diagram into an image.
  #
  class KrokiBlockMacroProcessor < Asciidoctor::Extensions::BlockMacroProcessor
    include Asciidoctor::Logging
    use_dsl

    name_positional_attributes 'format'

    # @param name [String] name of the block macro (optional)
    # @param config [Hash] a config hash (optional)
    #   - :logger a logger used to log warning and errors (optional)
    #
    def initialize(name = nil, config = {})
      @logger = (config || {}).delete(:logger) { ::Asciidoctor::LoggerManager.logger }
      super
    end

    # Processes the diagram block or block macro by converting it into an image or literal block.
    #
    # @param parent [Asciidoctor::AbstractBlock] the parent asciidoc block of the block or block macro being processed
    # @param target [String] the target value of a block macro
    # @param attrs [Hash] the attributes of the block or block macro
    # @return [Asciidoctor::AbstractBlock] a new block that replaces the original block or block macro
    # rubocop:disable Metrics/AbcSize
    def process(parent, target, attrs)
      diagram_type = @name
      role = attrs['role']
      target = parent.apply_subs(target, [:attributes])

      unless read_allowed?(target)
        link = create_inline(parent, :anchor, target, type: :link, target: target)
        return create_block(parent, :paragraph, link.convert, {}, content_model: :raw)
      end

      unless (path = resolve_target_path(parent, target))
        logger.error message_with_context "#{diagram_type} block macro not found: #{target}.", source_location: parent.document.reader.cursor_at_mark
        return create_block(parent, 'paragraph', unresolved_block_macro_message(diagram_type, target), {})
      end

      begin
        diagram_text = read(path)
      rescue => e # rubocop:disable Style/RescueStandardError
        logger.error message_with_context "Failed to read #{diagram_type} file: #{path}. #{e}.", source_location: parent.document.reader.cursor_at_mark
        return create_block(parent, 'paragraph', unresolved_block_macro_message(diagram_type, path), {})
      end
      begin
        KrokiProcessor.process(self, parent, attrs, diagram_type, diagram_text, @logger, resource_path: path)
      rescue => e # rubocop:disable Style/RescueStandardError
        # Matches the JavaScript/Node.js extension: a failure talking to the Kroki server
        # shouldn't abort the whole document conversion, so it's degraded to a warning instead.
        logger.warn message_with_context "Skipping #{diagram_type} block: #{e.message}", source_location: parent.document.reader.cursor_at_mark
        attrs['role'] = role ? "#{role} kroki-error" : 'kroki-error'
        create_block(parent, 'paragraph', "#{e.message} - #{diagram_type}::#{target}[]", attrs)
      end
    end
    # rubocop:enable Metrics/AbcSize

    protected

    attr_reader :logger

    # @param parent [Asciidoctor::AbstractBlock] the parent asciidoc block of the block or block macro being processed
    # @param target [String] the target value of a block macro
    def resolve_target_path(parent, target)
      parent.normalize_system_path(target)
    end

    def read_allowed?(_target)
      true
    end

    def read(target)
      if target.start_with?('http://') || target.start_with?('https://')
        require 'open-uri'
        ::OpenURI.open_uri(target, &:read)
      else
        File.read(target, mode: 'rb:utf-8:utf-8')
      end
    end

    def unresolved_block_macro_message(name, target)
      "Unresolved block macro - #{name}::#{target}[]"
    end
  end

  # Kroki API
  #
  module Kroki
    SUPPORTED_DIAGRAM_NAMES = %w[
      actdiag
      blockdiag
      bpmn
      bytefield
      c4plantuml
      d2
      dbml
      ditaa
      erd
      excalidraw
      goat
      graphviz
      mermaid
      nomnoml
      nwdiag
      packetdiag
      pikchr
      plantuml
      rackdiag
      seqdiag
      svgbob
      symbolator
      tikz
      umlet
      vega
      vegalite
      wavedrom
      structurizr
      diagramsnet
      wireviz
    ].freeze
  end

  # Internal processor
  #
  class KrokiProcessor
    include Asciidoctor::Logging

    TEXT_FORMATS = %w[txt atxt utxt].freeze
    BUILTIN_ATTRIBUTES = %w[target width height format fallback link float align role caption title cloaked-context subs].freeze
    PLANTUML_TYPES = %i[plantuml c4plantuml].freeze

    class << self
      # rubocop:disable Metrics/AbcSize, Metrics/PerceivedComplexity
      def process(processor, parent, attrs, diagram_type, diagram_text, logger, resource_path: nil)
        doc = parent.document
        diagram_text = prepend_plantuml_config(diagram_text, diagram_type, doc, logger)
        diagram_text = preprocess_plantuml_includes(diagram_text, diagram_type, doc, resource_path, logger)
        # If "subs" attribute is specified, substitute accordingly.
        # Be careful not to specify "specialcharacters" or your diagram code won't be valid anymore!
        if (subs = attrs['subs'])
          diagram_text = parent.apply_subs(diagram_text, parent.resolve_subs(subs))
        end
        attrs.delete('opts')
        # Apply the option defined on the block/macro or, as a fallback, the document-wide kroki-default-options.
        if (option = get_option(attrs, doc)) && option != 'none'
          attrs["#{option}-option"] = ''
        end
        format = get_format(doc, attrs, diagram_type)
        attrs['role'] = get_role(format, attrs['role'])
        attrs['format'] = format
        opts = attrs.filter { |key, _| key.is_a?(String) && BUILTIN_ATTRIBUTES.none? { |k| key == k } && !key.end_with?('-option') }
        kroki_diagram = KrokiDiagram.new(diagram_type, format, diagram_text, attrs['target'], opts)
        kroki_client = KrokiClient.new({
                                         server_url: server_url(doc),
                                         http_method: http_method(doc),
                                         max_uri_length: max_uri_length(doc),
                                         source_location: doc.reader.cursor_at_mark,
                                         http_client: KrokiHttpClient
                                       }, logger)
        alt = get_alt(attrs)
        title = attrs.delete('title')
        caption = attrs.delete('caption')
        if TEXT_FORMATS.include?(format)
          text_content = kroki_client.text_content(kroki_diagram)
          block = processor.create_block(parent, 'literal', text_content, attrs)
        else
          attrs['alt'] = alt
          apply_image_src(attrs, create_image_src(doc, kroki_diagram, kroki_client, logger, inline: option == 'inline'))
          block = processor.create_image_block(parent, attrs)
        end
        block.title = title if title
        block.assign_caption(caption, 'figure')
        block
      end
      # rubocop:enable Metrics/AbcSize, Metrics/PerceivedComplexity

      private

      # Prepends the kroki-plantuml-include file content to the diagram text. Unlike the
      # !include directives resolved by preprocess_plantuml_includes below, this path is jailed
      # to the document's safe-mode boundaries via normalize_system_path, since the attribute
      # value (unlike an !include target written by the diagram's author) may come from outside
      # the diagram itself (e.g. a document-wide default set by a build script).
      def prepend_plantuml_config(diagram_text, diagram_type, doc, logger)
        if PLANTUML_TYPES.include?(diagram_type) && doc.safe < ::Asciidoctor::SafeMode::SECURE && doc.attr?('kroki-plantuml-include')
          plantuml_include_path = doc.normalize_system_path(doc.attr('kroki-plantuml-include'))
          if ::File.readable? plantuml_include_path
            config = File.read(plantuml_include_path)
            diagram_text = "#{config}\n#{diagram_text}"
          else
            logger.warn message_with_context "Unable to read plantuml-include. File not found or not readable: #{plantuml_include_path}.",
                                             source_location: doc.reader.cursor_at_mark
          end
        end
        diagram_text
      end

      # Resolves !include/!include_once/!include_many/!includeurl/!includesub directives found in
      # PlantUML/C4-PlantUML diagram text (including text prepended above), searching resource_path's
      # own directory and then each directory in kroki-plantuml-include-paths, in order. Unlike
      # kroki-plantuml-include above, this mirrors the JavaScript extension's preprocessor and is
      # not jailed to the safe-mode boundary (see src/preprocess.js) — only gated by safe mode itself.
      def preprocess_plantuml_includes(diagram_text, diagram_type, doc, resource_path, logger)
        return diagram_text unless PLANTUML_TYPES.include?(diagram_type) && doc.safe < ::Asciidoctor::SafeMode::SECURE

        include_paths = doc.attr('kroki-plantuml-include-paths')
        PlantUmlPreprocessor.preprocess(diagram_text, resource_path, include_paths, logger)
      end

      def get_alt(attrs)
        if (title = attrs['title'])
          title
        elsif (target = attrs['target'])
          target
        else
          'Diagram'
        end
      end

      def get_role(format, role)
        if role
          if format
            "#{role} kroki-format-#{format} kroki"
          else
            "#{role} kroki"
          end
        else
          'kroki'
        end
      end

      # Get the option defined on the block or macro.
      #
      # First, check if an option is defined as an attribute (e.g. opts=inline).
      # If there is no match, fall back to the document-wide kroki-default-options attribute.
      #
      # @param attrs [Hash] the block or macro attributes
      # @param doc [Asciidoctor::Document] the Asciidoctor document
      # @return [String, nil] the option name (inline, interactive or none) or nil
      def get_option(attrs, doc)
        available_options = %w[inline interactive none]
        available_options.find { |option| attrs["#{option}-option"] == '' } ||
          available_options.find { |option| doc.attr('kroki-default-options') == option }
      end

      def get_format(doc, attrs, diagram_type)
        format = attrs['format'] || doc.attr('kroki-default-format') || 'svg'
        if format == 'png'
          # redirect PNG format to SVG if the diagram library only supports SVG as output format.
          # this is useful when the default format has been set to PNG
          # Currently, goat, nomnoml, svgbob, wavedrom only support SVG as output format.
          svg_only_diagram_types = %i[goat nomnoml svgbob wavedrom]
          format = 'svg' if svg_only_diagram_types.include?(diagram_type)
        end
        format
      end

      def apply_image_src(attrs, image_src)
        attrs['target'] = image_src[:target]
        attrs['imagesdir'] = image_src[:imagesdir] if image_src[:imagesdir]
      end

      def create_image_src(doc, kroki_diagram, kroki_client, logger, inline: false)
        if doc.attr('kroki-fetch-diagram') && doc.safe < ::Asciidoctor::SafeMode::SECURE
          # In data-URI mode no file is written, so the file name is irrelevant: embed the diagram inline.
          return { target: kroki_diagram.to_data_uri(kroki_client) } if doc.attr?('data-uri') || doc.attr?('kroki-data-uri')

          images_output_dir = output_dir_path(doc)
          diagram_name = kroki_diagram.save(images_output_dir, kroki_client, generated_files(doc), logger,
                                            cache_dir: KrokiCache.resolve_cache_dir(doc), cache_mode: KrokiCache.resolve_cache_mode(doc, logger))
          # The converter resolves the image target against the document's `imagesdir`
          # attribute, which only matches where we actually wrote the file when
          # `imagesoutdir` is unset. Overriding `imagesdir` on this image node (rather
          # than on the document) tells the converter exactly where to find this one
          # file, without disturbing other images in the document (asciidoctor/asciidoctor#3660).
          # As of this writing that core change is merged to `main` but not yet in a
          # released gem (still absent from 2.0.26); until it ships, Asciidoctor's
          # `image_uri` ignores the node-level attribute and this is a harmless no-op.
          { target: diagram_name, imagesdir: relative_images_dir(doc, images_output_dir) }
        elsif inline && !doc.attr?('allow-uri-read') && doc.safe < ::Asciidoctor::SafeMode::SECURE
          # The `inline` option asks the converter to embed the diagram itself (e.g. inline SVG).
          # When `allow-uri-read` is set, core fetches the target itself, so we hand back the plain
          # server URL below and let it do so. When it's unset, core can't read a remote target at
          # all, so without help here it would silently render a blank placeholder instead of the
          # diagram — so we fetch it ourselves, narrowly, from the already-configured Kroki server
          # (never an arbitrary document-supplied URI), and hand back a data URI instead.
          #
          # NOTE: as of this writing, Asciidoctor core's HTML5 converter treats a `data:` URI target
          # the same as any other remote URI and still refuses to read it without `allow-uri-read`
          # (https://github.com/asciidoctor/asciidoctor/issues/3791) — so today this still renders a
          # blank placeholder, only later than before. A fix decoding `data:` targets directly is
          # proposed upstream (https://github.com/asciidoctor/asciidoctor/pull/4865, not yet merged);
          # this branch is aligned with it ahead of time, matching the JavaScript/Node.js extension
          # (already correct against Asciidoctor.js >= 4.0.2, which carries the equivalent fix), so
          # it starts working here too as soon as a released gem picks it up — no code change needed.
          { target: kroki_diagram.to_data_uri(kroki_client) }
        else
          { target: kroki_diagram.get_diagram_uri(server_url(doc)) }
        end
      end

      # Returns the per-document registry of generated file names, mapping each name
      # to the diagram URI it was generated from. Used to detect name clashes within
      # a single conversion.
      def generated_files(doc)
        doc.instance_variable_get(:@kroki_generated_files) ||
          doc.instance_variable_set(:@kroki_generated_files, {})
      end

      def server_url(doc)
        doc.attr('kroki-server-url', 'https://kroki.io')
      end

      def http_method(doc)
        doc.attr('kroki-http-method', 'adaptive').downcase
      end

      def max_uri_length(doc)
        Integer(doc.attr('kroki-max-uri-length', '4000'), exception: false) || 4000
      end

      def output_dir_path(doc)
        images_output_dir = doc.attr('imagesoutdir')
        return images_output_dir if images_output_dir

        File.join(output_dir(doc), doc.attr('imagesdir', ''))
      end

      # the nested document logic will become obsolete once https://github.com/asciidoctor/asciidoctor/commit/7edc9da023522be67b17e2a085d72e056703a438 is released
      def output_dir(doc)
        doc.attr('outdir') || (doc.nested? ? doc.parent_document : doc).options[:to_dir] || doc.base_dir
      end

      def relative_images_dir(doc, images_output_dir)
        from = Pathname.new(File.expand_path(output_dir(doc)))
        to = Pathname.new(File.expand_path(images_output_dir))
        to.relative_path_from(from).to_s
      end
    end
  end

  # Kroki diagram
  #
  class KrokiDiagram
    require 'fileutils'
    require 'zlib'
    require 'digest'

    attr_reader :type, :text, :format, :target, :opts

    def initialize(type, format, text, target = nil, opts = {})
      @text = text
      @type = type
      @format = format
      @target = target
      @opts = opts
    end

    def get_diagram_uri(server_url)
      query_params = opts.map { |k, v| "#{k}=#{_url_encode(v.to_s)}" }.join('&') unless opts.empty?
      _join_uri_segments(server_url, @type, @format, encode) + (query_params ? "?#{query_params}" : '')
    end

    def encode
      ([Zlib::Deflate.deflate(@text, 9)].pack 'm0').tr '+/', '-_'
    end

    # @param cache_dir [String, nil] persistent cache directory (see KrokiCache.resolve_cache_dir); required when cache_mode[:enabled]
    # @param cache_mode [Hash] {enabled:, refresh:} (see KrokiCache.resolve_cache_mode); disabled by default so callers that
    #   don't pass it (e.g. specs exercising #save directly) keep the pre-cache behaviour
    def save(output_dir_path, kroki_client, generated_files = nil, logger = nil, cache_dir: nil, cache_mode: { enabled: false, refresh: false })
      diagram_url = get_diagram_uri(kroki_client.server_url)
      # An explicit name is used verbatim so links stay stable across content changes;
      # otherwise the name is content-addressed so anonymous diagrams don't collide (see #451).
      named = @target.is_a?(::String) && !@target.empty?
      diagram_name = named ? "#{@target}.#{@format}" : "diag-#{Digest::SHA256.hexdigest diagram_url}.#{@format}"
      file_path = File.join(output_dir_path, diagram_name)
      if named
        # A stable file may exist from a previous build with stale content, so it cannot be
        # trusted by name alone: go through fetch_diagram, which re-fetches only when the
        # persistent cache doesn't already have this exact content (see #90). Warn when the
        # same name is reused for a diagram with different content.
        warn_on_name_clash(generated_files, diagram_name, diagram_url, logger)
        generated_files[diagram_name] = diagram_url if generated_files
        fetch_and_write(output_dir_path, file_path, kroki_client, cache_dir, cache_mode)
      elsif !File.exist?(file_path)
        # Content-addressed name: an existing output file necessarily has identical content.
        fetch_and_write(output_dir_path, file_path, kroki_client, cache_dir, cache_mode)
      end
      diagram_name
    end

    # Fetches this diagram from Kroki and returns it as a `data:` URI, embedding the
    # content directly without writing any file (mirrors the JavaScript extension's
    # fetch.js#toDataUri).
    def to_data_uri(kroki_client)
      contents = kroki_client.get_image(self, image_encoding)
      "data:#{media_type};base64,#{[contents].pack 'm0'}"
    end

    private

    def media_type
      case @format
      when 'txt', 'atxt', 'utxt'
        'text/plain; charset=utf-8'
      when 'svg'
        'image/svg+xml'
      else
        'image/png'
      end
    end

    def fetch_and_write(output_dir_path, file_path, kroki_client, cache_dir, cache_mode)
      contents = fetch_diagram(kroki_client, cache_dir, cache_mode)
      FileUtils.mkdir_p(output_dir_path)
      File.write(file_path, contents, mode: 'wb')
    end

    # Fetches from Kroki, going through the persistent cache (see cache.rb) when enabled. The
    # cache is keyed on the diagram's actual content, not on the output file name, so it also
    # survives builds that wipe the output directory (e.g. Antora) and correctly detects
    # unchanged content for named diagrams (see #90, #113).
    def fetch_diagram(kroki_client, cache_dir, cache_mode)
      return kroki_client.get_image(self, image_encoding) unless cache_mode[:enabled]

      key = KrokiCache.content_key(self, kroki_client.server_url)
      return KrokiCache.read_from_cache(cache_dir, key, @format) if !cache_mode[:refresh] && KrokiCache.exists_in_cache?(cache_dir, key, @format)

      fetched = kroki_client.get_image(self, image_encoding)
      KrokiCache.write_to_cache(cache_dir, key, @format, fetched)
      fetched
    end

    def warn_on_name_clash(generated_files, diagram_name, diagram_url, logger)
      return unless generated_files && logger

      previous_url = generated_files[diagram_name]
      return if previous_url.nil? || previous_url == diagram_url

      logger.warn "kroki: the diagram file name '#{diagram_name}' is generated by more than one diagram with different content; " \
                  'the file will be overwritten. Use unique names to keep stable links.'
    end

    def image_encoding
      case @format
      when 'txt', 'atxt', 'utxt', 'svg'
        'utf8'
      else
        'binary'
      end
    end

    def _url_encode(text)
      CGI.escape(text).gsub('+', '%20')
    end

    def _join_uri_segments(base, *uris)
      segments = []
      # remove trailing slashes
      segments.push(base.gsub(%r{/+$}, ''))
      segments.concat(uris.map do |uri|
        # remove leading and trailing slashes
        uri.to_s
          .gsub(%r{^/+}, '')
          .gsub(%r{/+$}, '')
      end)
      segments.join('/')
    end
  end

  # Kroki client
  #
  class KrokiClient
    include Asciidoctor::Logging

    attr_reader :server_url, :method, :max_uri_length

    SUPPORTED_HTTP_METHODS = %w[get post adaptive].freeze

    # Maps a diagram output format to its expected MIME type, mirroring the JavaScript/Node.js
    # extension's kroki-client.js. Used to detect a Kroki server response that doesn't match the
    # requested format (e.g. a plain-text syntax error returned for what should be an SVG).
    MIME_TYPES = {
      'svg' => 'image/svg+xml',
      'png' => 'image/png',
      'jpg' => 'image/jpeg',
      'jpeg' => 'image/jpeg',
      'pdf' => 'application/pdf',
      'txt' => 'text/plain',
      'atxt' => 'text/plain',
      'utxt' => 'text/plain',
      'base64' => 'text/plain'
    }.freeze

    def initialize(opts, logger = ::Asciidoctor::LoggerManager.logger)
      @server_url = opts[:server_url]
      @max_uri_length = opts.fetch(:max_uri_length, 4000)
      @http_client = opts[:http_client]
      @source_location = opts[:source_location]
      @logger = logger
      method = opts.fetch(:http_method, 'adaptive').downcase
      if SUPPORTED_HTTP_METHODS.include?(method)
        @method = method
      else
        logger.warn message_with_context "Invalid value '#{method}' for kroki-http-method attribute. The value must be either: " \
                                         "'get', 'post' or 'adaptive'. Proceeding using: 'adaptive'.",
                                         source_location: @source_location
        @method = 'adaptive'
      end
    end

    def text_content(kroki_diagram)
      get_image(kroki_diagram, 'utf-8')
    end

    def get_image(kroki_diagram, encoding)
      type = kroki_diagram.type
      format = kroki_diagram.format
      text = kroki_diagram.text
      opts = kroki_diagram.opts
      expected_content_type = MIME_TYPES[format]
      if @method == 'adaptive' || @method == 'get'
        uri = kroki_diagram.get_diagram_uri(server_url)
        if uri.length > @max_uri_length
          # The request URI is longer than the max URI length.
          if @method == 'get'
            # The server may reject the request with a 414 (URI Too Long).
            @logger.warn message_with_context "The diagram URI length (#{uri.length}) exceeds kroki-max-uri-length (#{@max_uri_length}). " \
                                              'The server may reject the request with a 414 (URI Too Long). Consider using the ' \
                                              "'kroki-http-method' attribute set to 'adaptive' or 'post'.",
                                              source_location: @source_location
            @http_client.get(uri, opts, encoding, expected_content_type)
          else
            @http_client.post("#{@server_url}/#{type}/#{format}", text, opts, encoding, expected_content_type)
          end
        else
          @http_client.get(uri, opts, encoding, expected_content_type)
        end
      else
        @http_client.post("#{@server_url}/#{type}/#{format}", text, opts, encoding, expected_content_type)
      end
    end
  end

  # Kroki HTTP client
  #
  class KrokiHttpClient
    require 'net/http'
    require 'uri'
    require 'json'

    class << self
      REFERER = "asciidoctor/kroki.rb/#{Asciidoctor::AsciidoctorKroki::VERSION}-intellij"

      def get(uri, opts, _encoding, expected_content_type = nil)
        parsed_uri = URI(uri)
        headers = opts.transform_keys { |key| "Kroki-Diagram-Options-#{key}" }
                      .merge({ 'referer' => REFERER })
        request = ::Net::HTTP::Get.new(parsed_uri, headers)
        response = ::Net::HTTP.start(
          parsed_uri.hostname,
          parsed_uri.port,
          use_ssl: (parsed_uri.scheme == 'https')
        ) do |http|
          http.request(request)
        end
        handle_response(response, 'GET', uri, expected_content_type)
      end

      def post(uri, data, opts, _encoding, expected_content_type = nil)
        headers = opts.transform_keys { |key| "Kroki-Diagram-Options-#{key}" }
                      .merge({
                               'Content-Type' => 'text/plain',
                               'referer' => REFERER
                             })
        response = ::Net::HTTP.post(
          URI(uri),
          data,
          headers
        )
        handle_response(response, 'POST', uri, expected_content_type)
      end

      private

      # Mirrors the JavaScript/Node.js extension's http-client.js: unlike Net::HTTP, which
      # returns whatever body the server sent regardless of status code, this raises a clear
      # error for a non-2xx response, an unexpected content-type (e.g. the Kroki server
      # returning a plain-text syntax error for what should be an SVG), or an empty body —
      # instead of silently treating the error message as if it were the diagram itself.
      def handle_response(response, method, uri, expected_content_type)
        if response.is_a?(::Net::HTTPSuccess)
          if expected_content_type
            content_type = (response['content-type'] || '').downcase
            unless content_type.start_with?(expected_content_type.downcase)
              raise "#{method} #{uri} - unexpected content-type; expected: #{expected_content_type}, got: #{content_type}"
            end
          end
          body = response.body
          raise "#{method} #{uri} - server returns an empty response" if body.nil? || body.empty?

          return body
        end
        if response.code == '414'
          raise "#{method} #{uri} - server returns 414 (URI Too Long). The diagram URI is too long for the server. " \
                "Consider using the 'kroki-http-method' attribute set to 'post' or 'adaptive' to send the diagram source via POST."
        end
        raise "#{method} #{uri} - server returns #{response.code} status code; response: #{response.body}"
      end
    end
  end
end

Asciidoctor::Extensions.register do
  ::AsciidoctorExtensions::Kroki::SUPPORTED_DIAGRAM_NAMES.each { |name|
    block_macro AsciidoctorExtensions::KrokiBlockMacroProcessor, name
    block AsciidoctorExtensions::KrokiBlockProcessor, name
  }
end
