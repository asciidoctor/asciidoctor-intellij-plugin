require 'asciidoctor/extensions'

include ::Asciidoctor

class SourceLineTreeProcessor < Extensions::Treeprocessor
  def process document

    document.find_by.each do |node|

      # on each node add the source file information as role (will result in CSS class in HTML)
      if node.source_location
        if node.source_location.lineno != 1
          # on AsciiDoc 1.5.7 I've seen source lines of "1" with in inline formatting (i.e. links and bold)
          # it seems that they have been inherited from the document/parent, therefore all entries with lineno 1 are ignored
          node.attributes['role'] = 'has-source-line data-line-' + (node.source_location.file || 'stdin') + "-#{node.source_location.lineno}"
        end
      end
    end
    nil
  end


end
