require 'asciidoctor/extensions'
require 'stringio'
require 'zlib'

include ::Asciidoctor

#
#
class KrokiBlock < Asciidoctor::Extensions::BlockProcessor
  use_dsl
  on_context :listing, :literal
  name_positional_attributes 'target', 'format'

  def process(parent, reader, attrs)
    diagram_type = @name
    diagram_text = reader.string
    KrokiProcessor.process(self, parent, attrs, diagram_type, diagram_text)
  end
end

#
#
class KrokiBlockMacro < Asciidoctor::Extensions::BlockMacroProcessor
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
    end
    open(target) { |f| f.read }
  end
end

class KrokiProcessor
  class << self
    def process(processor, parent, attrs, diagram_type, diagram_text)
      doc = parent.document
      # If "subs" attribute is specified, substitute accordingly.
      # Be careful not to specify "specialcharacters" or your diagram code won't be valid anymore!
      if (subs = attrs['subs'])
        diagram_text = parent.apply_subs(diagram_text, parent.resolve_subs(subs), true)
      end
      role = attrs['role']
      block_id = attrs['id']
      title = attrs['title']
      target = attrs['target']
      format = attrs['format'] || 'svg'
      # The JavaFX preview doesn't support SVG well, therefore we'll use PNG format...
      if format == 'svg'
        # ... unless the diagram library does not support PNG as output format!
        # Currently, mermaid, nomnoml and svgbob only support SVG as output format.
        format = 'png' unless diagram_type == :mermaid || diagram_type == :nomnoml || diagram_type == :svgbob
      end
      image_url = _create_image_src(doc, diagram_type, format, diagram_text)
      block_attrs = {
          'role' => role ? "#{role} kroki" : 'kroki',
          'target' => image_url,
          'alt' => target || 'diagram',
          'title' => title
      }
      if block_id
        block_attrs['id'] = block_id
      end
      processor.create_image_block(parent, block_attrs)
    end

    private

    def _create_image_src(doc, type, format, text)
      server_url = _server_url(doc)
      data = Base64.urlsafe_encode64(Zlib::Deflate.deflate(text, 9))
      "#{server_url}/#{type}/#{format}/#{data}"
    end

    def _server_url(doc)
      doc.attr('kroki-server-url') || 'https://kroki.io'
    end
  end
end

Extensions.register do
  names = %w(plantuml ditaa graphviz blockdiag seqdiag actdiag nwdiag c4plantuml erd mermaid nomnoml svgbob umlet)
  names.each { |name|
    block_macro KrokiBlockMacro, name
    block KrokiBlock, name
  }
end
