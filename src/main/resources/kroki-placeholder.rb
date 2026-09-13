# frozen_string_literal: true
require 'cgi'

# Placeholders for Kroki diagrams that cannot be rendered in the preview (loaded after kroki-extension.rb and
# kroki-antora.rb, only when Kroki is the selected diagram renderer).
#
# A diagram fails in the preview when its block-macro target cannot be resolved or read, when it still contains
# a local `!include` after the Kroki extension's preprocessing (the referenced file does not exist; the Kroki
# server has no file system and PlantUML there silently drops such a line and renders a degraded diagram, e.g.
# without the shared layout), or when the Kroki extension itself reports an error (a `kroki-error` block).
# Instead of an error paragraph or a silently incomplete diagram, the preview shows a placeholder box naming
# the diagram and the reason, with a link that opens the source in the IDE editor - where the PlantUML
# integration plugin, if installed, previews it on its own.
#
#   1. KrokiBlockMacroProcessor#process and KrokiBlockProcessor#process are wrapped: an unreadable target
#      yields the placeholder directly; a `kroki-error` block returned by the Kroki extension is turned into
#      the placeholder carrying its message.
#   2. PlantUmlPreprocessor.preprocess is wrapped: a local `!include` (not `<stdlib>`, not http(s)) still
#      present after preprocessing raises UnresolvedIncludeError, which the Kroki extension reports as a
#      `kroki-error` block -> placeholder, so the server never renders a silently degraded diagram.
#   3. Setting the document attribute `kroki-preview-placeholder` (e.g. via the plugin's attribute settings)
#      shows placeholders for all Kroki diagrams without contacting the server at all.
#
# The image itself is fetched by the preview's browser, so a transport failure (server unreachable, URI too
# long) still shows as a broken image; that case is intentionally not handled here.
#
# See https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/516
module AsciidoctorExtensions
  # Raised before a PlantUML diagram with a local include the Kroki server cannot resolve is sent.
  class UnresolvedIncludeError < StandardError; end

  module KrokiPlaceholder
    STYLE = 'border: 1px dashed #999; border-radius: 4px; padding: 0.6em 0.9em; margin: 0.5em 0; ' \
            'font-family: sans-serif; font-size: 0.9em; background: rgba(128,128,128,0.08);'
    LOCAL_INCLUDE_RX = /^\s*!include(?:_many|_once|sub|url)?\s+(\S+)/i

    module_function

    def placeholder_only?(doc)
      doc.attr?('kroki-preview-placeholder')
    end

    # Local include targets (relative paths) left in a diagram: the server cannot read them.
    def unresolved_includes(diagram_text)
      diagram_text.each_line.filter_map do |line|
        next unless (m = LOCAL_INCLUDE_RX.match(line))

        target = m[1].split('!').first
        next if target.start_with?('<', 'http://', 'https://')

        target
      end.uniq
    end

    def error_block?(block)
      block.respond_to?(:role) && block.role.to_s.split.include?('kroki-error')
    end

    def file_link(path)
      return nil if path.nil? || path.empty? || path.start_with?('http://', 'https://')

      p = path.to_s.tr('\\', '/')
      p = "/#{p}" unless p.start_with?('/')
      "file://#{p}"
    end

    # Builds the placeholder block. `source` is the resolved local path of the diagram source (or nil for an
    # inline block), `message` the reason, `text` an optional diagram excerpt (inline blocks).
    def block(processor, parent, diagram_type, source, message, text = nil)
      e = ->(s) { CGI.escapeHTML(s.to_s) }
      html = +"<div class=\"kroki-placeholder\" style=\"#{STYLE}\" data-kroki-type=\"#{e[diagram_type]}\">"
      html << "<div><strong>#{e[diagram_type]} diagram not rendered</strong></div>"
      html << "<div class=\"kroki-placeholder-message\">#{e[message]}</div>" if message && !message.empty?
      if (link = file_link(source))
        html << "<div><a href=\"#{e[link]}\" class=\"kroki-placeholder-open\">Open #{e[::File.basename(source)]}</a></div>"
      elsif text
        html << "<pre class=\"kroki-placeholder-source\" style=\"margin:0.4em 0 0; white-space: pre-wrap;\">#{e[text]}</pre>"
      end
      html << '</div>'
      processor.create_block(parent, :pass, html, {}, content_model: :raw)
    end

    # The result of the Kroki extension: a placeholder if it reported an error, the result itself otherwise.
    # For a block macro the error block is a paragraph carrying the message; for a delimited block it is the
    # original listing/literal block, i.e. its source is the diagram text (shown as the excerpt).
    def finish(processor, parent, result, diagram_type, source)
      return result unless error_block?(result)

      if result.context.to_s == 'paragraph' # the Kroki extension creates it with a String context
        message = result.source.to_s.lines.first.to_s.strip
        text = nil
      else
        message = 'Diagram could not be rendered'
        text = result.source.to_s
      end
      message = 'Diagram could not be rendered' if message.empty?
      block(processor, parent, diagram_type, source, message, text)
    end
  end

  # Block macro form: plantuml::example$model.puml[]
  module KrokiPlaceholderBlockMacro
    def process(parent, target, attrs)
      diagram_type = @name
      resolved_target = parent.apply_subs(target, [:attributes])
      path = resolve_target_path(parent, resolved_target)
      remote = path && (path.start_with?('http://') || path.start_with?('https://'))
      unless path && (remote || ::File.readable?(path))
        # no file to open: name the macro and, if the target resolved to a path, where the file was expected
        message = "Cannot read #{diagram_type}::#{resolved_target}[]"
        message += " (resolved to #{path})" if path && path != resolved_target
        return KrokiPlaceholder.block(self, parent, diagram_type, nil, message)
      end
      if KrokiPlaceholder.placeholder_only?(parent.document)
        return KrokiPlaceholder.block(self, parent, diagram_type, path, 'Preview placeholders enabled (kroki-preview-placeholder)')
      end
      KrokiPlaceholder.finish(self, parent, super, diagram_type, remote ? nil : path)
    end
  end

  # Delimited block form: [plantuml] ---- ... ----
  module KrokiPlaceholderBlock
    def process(parent, reader, attrs)
      diagram_type = @name
      if KrokiPlaceholder.placeholder_only?(parent.document)
        # the only path that reads the text itself; otherwise the reader is left to the Kroki extension
        return KrokiPlaceholder.block(self, parent, diagram_type, nil, 'Preview placeholders enabled (kroki-preview-placeholder)', reader.string)
      end
      KrokiPlaceholder.finish(self, parent, super, diagram_type, nil)
    end
  end

  # After the Kroki extension resolved what it could, a remaining local include means a missing file.
  module KrokiPlaceholderPreprocessor
    def preprocess(diagram_text, resource_path, include_paths, logger)
      text = super
      unresolved = KrokiPlaceholder.unresolved_includes(text)
      unless unresolved.empty?
        raise UnresolvedIncludeError, "Cannot resolve PlantUML include#{'s' if unresolved.size > 1}: #{unresolved.join(', ')}"
      end
      text
    end
  end

  module PlantUmlPreprocessor
    class << self
      prepend KrokiPlaceholderPreprocessor
    end
  end

  class KrokiBlockMacroProcessor
    prepend KrokiPlaceholderBlockMacro
  end

  class KrokiBlockProcessor
    prepend KrokiPlaceholderBlock
  end
end
