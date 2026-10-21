require 'asciidoctor/extensions'
require 'asciidoctor-diagram'

# A relative 'imagesoutdir' is resolved by asciidoctor-diagram against the working directory of the JVM,
# which would be the installation directory of the IDE. Resolve it against the output directory instead.
# see https://github.com/asciidoctor/asciidoctor-diagram/blob/master/lib/asciidoctor-diagram/diagram_processor.rb for the source
module Asciidoctor
  module Diagram
    module DiagramProcessor
      def image_output_dir(parent)
        images_out_dir = parent.attr('imagesoutdir', nil, true)

        if images_out_dir
          resolve_path(parent, images_out_dir, output_dir(parent))
        else
          images_dir = parent.attr('imagesdir', nil, true)
          resolve_path(parent, images_dir, output_dir(parent))
        end
      end
    end
  end
end
