require 'asciidoctor/extensions'

include ::Asciidoctor

# Asciidoctor peeks two lines to detect setext section titles. If the next block starts on the last line of an include,
# the second line is read from the parent document. This pops the include and moves the last line of the include
# to the parent document, so the block's source location points to the including file instead of the included file.
# Limit the peek to the lines remaining in the current include to keep the source location of the block.
# See https://github.com/asciidoctor/asciidoctor/issues/4907
module PeekLinesWithinInclude
  def peek_lines num = nil, direct = false
    if num && !@include_stack.empty? && (remaining = @lines.size) > 0 && remaining < num
      num = remaining
    end
    super num, direct
  end
end
Asciidoctor::PreprocessorReader.prepend PeekLinesWithinInclude

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
