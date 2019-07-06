package org.asciidoc.intellij.highlighting;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AsciiDocColorSettingsPage implements ColorSettingsPage {

  private static final AttributesDescriptor[] ATTRIBUTE_DESCRIPTORS = AttributeDescriptorsHolder.INSTANCE.get();

  @Override
  @NotNull
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    final Map<String, TextAttributesKey> result = new HashMap<>();

    result.put("comment", AsciiDocSyntaxHighlighter.ASCIIDOC_COMMENT);
    result.put("heading", AsciiDocSyntaxHighlighter.ASCIIDOC_HEADING);
    result.put("bullet", AsciiDocSyntaxHighlighter.ASCIIDOC_BULLET);
    result.put("description", AsciiDocSyntaxHighlighter.ASCIIDOC_DESCRIPTION);
    result.put("enumeration", AsciiDocSyntaxHighlighter.ASCIIDOC_ENUMERATION);
    result.put("callout", AsciiDocSyntaxHighlighter.ASCIIDOC_CALLOUT);
    result.put("block_macro", AsciiDocSyntaxHighlighter.ASCIIDOC_BLOCK_MACRO_ID);
    result.put("marker", AsciiDocSyntaxHighlighter.ASCIIDOC_MARKER);
    result.put("attribute", AsciiDocSyntaxHighlighter.ASCIIDOC_ATTRIBUTE);
    result.put("value", AsciiDocSyntaxHighlighter.ASCIIDOC_ATTRIBUTE_VAL);
    result.put("listing", AsciiDocSyntaxHighlighter.ASCIIDOC_LISTING_TEXT);

    result.put("bold", AsciiDocSyntaxHighlighter.ASCIIDOC_BOLD);
    result.put("italic", AsciiDocSyntaxHighlighter.ASCIIDOC_ITALIC);
    result.put("mono", AsciiDocSyntaxHighlighter.ASCIIDOC_MONO);
    result.put("bolditalic", AsciiDocSyntaxHighlighter.ASCIIDOC_BOLDITALIC);
    result.put("monobold", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLD);
    result.put("monoitalic", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOITALIC);
    result.put("monobolditalic", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLDITALIC);

    return result;
  }

  @Override
  @NotNull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRIBUTE_DESCRIPTORS;
  }

  @Override
  @NotNull
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  @NonNls
  @NotNull
  public String getDemoText() {
    final InputStream stream = getClass().getResourceAsStream("SampleDocument.adoc");
    try {
      final String result = StreamUtil.readText(stream, CharsetToolkit.UTF8);
      stream.close();
      return StringUtil.convertLineSeparators(result);
    } catch (IOException ignored) {
      return "*error loading text*";
    }
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return AsciiDocBundle.message("settings.asciidoc.preview.name");
  }

  @Override
  @NotNull
  public SyntaxHighlighter getHighlighter() {
    return new AsciiDocSyntaxHighlighter();
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return null;
  }

  private enum AttributeDescriptorsHolder {
    INSTANCE;

    private final Map<String, TextAttributesKey> myMap = new HashMap<>();

    AttributeDescriptorsHolder() {
      put("asciidoc.editor.colors.comment", AsciiDocSyntaxHighlighter.ASCIIDOC_COMMENT);
      put("asciidoc.editor.colors.heading", AsciiDocSyntaxHighlighter.ASCIIDOC_HEADING);
      put("asciidoc.editor.colors.bullet", AsciiDocSyntaxHighlighter.ASCIIDOC_BULLET);
      put("asciidoc.editor.colors.description", AsciiDocSyntaxHighlighter.ASCIIDOC_DESCRIPTION);
      put("asciidoc.editor.colors.enumeration", AsciiDocSyntaxHighlighter.ASCIIDOC_ENUMERATION);
      put("asciidoc.editor.colors.callout", AsciiDocSyntaxHighlighter.ASCIIDOC_CALLOUT);
      put("asciidoc.editor.colors.block_macro", AsciiDocSyntaxHighlighter.ASCIIDOC_BLOCK_MACRO_ID);
      put("asciidoc.editor.colors.marker", AsciiDocSyntaxHighlighter.ASCIIDOC_MARKER);
      put("asciidoc.editor.colors.attribute", AsciiDocSyntaxHighlighter.ASCIIDOC_ATTRIBUTE);
      put("asciidoc.editor.colors.value", AsciiDocSyntaxHighlighter.ASCIIDOC_ATTRIBUTE_VAL);
      put("asciidoc.editor.colors.listing", AsciiDocSyntaxHighlighter.ASCIIDOC_LISTING_TEXT);

      put("asciidoc.editor.colors.bold", AsciiDocSyntaxHighlighter.ASCIIDOC_BOLD);
      put("asciidoc.editor.colors.italic", AsciiDocSyntaxHighlighter.ASCIIDOC_ITALIC);
      put("asciidoc.editor.colors.mono", AsciiDocSyntaxHighlighter.ASCIIDOC_MONO);
      put("asciidoc.editor.colors.bolditalic", AsciiDocSyntaxHighlighter.ASCIIDOC_BOLDITALIC);
      put("asciidoc.editor.colors.monobold", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLD);
      put("asciidoc.editor.colors.monoitalic", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOITALIC);
      put("asciidoc.editor.colors.monobolditalic", AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLDITALIC);
    }

    @NotNull
    public AttributesDescriptor[] get() {
      final AttributesDescriptor[] result = new AttributesDescriptor[myMap.size()];
      int i = 0;

      for (Map.Entry<String, TextAttributesKey> entry : myMap.entrySet()) {
        result[i++] = new AttributesDescriptor(AsciiDocBundle.message(entry.getKey()), entry.getValue());
      }

      return result;
    }

    private void put(@NotNull String bundleKey, @NotNull TextAttributesKey attributes) {
      if (myMap.put(bundleKey, attributes) != null) {
        throw new IllegalArgumentException("Duplicated key: " + bundleKey);
      }
    }
  }
}
