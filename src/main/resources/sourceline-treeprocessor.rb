
require 'asciidoctor/extensions'

include ::Asciidoctor

class SourceLineTreeProcessor < Extensions::Treeprocessor
  def process document

    document.find_by.each do |node|

      # on each node add the source file information as role (will result in CSS class in HTML)
      if (node.source_location) then
        node.attributes['role'] = 'has-source-line data-line-' + (node.source_location.file || 'stdin') + "-#{node.source_location.lineno}"
      end
    end
    nil
  end


end