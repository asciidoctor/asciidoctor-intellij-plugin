require 'asciidoctor/extensions'
require 'asciidoctor-diagram'

include ::Asciidoctor

# Duplicate target names from different diagrams will overwrite each others files. Therefore, issue a warning.

def check_duplicate_target(attributes, location, parent)
  if attributes['target'] && !attributes.key?('secondrun')
    attr_name = 'asciidoctor-diagram-target-name-' + attributes['target']
    original_location = parent.document.attr(attr_name)
    # if previous location found, and previous location is not current location
    # this allows for the second run to create both SVG and PNG
    if original_location
      logger.error message_with_context 'Duplicate target name "' + attributes['target'] + '", will overwrite file, first occurrence at ' + original_location.line_info, source_location: location
      logger.error message_with_context 'First occurrence of duplicate target name "' + attributes['target'] + '", another occurrence at ' + location.line_info, source_location: original_location
    else
      parent.document.set_attr(attr_name, location)
    end
  end
end

module DuplicateNameBlockHack
  # see https://github.com/asciidoctor/asciidoctor-diagram/blob/master/lib/asciidoctor-diagram/extensions.rb for the source
  def process(parent, reader_or_target, attributes)
    location = parent.document.reader.cursor_at_mark
    # move back one line to place warning on macro
    location = Reader::Cursor.new(location.file, location.dir, location.path, location.lineno - 1)
    check_duplicate_target(attributes, location, parent)
    super(parent, reader_or_target, attributes)
  end
end

module DuplicateNameBlockMacroHack
  # see https://github.com/asciidoctor/asciidoctor-diagram/blob/master/lib/asciidoctor-diagram/extensions.rb for the source
  def process(parent, reader_or_target, attributes)
    location = parent.document.reader.cursor_at_mark
    check_duplicate_target(attributes, location, parent)
    super(parent, reader_or_target, attributes)
  end

end

class Diagram::DiagramBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::DiagramBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end
