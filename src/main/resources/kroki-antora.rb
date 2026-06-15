# frozen_string_literal: true
require 'java'

# Antora support for the Kroki extension (loaded right after kroki-extension.rb, only when Kroki is enabled).
#
# Asciidoctor / the Ruby Kroki extension do not understand Antora resource identifiers. This patch adds:
#   1. resolution of diagram block-macro targets, e.g.  plantuml::example$dir/file.puml[]
#   2. inlining of PlantUML !include / !includesub directives that reference Antora resources, e.g.
#        !include example$dir/layout.puml
#        !includesub example$dir/model.puml!SomeSub
#      (the Kroki server has no file-system access, so includes must be inlined before sending).
#
# Resolution of an Antora resource id to a local path is delegated to the Java side
# (AntoraReferenceAdapter#resolveAntoraResourcePath), which reuses the plugin's existing Antora
# module resolution. Outside an Antora module the resolver returns nil and behavior is unchanged.
#
# The include inlining is ported from the asciidoctor-kroki JavaScript extension (preprocess.js).
# See https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/516

module AsciidoctorExtensions
  # Raised when a PlantUML !include referencing an Antora resource cannot be safely inlined: the target
  # can't be resolved/read, an !include_once target repeats, or an include cycle is detected. It is
  # allowed to escape the defensive rescues below so the diagram fails loudly with a clear message —
  # matching the asciidoctor-kroki JS extension and the rest of the toolchain (the Antora build,
  # asciidoctor and standalone PlantUML all bail rather than render a broken diagram).
  class AntoraIncludeError < StandardError; end

  # Antora-aware PlantUML preprocessing helpers.
  module AntoraKroki
    PLANTUML_BLOCK_RX = /@startuml(?:\r?\n)([\s\S]*?)(?:\r?\n)@enduml/m
    INCLUDE_RX = /^\s*!(include(?:_many|_once|url|sub)?)\s+(.*)$/
    # Start of a trailing PlantUML comment in an !include line: an inline " #..." (introduced by a
    # space) or a block "/'...". A backslash before the marker escapes it (so it is not a comment).
    COMMENT_RX = %r{(?<!\\) #|(?<!\\)/'}

    module_function

    # Resolve an Antora resource id to an absolute local path, or nil if not resolvable / not a resource id.
    def resolve(target)
      org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.resolveAntoraResourcePath(target)
    rescue StandardError
      nil
    end

    def remote?(url)
      url.start_with?('http://') || url.start_with?('https://')
    end

    # An Antora resource id carries a family marker (e.g. example$, partial$, image$); plain PlantUML
    # includes (relative paths, stdlib) have none and must pass through untouched.
    def antora_resource_id?(target)
      target.include?('$')
    end

    # Inline all Antora-resource !include directives in a PlantUML diagram, then strip the @startuml/@enduml
    # tags (the Kroki server adds them back), mirroring the JS extension's preprocessPlantUML.
    def preprocess(diagram_text)
      remove_tags(process_includes(diagram_text, [], []))
    end

    def process_includes(diagram_text, include_once, include_stack)
      inside_comment = false
      diagram_text.each_line.map do |line|
        result = line
        unless inside_comment
          if (m = INCLUDE_RX.match(line.chomp))
            replacement = handle_include(m[1].downcase, m[2], include_once, include_stack)
            result = replacement.end_with?("\n") ? replacement : "#{replacement}\n"
          end
        end
        inside_comment = true if line.include?("/'")
        inside_comment = false if inside_comment && line.include?("'/")
        result
      end.join
    end

    # Returns the inlined text for one !include directive, or the original directive line if it can't
    # (or shouldn't) be resolved as an Antora resource.
    def handle_include(directive, args, include_once, include_stack)
      original = "!#{directive} #{args}"
      url, comment = parse_target(args)
      url_sub = url.split('!')
      target = url_sub[0].gsub(/\\ /, ' ').sub(/\s+\z/, '')
      sub = url_sub[1]

      return original if target.start_with?('<') # PlantUML standard library, resolved by the Kroki server
      return original if remote?(target)

      resolved = resolve(target)
      if resolved.nil? || !::File.readable?(resolved)
        # A broken Antora resource id is a real authoring error: fail loudly (like the Antora build,
        # asciidoctor and PlantUML) instead of silently rendering an incomplete diagram. A plain include
        # (no family marker) is not ours to resolve, so it passes through untouched.
        raise AntoraIncludeError, "kroki (antora): cannot resolve PlantUML include '#{target}'" if antora_resource_id?(target)
        return original
      end
      # A cycle would otherwise leave an unresolvable include in the output; fail like the JS extension.
      raise AntoraIncludeError, "kroki (antora): recursive PlantUML include of '#{target}'" if include_stack.include?(resolved)

      raw = ::File.read(resolved, mode: 'rb:utf-8:utf-8')

      text =
        if sub && !sub.empty?
          if directive == 'includesub'
            sub_text(raw, sub)
          elsif sub =~ /\A\d+\z/
            index_text(raw, sub.to_i)
          else
            id_text(raw, sub)
          end
        else
          first_block(raw)
        end

      if directive == 'include_once'
        # The resource was already inlined; passing the directive through would leave an unresolvable
        # include for Kroki. Fail like the asciidoctor-kroki JS extension rather than silently skip.
        raise AntoraIncludeError, "kroki (antora): '#{target}' is included more than once with !include_once" if include_once.include?(resolved)

        include_once << resolved
      end

      inlined = process_includes(text, include_once, include_stack + [resolved])
      comment.empty? ? inlined : "#{inlined} #{comment}"
    rescue AntoraIncludeError
      raise # intentional hard failure: must not be swallowed by the defensive rescue below
    rescue StandardError
      original
    end

    # Split a "!include" argument into [target, trailing-comment]. The space introducing a "#" comment
    # is dropped; the "/" of a "/'" comment is kept (matching PlantUML). With no comment the whole
    # value is the target and the comment is empty.
    def parse_target(value)
      match = COMMENT_RX.match(value)
      return [value, ''] unless match

      marker = match.begin(0)
      comment_start = value[marker] == ' ' ? marker + 1 : marker
      [value[0...marker].strip, value[comment_start..]]
    end

    def first_block(text)
      (m = text.match(PLANTUML_BLOCK_RX)) ? m[1] : text
    end

    def sub_text(text, sub)
      collect(text, /!startsub\s+#{Regexp.escape(sub)}(?:\r?\n)([\s\S]*?)(?:\r?\n)!endsub/m)
    end

    def id_text(text, id)
      collect(text, /@startuml\(id=#{Regexp.escape(id)}\)(?:\r?\n)([\s\S]*?)(?:\r?\n)@enduml/m)
    end

    def index_text(text, index)
      blocks = text.scan(PLANTUML_BLOCK_RX)
      blocks[index] ? blocks[index][0] : ''
    end

    def collect(text, regex)
      text.scan(regex).map { |g| g[0] }.join("\n")
    end

    def remove_tags(text)
      text.gsub(/^[ \t]*@(?:startuml|enduml).*\r?\n?/, '')
    end
  end

  # Resolve Antora resource ids used as the block-macro target (plantuml::example$...puml[]).
  module AntoraKrokiTarget
    def resolve_target_path(target)
      AntoraKroki.resolve(target) || super
    end
  end

  class KrokiBlockMacroProcessor
    prepend AntoraKrokiTarget
  end

  # Inline Antora-resource !include directives for both block (`[plantuml]`) and block-macro forms,
  # before the diagram text is encoded/sent to Kroki.
  class KrokiProcessor
    class << self
      prepend(Module.new do
        def process(processor, parent, attrs, diagram_type, diagram_text, logger)
          if %w[plantuml c4plantuml].include?(diagram_type.to_s)
            begin
              diagram_text = AntoraKroki.preprocess(diagram_text)
            rescue AntoraIncludeError
              raise # surface the clear error; don't silently render an incomplete diagram
            rescue StandardError
              # fall back to the unmodified diagram text
            end
          end
          super(processor, parent, attrs, diagram_type, diagram_text, logger)
        end
      end)
    end
  end
end
