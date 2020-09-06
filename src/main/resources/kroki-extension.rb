# frozen_string_literal: true

require 'asciidoctor/extensions' unless RUBY_ENGINE == 'opal'
require 'stringio'
require 'zlib'

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

    def process(parent, target, attrs)
      diagram_type = @name
      target = parent.apply_subs(target, ['attributes'])
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

  # Internal processor
  #
  class KrokiProcessor
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
        attrs['alt'] = get_alt(attrs)
        attrs['target'] = create_image_src(doc, diagram_type, format, diagram_text)
        attrs['format'] = format
        block = processor.create_image_block(parent, attrs)
        block.title = title
        block.assign_caption(caption, 'figure')
        block
      end

      private

      def prepend_plantuml_config(diagram_text, diagram_type, doc)
        if diagram_type == :plantuml && doc.attr?('kroki-plantuml-include')
          # TODO: this behaves different than the JS version
          # The file should be added by !include #{plantuml_include}" once we have a preprocessor for ruby
          config = File.read(doc.attr('kroki-plantuml-include'))
          diagram_text = config + '\n' + diagram_text
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

      def create_image_src(doc, type, format, text)
        data = Base64.urlsafe_encode64(Zlib::Deflate.deflate(text, 9))
        "#{server_url(doc)}/#{type}/#{format}/#{data}"
      end

      def server_url(doc)
        doc.attr('kroki-server-url') || 'https://kroki.io'
      end
    end
  end
end

Extensions.register do
  names = %w(plantuml ditaa graphviz blockdiag seqdiag actdiag nwdiag packetdiag rackdiag c4plantuml erd mermaid nomnoml svgbob umlet vega vegalite wavedrom)
  names.each { |name|
    block_macro AsciidoctorExtensions::KrokiBlockMacroProcessor, name
    block AsciidoctorExtensions::KrokiBlockProcessor, name
  }
end
