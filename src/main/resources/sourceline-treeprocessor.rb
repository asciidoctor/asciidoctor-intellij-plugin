require 'asciidoctor/extensions'

include ::Asciidoctor

# inspired by AsciiDocFX
# see: https://github.com/asciidocfx/AsciidocFX/blob/master/conf/public/js/asciidoctor-data-line.js
class SourceLineTreeProcessor < Extensions::Treeprocessor
  def process document

    # docfile has been set to emulate non-embedded style
    docfile = document.attr('docfile')

    document.find_by(traverse_documents: true).each do |node|

      # on each node add the source file information as role (will result in CSS class in HTML)
      if node.source_location
        if node.class.name != 'Asciidoctor::Document'
          # on AsciiDoc 1.5.7 I've seen source lines with in inline formatting (i.e. links and bold)
          # it seems that they have been inherited from the document/parent, therefore the document will not get a role
          oldrole = node.attributes['role'] ? node.attributes['role'] + ' ' : ''
          node.attributes['role'] = oldrole + 'has-source-line data-line-' + ((node.source_location.file && node.source_location.file.to_s != docfile) ? node.source_location.file.to_s : 'stdin') + "-#{node.source_location.lineno}"
        end
      end
    end
    nil
  end


end
