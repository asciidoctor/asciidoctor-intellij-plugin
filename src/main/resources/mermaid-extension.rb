# frozen_string_literal: true

require 'asciidoctor/extensions' unless RUBY_ENGINE == 'opal'

# Asciidoctor extensions
#
module AsciidoctorExtensions
  include Asciidoctor

  # A block extension that converts a diagram into an image.
  #
  class MermaidBlockProcessor < Extensions::BlockProcessor
    use_dsl

    on_context :listing, :literal

    # @param name [String] name of the block macro (optional)
    # @param config [Hash] a config hash (optional)
    #   - :logger a logger used to log warning and errors (optional)
    #
    def initialize(name = nil, config = {})
      @logger = (config || {}).delete(:logger) { ::Asciidoctor::LoggerManager.logger }
      super(name, config)
    end

    def process(parent, reader, attrs)
      diagram_type = @name
      diagram_text = reader.read
      MermaidProcessor.process(self, parent, attrs, diagram_type, diagram_text, @logger)
    end

    protected

    attr_reader :logger
  end

  # A block macro extension that converts a diagram into an image.
  #
  class MermaidBlockMacroProcessor < Asciidoctor::Extensions::BlockMacroProcessor
    include Asciidoctor::Logging
    use_dsl

    # @param name [String] name of the block macro (optional)
    # @param config [Hash] a config hash (optional)
    #   - :logger a logger used to log warning and errors (optional)
    #
    def initialize(name = nil, config = {})
      @logger = (config || {}).delete(:logger) { ::Asciidoctor::LoggerManager.logger }
      super(name, config)
    end

    def process(parent, target, attrs)
      diagram_type = @name
      target = parent.apply_subs(target, [:attributes])

      unless (path = resolve_target_path(target))
        logger.error message_with_context "#{diagram_type} block macro not found: #{target}.", source_location: parent.document.reader.cursor_at_mark
        create_block(parent, 'paragraph', unresolved_block_macro_message(diagram_type, target), {})
      end

      begin
        diagram_text = read(path)
      rescue => e # rubocop:disable Style/RescueStandardError
        logger.error message_with_context "Failed to read #{diagram_type} file: #{path}. #{e}.", source_location: parent.document.reader.cursor_at_mark
        return create_block(parent, 'paragraph', unresolved_block_macro_message(diagram_type, path), {})
      end
      MermaidProcessor.process(self, parent, attrs, diagram_type, diagram_text, @logger)
    end

    protected

    attr_reader :logger

    def resolve_target_path(target)
      target
    end

    def unresolved_block_macro_message(name, target)
      "Unresolved block macro - #{name}::#{target}[]"
    end

    def read(target)
      if target.start_with?('http://') || target.start_with?('https://')
        require 'open-uri'
        ::OpenURI.open_uri(target, &:read)
      else
        File.open(target, &:read)
      end
    end

  end

  # Mermaid API
  #
  module Mermaid
    SUPPORTED_DIAGRAM_NAMES = %w[
      mermaid
    ].freeze
  end

  # Internal processor
  #
  class MermaidProcessor
    include Asciidoctor::Logging

    class << self
      # rubocop:disable Metrics/AbcSize
      def process(processor, parent, attrs, diagram_type, diagram_text, logger)
        doc = parent.document
        # If "subs" attribute is specified, substitute accordingly.
        # Be careful not to specify "specialcharacters" or your diagram code won't be valid anymore!
        if (subs = attrs['subs'])
          diagram_text = parent.apply_subs(diagram_text, parent.resolve_subs(subs))
        end
        title = attrs.delete('title')
        caption = attrs.delete('caption')
        attrs.delete('opts')
        attrs['alt'] = get_alt(attrs)
        html = %(<pre class="mermaid">#{diagram_text}</pre>)
        block = processor.create_open_block parent, [], attrs,  content_model: :compound
        block << (processor.create_pass_block(block, html, {}))
        block.title = title if title
        block.assign_caption(caption, 'figure')
        block
      end
      # rubocop:enable Metrics/AbcSize

      private

      def get_alt(attrs)
        if (title = attrs['title'])
          title
        elsif (target = attrs['target'])
          target
        else
          'Diagram'
        end
      end

      def get_role(format, role)
        if role
          if format
            "#{role} Mermaid-format-#{format} Mermaid"
          else
            "#{role} Mermaid"
          end
        else
          'Mermaid'
        end
      end

    end
  end
end

Extensions.register do
  ::AsciidoctorExtensions::Mermaid::SUPPORTED_DIAGRAM_NAMES.each { |name|
    block_macro AsciidoctorExtensions::MermaidBlockMacroProcessor, name
    block AsciidoctorExtensions::MermaidBlockProcessor, name
  }
end
