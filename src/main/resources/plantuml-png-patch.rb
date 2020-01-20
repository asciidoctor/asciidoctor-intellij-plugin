require 'asciidoctor/extensions'
require 'asciidoctor-diagram/plantuml/extension'

include ::Asciidoctor

# The JavaFX preview doesn't support SVG well. Therefore, we'll render all PlantUML images as PNG.
# To also be able to save them as SVGs with right-click in the preview, render them a second time as SVG.

module SvgToPngHack
  # see https://github.com/asciidoctor/asciidoctor-diagram/blob/master/lib/asciidoctor-diagram/extensions.rb for the source
  def process(parent, reader_or_target, attributes)
    # render as SVG to be used for save-as functionality
    attributes['format'] = 'svg'
    # rewind figure-number for second call (note that it will only be incremented if the diagram has a title)
    fignum = parent.document.attr('figure-number')
    super(parent, reader_or_target, attributes)
    parent.document.set_attr('figure-number', fignum)
    # render a second time for PNG to be used in preview
    attributes['format'] = 'png'
    super(parent, reader_or_target, attributes)
  end
end

# Duplicate target names from different diagrams will overwrite each others files. Therefore, issue a warning.

def check_duplicate_target(attributes, location, parent)
  if attributes['target']
    attr_name = 'plantuml-target-name-' + attributes['target']
    original_location = parent.document.attr(attr_name)
    if original_location
      logger.error message_with_context 'Duplicate target name, will overwrite file, first occurrence at ' + original_location.line_info, source_location: location
      logger.error message_with_context 'First occurrence of duplicate target name, another occurrence at ' + location.line_info, source_location: original_location
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

class Diagram::PlantUmlBlockProcessor
  prepend SvgToPngHack
  prepend DuplicateNameBlockHack
end

class Diagram::PlantUmlBlockMacroProcessor
  prepend SvgToPngHack
  prepend DuplicateNameBlockMacroHack
end

class Diagram::SaltBlockProcessor
  prepend SvgToPngHack
  prepend DuplicateNameBlockHack
end

class Diagram::SaltBlockMacroProcessor
  prepend SvgToPngHack
  prepend DuplicateNameBlockMacroHack
end

class Diagram::DiagramBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::DiagramBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::VegaBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::VegaBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::VegaBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::VegaBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::UmletBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::UmletBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::TikZBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::TikZBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::SyntraxBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::SyntraxBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::SyntraxBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::SyntraxBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::SvgBobBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::SvgBobBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::ShaapeBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::ShaapeBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::NomnomlBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::NomnomlBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::MscBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::MscBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::MermaidBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::MermaidBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::MermaidBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::MermaidBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::GraphvizBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::GraphvizBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::ErdBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::ErdBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::DitaaBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::DitaaBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end

class Diagram::AsciiToSvgBlockProcessor
  prepend DuplicateNameBlockHack
end

class Diagram::AsciiToSvgBlockMacroProcessor
  prepend DuplicateNameBlockMacroHack
end
