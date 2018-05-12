require 'asciidoctor/extensions'
require 'asciidoctor-diagram/plantuml/extension'

include ::Asciidoctor

# The JavaFX preview doesn't support SVG well. Therefore we'll render all PlantUML images as PNG

module SvgToPngHack
  # see https://github.com/asciidoctor/asciidoctor-diagram/blob/master/lib/asciidoctor-diagram/extensions.rb for the souce
  def process(parent, reader_or_target, attributes)
    if(attributes['format'] == 'svg')
      attributes['format'] = 'png'
    end
    super(parent, reader_or_target, attributes)
  end
end

class Diagram::PlantUmlBlockProcessor
  prepend SvgToPngHack
end

class Diagram::PlantUmlBlockMacroProcessor
  prepend SvgToPngHack
end

class Diagram::SaltBlockProcessor
  prepend SvgToPngHack
end

class Diagram::SaltBlockMacroProcessor
  prepend SvgToPngHack
end
