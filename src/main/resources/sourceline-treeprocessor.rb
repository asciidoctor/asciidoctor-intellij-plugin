require 'asciidoctor/extensions'

include ::Asciidoctor

class SourceLineTreeProcessor < Extensions::Treeprocessor
  def process document

    document.find_by.each do |node|

      # on each node add the source file information as role (will result in CSS class in HTML)
      if node.source_location
        if node.class.name != 'Asciidoctor::Document'
          # on AsciiDoc 1.5.7 I've seen source lines with in inline formatting (i.e. links and bold)
          # it seems that they have been inherited from the document/parent, therefore the document will not get a role
          node.attributes['role'] = 'has-source-line data-line-' + (node.source_location.file || 'stdin') + "-#{node.source_location.lineno}"
        end
      end
    end
    nil
  end


end
