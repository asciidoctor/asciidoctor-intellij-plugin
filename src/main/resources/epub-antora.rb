require 'java'
require 'asciidoctor/epub3'

include ::Asciidoctor

# resolve Antora references

module ResolveAntoraEpub
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
  def convert_image(node, opts = {})
    org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.convertImage(node)
    super(node, opts)
  end
end

class Asciidoctor::EPUB::Converter < ::Prawn::Document
  prepend ResolveAntoraEpub
end

