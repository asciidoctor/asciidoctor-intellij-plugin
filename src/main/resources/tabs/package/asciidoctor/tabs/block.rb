# frozen_string_literal: true

module Asciidoctor
  module Tabs
    class Block < ::Asciidoctor::Extensions::BlockProcessor
      use_dsl
      on_context :example

      def process parent, reader, attrs
        doc = parent.document
        return create_open_block parent, nil, attrs unless doc.attr? 'filetype', 'html'
        tabs_number = doc.counter 'tabs-number'
        block = create_block parent, attrs['cloaked-context'], nil, attrs, content_model: :compound
        children = (parse_content block, reader).blocks
        return (reset_counter doc, 'tabs-number', (tabs_number - 1)) || block unless children.size == 1 &&
          (seed_tabs = children[0]).context == :dlist && seed_tabs.items?
        tabs_id = attrs['id'] || (generate_id %(tabs #{tabs_number}), doc)
        tabs_role = 'tabs' + (!(block.option? 'nosync') && ((block.option? 'sync') || (doc.option? 'tabs-sync')) ?
          ((gid = attrs['sync-group-id']) ? %( is-sync data-sync-group-id=#{gid.gsub ' ', ?\u00a0}) : ' is-sync') : '')
        tabs_role += (tabs_user_role = attrs['role']) ? %( #{tabs_user_role} is-loading) : ' is-loading'
        tabs_attrs = { 'id' => tabs_id, 'role' => tabs_role }
        tabs_attrs[:attribute_entries] = attrs[:attribute_entries] if attrs.key? :attribute_entries
        (tabs = create_open_block parent, nil, tabs_attrs).title = attrs['title']
        tablist = create_list parent, :ulist, { 'role' => 'tablist' }
        panes = {}
        set_id_on_tab = (doc.backend == 'html5') || (list_item_supports_id? doc)
        seed_tabs.items.each do |labels, content|
          tab_ids = labels.map do |tab|
            tablist << tab
            tab_id = generate_id tab.text, doc, tabs_id
            tab_text_source = tab.instance_variable_get :@text
            set_id_on_tab ? (tab.id = tab_id) : (tab.text = %([[#{tab_id}]]#{tab_text_source}))
            tab.role = 'tab'
            (doc.register :refs, [tab_id, tab]).set_attr 'reftext', tab_text_source
            tab_id
          end
          if content
            tab_blocks = content.text? ?
              [(create_paragraph parent, (content.instance_variable_get :@text), nil, subs: :normal)] : []
            if content.blocks?
              if (block0 = (blocks = content.blocks)[0]).context == :open && block0.style == 'open' &&
                  blocks.size == 1 && !(block0.id || block0.title? || block0.attributes.keys.any? {|k| k != 'style' })
                blocks = block0.blocks
              end
              tab_blocks.push(*blocks)
            end
          end
          panes[tab_ids] = tab_blocks || []
        end
        tabs << tablist
        panes.each do |tab_ids, blocks|
          attrs = %( id="#{tab_ids[0]}--panel" class="tabpanel" aria-labelledby="#{tab_ids.join ' '}")
          tabs << (create_html_fragment parent, %(<div#{attrs}>))
          blocks.each {|it| tabs << it }
          tabs << (create_html_fragment parent, '</div>')
        end
        tabs
      end

      private

      def create_html_fragment parent, html, attributes = nil
        create_block parent, :pass, html, attributes
      end

      def generate_id str, doc, base_id = nil
        if base_id
          restore_idprefix = (attrs = doc.attributes)['idprefix']
          attrs['idprefix'] = %(#{base_id}#{attrs['idseparator'] || '_'})
        end
        ::Asciidoctor::Section.generate_id str, doc
      ensure
        restore_idprefix ? (attrs['idprefix'] = restore_idprefix) : (attrs.delete 'idprefix') if base_id
      end

      def list_item_supports_id? doc
        if (converter = doc.converter).instance_variable_defined? :@list_item_supports_id
          converter.instance_variable_get :@list_item_supports_id
        else
          output = (create_list doc, :ulist).tap {|ul| ul << (create_list_item ul).tap {|li| li.id = 'name' } }.convert
          converter.instance_variable_set :@list_item_supports_id, (output.include? ' id="name"')
        end
      end

      def reset_counter doc, name, val
        doc.counters[name] = val
        doc.set_attr name, val unless doc.attribute_locked? name
        nil
      end
    end
  end
end
