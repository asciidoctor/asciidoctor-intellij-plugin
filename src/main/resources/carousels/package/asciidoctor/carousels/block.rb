# frozen_string_literal: true

require 'cgi'

module Asciidoctor
  module Carousels
    # Processes an example block marked with the carousel style and transforms
    # its visual children into an accessible, scripted carousel.
    #
    # The processor intentionally delegates the conversion of each slide to the
    # standard AsciiDoc converters. That allows ordinary image blocks and blocks
    # produced by other extensions, such as Asciidoctor Diagram, to keep all of
    # their usual behavior and attributes.
    class Block < ::Asciidoctor::Extensions::BlockProcessor
      use_dsl
      on_context :example

      DEFAULT_INTERVAL = '3000'
      DEFAULT_ACTIVE_SLIDE = '1'
      STAGE_ALIGNMENTS = %w(left right center).freeze
      DEFAULT_TRANSITION = 'slide'
      TRANSITIONS = %w(slide fade zoom flip cube cards coverflow kenburns).freeze

      def process parent, reader, attrs
        doc = parent.document
        block = create_block parent, attrs['cloaked-context'], nil, attrs, content_model: :compound
        children = (parse_content block, reader).blocks
        return block unless doc.attr? 'filetype', 'html'

        # Only children that resolve to a single visual image are valid slides.
        slides = children.map {|child| slide_type child }
        return block if slides.empty? || slides.any?(&:nil?)

        carousel_number = doc.counter 'carousel-number'
        carousel_id = attrs['id'] || (generate_id %(carousel #{carousel_number}), doc)
        carousel = create_open_block parent, nil, carousel_attributes(carousel_id, attrs)
        carousel.title = attrs['title']

        active_slide = active_slide_index attrs
        stage_class = %(carousel-stage carousel-stage-align-#{stage_align attrs})
        carousel << (create_html_fragment parent, %(<div class="#{stage_class}"#{stage_attributes attrs}>))
        carousel << (create_html_fragment parent, %(<div class="carousel-track">))

        children.each_with_index do |child, idx|
          slide_number = idx + 1
          active = slide_number == active_slide
          carousel << (create_html_fragment parent, slide_opening(slide_id(carousel_id, slide_number, doc), active))
          if (caption = caption_for child)
            carousel << (create_html_fragment parent, %(<div class="carousel-slide-caption">#{caption}</div>))
            # Remove the title from the child so the slide does not also render
            # a numbered figure caption (or a paragraph title) underneath it.
            child.title = nil
            child.caption = nil
          end
          carousel << child
          carousel << (create_html_fragment parent, '</div>')
        end

        carousel << (create_html_fragment parent, '</div>')
        append_controls carousel, parent, carousel_id unless option_enabled? attrs, 'nocontrols'
        append_indicators carousel, parent, children.size, active_slide unless option_enabled? attrs, 'noindicators'
        carousel << (create_html_fragment parent, '</div>')
        carousel
      end

      private

      # Returns the kind of slide represented by the child, or nil when the
      # child cannot be represented as a single visual slide.
      def slide_type child
        case child.context
        when :image
          :image
        when :paragraph
          single_inline_image?(child) ? :paragraph : nil
        end
      end

      # Detects a paragraph whose only inline content is a single image.
      # This is how inline image macros and inline diagram macros are promoted
      # to slides without introducing a dedicated AsciiDoc syntax.
      def single_inline_image? paragraph
        html = paragraph.convert
        # A block title is not part of the inline content, so remove it before
        # deciding whether the paragraph contains exactly one image.
        html = html.sub %r/<div class="title">.*?<\/div>/m, ''
        return false unless html.scan(/<img\b/).size == 1
        html.gsub(/<[^>]+>/m, '').strip.empty?
      end

      def caption_for child
        child.title? ? child.title : nil
      end

      def carousel_attributes carousel_id, attrs
        role = ['carousel', attrs['role']].compact
        role << 'is-loading'
        role << %(carousel-transition-#{transition attrs})
        role << %(carousel-align-#{stage_align attrs})
        attributes = { 'id' => carousel_id, 'role' => role.join(' ') }
        attributes[:attribute_entries] = attrs[:attribute_entries] if attrs.key? :attribute_entries
        attributes
      end

      def stage_attributes attrs
        pairs = [
          %(data-interval="#{escape_attr attrs['interval'] || DEFAULT_INTERVAL}"),
          %(data-active-slide="#{escape_attr attrs['active-slide'] || DEFAULT_ACTIVE_SLIDE}"),
          %(data-autoplay="#{option_enabled? attrs, 'autoplay'}"),
          %(data-loop="#{!option_enabled? attrs, 'noloop'}"),
          %(data-keyboard="#{!option_enabled? attrs, 'nokeyboard'}"),
          %(data-touch="#{!option_enabled? attrs, 'notouch'}"),
          %(data-pause-on-hover="#{option_enabled? attrs, 'pause-on-hover'}"),
          %(data-transition="#{transition attrs}"),
        ]
        style = []
        style << %(--carousel-height: #{attrs['height']}) if attrs['height']
        style << %(--carousel-width: #{attrs['width']}) if attrs['width']
        style << %(--carousel-aspect-ratio: #{attrs['aspect-ratio']}) if attrs['aspect-ratio']
        %( #{pairs.join ' '}#{style.empty? ? '' : %( style="#{escape_attr style.join '; '}")})
      end

      def stage_align attrs
        align = attrs['align']
        STAGE_ALIGNMENTS.include?(align) ? align : 'center'
      end

      # Resolves the transition effect from the fade attribute, which names the
      # effect to use. A value the extension does not know falls back to a plain
      # slide, the same way an unknown align falls back to center.
      def transition attrs
        value = attrs['fade']
        TRANSITIONS.include?(value) ? value : DEFAULT_TRANSITION
      end

      def active_slide_index attrs
        slide = attrs['active-slide'] || DEFAULT_ACTIVE_SLIDE
        slide = slide.to_i
        slide > 0 ? slide : 1
      end

      def option_enabled? attrs, name
        attrs.key? %(#{name}-option)
      end

      def slide_id carousel_id, slide_number, doc
        %(#{carousel_id}#{doc.attr 'idseparator', '_'}slide-#{slide_number})
      end

      def slide_opening slide_id, active
        state_class = active ? ' is-active' : ' is-hidden'
        aria_hidden = active ? 'false' : 'true'
        %(<div id="#{escape_attr slide_id}" class="carousel-slide#{state_class}") +
          %( aria-hidden="#{aria_hidden}">)
      end

      def append_controls carousel, parent, carousel_id
        carousel << (create_html_fragment parent, control_html('prev', carousel_id))
        carousel << (create_html_fragment parent, control_html('next', carousel_id))
      end

      def control_html direction, carousel_id
        label = direction == 'prev' ? 'Previous slide' : 'Next slide'
        glyph = direction == 'prev' ? '&#8249;' : '&#8250;'
        %(<button type="button" class="carousel-control carousel-control-#{direction}") +
          %( data-carousel-action="#{direction}") +
          %( aria-controls="#{escape_attr carousel_id}" aria-label="#{label}">) +
          %(<span class="carousel-control-icon">#{glyph}</span></button>)
      end

      def append_indicators carousel, parent, count, active_slide
        carousel << (create_html_fragment parent, %(<div class="carousel-indicators" role="tablist">))
        count.times do |idx|
          slide_number = idx + 1
          active = slide_number == active_slide
          carousel << (create_html_fragment parent, indicator_html(idx, slide_number, active))
        end
        carousel << (create_html_fragment parent, '</div>')
      end

      def indicator_html idx, slide_number, active
        active_class = active ? ' is-active' : ''
        %(<button type="button" class="carousel-indicator#{active_class}" data-carousel-action="go") +
          %( data-carousel-index="#{idx}") +
          %( aria-label="Go to slide #{slide_number}"><span>#{slide_number}</span></button>)
      end

      def create_html_fragment parent, html
        create_block parent, :pass, html, nil
      end

      def escape_attr value
        CGI.escapeHTML value.to_s
      end

      def generate_id str, doc, base_id = nil
        if base_id
          restore_idprefix = (attrs = doc.attributes)['idprefix']
          attrs['idprefix'] = %(#{base_id}#{attrs['idseparator'] || '_'})
        end
        ::Asciidoctor::Section.generate_id str, doc
      ensure
        if base_id
          restore_idprefix ? (attrs['idprefix'] = restore_idprefix) : (attrs.delete 'idprefix')
        end
      end
    end
  end
end
