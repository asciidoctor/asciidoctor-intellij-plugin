# frozen_string_literal: true
# Original source:
# https://github.com/Mogztter/asciidoctor-kroki/blob/master/ruby/lib/asciidoctor/extensions/asciidoctor_kroki/extension.rb

require 'asciidoctor/extensions' unless RUBY_ENGINE == 'opal'

# Asciidoctor extensions
#
module AsciidoctorExtensions
  include Asciidoctor

  # A block extension that converts a diagram into an image.
  #
  class KrokiBlockProcessor < Extensions::BlockProcessor
    use_dsl

    on_context :listing, :literal
    name_positional_attributes 'target', 'format'

    def process(parent, reader, attrs)
      diagram_type = @name
      diagram_text = reader.string
      KrokiProcessor.process(self, parent, attrs, diagram_type, diagram_text)
    end
  end

  # A block macro extension that converts a diagram into an image.
  #
  class KrokiBlockMacroProcessor < Asciidoctor::Extensions::BlockMacroProcessor
    use_dsl

    name_positional_attributes 'format'

    def process(parent, target, attrs)
      diagram_type = @name
      target = parent.apply_subs(target, [:attributes])
      diagram_text = read(target)
      KrokiProcessor.process(self, parent, attrs, diagram_type, diagram_text)
    end

    def read(target)
      if target.start_with?('http://') || target.start_with?('https://')
        require 'open-uri'
        URI.open(target, &:read)
      else
        File.open(target, &:read)
      end
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
      ditaa
      erd
      excalidraw
      graphviz
      mermaid
      nomnoml
      nwdiag
      packetdiag
      plantuml
      rackdiag
      seqdiag
      svgbob
      umlet
      vega
      vegalite
      wavedrom
    ].freeze
  end

  # Internal processor
  #
  class KrokiProcessor
    TEXT_FORMATS = %w[txt atxt utxt].freeze

    class << self
      def process(processor, parent, attrs, diagram_type, diagram_text)
        doc = parent.document
        diagram_text = prepend_plantuml_config(diagram_text, diagram_type, doc)
        # If "subs" attribute is specified, substitute accordingly.
        # Be careful not to specify "specialcharacters" or your diagram code won't be valid anymore!
        if (subs = attrs['subs'])
          diagram_text = parent.apply_subs(diagram_text, parent.resolve_subs(subs))
        end
        title = attrs.delete('title')
        caption = attrs.delete('caption')
        attrs.delete('opts')
        role = attrs['role']
        format = get_format(doc, attrs, diagram_type)
        attrs['role'] = get_role(format, role)
        attrs['format'] = format
        kroki_diagram = KrokiDiagram.new(diagram_type, format, diagram_text)
        kroki_client = KrokiClient.new(server_url(doc), http_method(doc), KrokiHttpClient)
        if TEXT_FORMATS.include?(format)
          text_content = kroki_client.text_content(kroki_diagram)
          block = processor.create_block(parent, 'literal', text_content, attrs)
        else
          attrs['alt'] = get_alt(attrs)
          attrs['target'] = create_image_src(doc, kroki_diagram, kroki_client)
          block = processor.create_image_block(parent, attrs)
        end
        block.title = title if title
        block.assign_caption(caption, 'figure')
        block
      end

      private

      def prepend_plantuml_config(diagram_text, diagram_type, doc)
        if diagram_type == :plantuml && doc.attr?('kroki-plantuml-include')
          # TODO: this behaves different than the JS version
          # The file should be added by !include #{plantuml_include}" once we have a preprocessor for ruby
          config = File.read(doc.attr('kroki-plantuml-include'))
          diagram_text = config + "\n" + diagram_text
        end
        diagram_text
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

      def get_format(doc, attrs, diagram_type)
        format = attrs['format'] || 'svg'
        # The JavaFX preview doesn't support SVG well, therefore we'll use PNG format...
        if doc.attr?('kroki-force-png') && format == 'svg'
          # ... unless the diagram library does not support PNG as output format!
          # Currently, mermaid, nomnoml, svgbob, wavedrom only support SVG as output format.
          svg_only_diagram_types = %w[:mermaid :nomnoml :svgbob :wavedrom]
          format = 'png' unless svg_only_diagram_types.include?(diagram_type)
        end
        format
      end

      def create_image_src(doc, kroki_diagram, kroki_client)
        if doc.attr('kroki-fetch-diagram')
          kroki_diagram.save(output_dir_path(doc), kroki_client)
        else
          kroki_diagram.get_diagram_uri(server_url(doc))
        end
      end

      def server_url(doc)
        doc.attr('kroki-server-url', 'https://kroki.io')
      end

      def http_method(doc)
        doc.attr('kroki-http-method', 'adaptive').downcase
      end

      def output_dir_path(doc)
        images_output_dir = doc.attr('imagesoutdir')
        out_dir = doc.attr('outdir')
        to_dir = doc.attr('to_dir')
        base_dir = doc.base_dir
        images_dir = doc.attr('imagesdir', '')
        if images_output_dir
          images_output_dir
        elsif out_dir
          File.join(out_dir, images_dir)
        elsif to_dir
          File.join(to_dir, images_dir)
        else
          File.join(base_dir, images_dir)
        end
      end
    end
  end

  # Kroki diagram
  #
  class KrokiDiagram
    require 'fileutils'
    require 'zlib'
    require 'digest'

    attr_reader :type
    attr_reader :text
    attr_reader :format

    def initialize(type, format, text)
      @text = text
      @type = type
      @format = format
    end

    def get_diagram_uri(server_url)
      _join_uri_segments(server_url, @type, @format, encode)
    end

    def encode
      Base64.urlsafe_encode64(Zlib::Deflate.deflate(@text, 9))
    end

    def save(output_dir_path, kroki_client)
      diagram_url = get_diagram_uri(kroki_client.server_url)
      diagram_name = "diag-#{Digest::SHA256.hexdigest diagram_url}.#{@format}"
      file_path = File.join(output_dir_path, diagram_name)
      encoding = if @format == 'txt' || @format == 'atxt' || @format == 'utxt'
                   'utf8'
                 elsif @format == 'svg'
                   'binary'
                 else
                   'binary'
                 end
      # file is either (already) on the file system or we should read it from Kroki
      contents = File.exist?(file_path) ? File.open(file_path, &:read) : kroki_client.get_image(self, encoding)
      FileUtils.mkdir_p(output_dir_path)
      if encoding == 'binary'
        File.binwrite(file_path, contents)
      else
        File.write(file_path, contents)
      end
      diagram_name
    end

    private

    def _join_uri_segments(base, *uris)
      segments = []
      # remove trailing slashes
      segments.push(base.gsub(%r{[/]+$}, ''))
      segments.concat(uris.map do |uri|
        # remove leading and trailing slashes
        uri.to_s
           .gsub(%r{^[/]+}, '')
           .gsub(%r{[/]+$}, '')
      end)
      segments.join('/')
    end
  end

  # Kroki client
  #
  class KrokiClient
    attr_reader :server_url
    attr_reader :method

    SUPPORTED_HTTP_METHODS = %w[get post adaptive].freeze

    def initialize(server_url, http_method, http_client)
      @server_url = server_url
      @max_uri_length = 4096
      @http_client = http_client
      method = (http_method || 'adaptive').downcase
      if SUPPORTED_HTTP_METHODS.include?(method)
        @method = method
      else
        puts "Invalid value '#{method}' for kroki-http-method attribute. The value must be either: 'get', 'post' or 'adaptive'. Proceeding using: 'adaptive'."
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
      if @method == 'adaptive' || @method == 'get'
        uri = kroki_diagram.get_diagram_uri(server_url)
        if uri.length > @max_uri_length
          # The request URI is longer than 4096.
          if @method == 'get'
            # The request might be rejected by the server with a 414 Request-URI Too Large.
            # Consider using the attribute kroki-http-method with the value 'adaptive'.
            @http_client.get(uri, encoding)
          else
            @http_client.post("#{@server_url}/#{type}/#{format}", text, encoding)
          end
        else
          @http_client.get(uri, encoding)
        end
      else
        @http_client.post("#{@server_url}/#{type}/#{format}", text, encoding)
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
      def get(uri, _)
        ::OpenURI.open_uri(uri, 'r', &:read)
      end

      def post(uri, data, _)
        res = ::Net::HTTP.request_post(uri, data)
        res.body
      end
    end
  end
end

Extensions.register do
  ::AsciidoctorExtensions::Kroki::SUPPORTED_DIAGRAM_NAMES.each { |name|
    block_macro AsciidoctorExtensions::KrokiBlockMacroProcessor, name
    block AsciidoctorExtensions::KrokiBlockProcessor, name
  }
end
