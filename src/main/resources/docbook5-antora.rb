require 'java'

include ::Asciidoctor

# resolve Antora references

module ResolveAntoraDocbook5
  def convert_inline_anchor(node)
    if node.type == :xref
      org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.convertInlineAnchor(node)
    end
    super(node)
  end
  def convert_inline_image(node)
    org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.convertInlineImage(node)
    super(node)
  end
  def convert_image(node)
    org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.convertImage(node)
    super(node)
  end
end

class Converter::DocBook5Converter < Converter::Base
  prepend ResolveAntoraDocbook5
end
