# frozen_string_literal: true
require 'java'

# Antora support for the Kroki extension (loaded right after kroki-extension.rb, only when Kroki is enabled).
#
# Inside an Antora module a diagram source is referenced by resource id, e.g.
#
#   plantuml::example$order-model.puml[]
#
# Asciidoctor and the embedded Kroki extension do not understand resource ids, so the block macro target is
# resolved to a local path here, delegating to the Java side (AntoraReferenceAdapter#resolveAntoraResourcePath),
# which reuses the plugin's existing Antora module resolution. Outside an Antora module, or for a target that
# is not a resource id, the resolver returns nil and the default resolution (relative to the document) applies.
#
# Includes *inside* a .puml file are plain relative PlantUML includes (`!include layout/colors.puml`), resolved
# against the diagram file by the Kroki extension's own preprocessor - exactly as the asciidoctor-kroki
# JavaScript extension does for the Antora site. Nothing Antora-specific is needed for them.
#
# See https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/516
module AsciidoctorExtensions
  module AntoraKroki
    module_function

    # Resolve an Antora resource id to an absolute local path, or nil if not resolvable / not a resource id.
    def resolve(target)
      org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter.resolveAntoraResourcePath(target)
    rescue StandardError
      nil
    end
  end

  module AntoraKrokiTarget
    def resolve_target_path(parent, target)
      AntoraKroki.resolve(target) || super
    end
  end

  class KrokiBlockMacroProcessor
    prepend AntoraKrokiTarget
  end
end
