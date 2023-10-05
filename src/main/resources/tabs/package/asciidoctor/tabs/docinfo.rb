# frozen_string_literal: true

module Asciidoctor
  module Tabs
    module Docinfo
      if RUBY_ENGINE == 'opal'
        DATA_DIR = ::File.absolute_path '../dist', %x(__dirname)
      else
        DATA_DIR = ::File.join (::File.absolute_path '../../..', __dir__), 'data'
      end

      class Style < ::Asciidoctor::Extensions::DocinfoProcessor
        use_dsl
        at_location :head

        DEFAULT_STYLESHEET_FILE = ::File.join DATA_DIR, 'css/tabs.css'

        def process doc
          return unless (path = doc.attr 'tabs-stylesheet')
          if doc.attr? 'linkcss'
            href = doc.normalize_web_path (path.empty? ? 'asciidoctor-tabs.css' : path), (doc.attr 'stylesdir')
            %(<link rel="stylesheet" href="#{href}"#{(doc.attr? 'htmlsyntax', 'xml') ? '/' : ''}>) # rubocop:disable Style/TernaryParentheses
          elsif (styles = path.empty? ?
              (doc.read_asset DEFAULT_STYLESHEET_FILE) :
              (doc.read_contents path, start: (doc.attr 'stylesdir'), warn_on_failure: true, label: 'tabs stylesheet'))
            %(<style>\n#{styles.chomp}\n</style>)
          end
        end
      end

      class Behavior < ::Asciidoctor::Extensions::DocinfoProcessor
        use_dsl
        at_location :footer

        JAVASCRIPT_FILE = ::File.join DATA_DIR, 'js/tabs.js'

        def process doc
          if doc.attr? 'tabs-sync-storage-key'
            config_attrs = %( data-sync-storage-key="#{doc.attr 'tabs-sync-storage-key'}")
            if doc.attr? 'tabs-sync-storage-scope'
              config_attrs += %( data-sync-storage-scope="#{doc.attr 'tabs-sync-storage-scope'}")
            end
          else
            config_attrs = ''
          end
          if doc.attr? 'linkcss'
            src = doc.normalize_web_path 'asciidoctor-tabs.js', (doc.attr 'scriptsdir')
            %(<script src="#{src}"#{config_attrs}></script>)
          elsif (script = doc.read_asset JAVASCRIPT_FILE)
            %(<script#{config_attrs}>\n#{script.chomp}\n</script>)
          end
        end
      end
    end
  end
end
