package org.asciidoc.intellij.macros;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Knowledge about the positional and named attributes of the macros built into Asciidoctor.
 * The positional attributes are defined by Asciidoctor's parser (<code>parser.rb</code> for block macros and
 * <code>substitutors.rb</code> for inline macros), the named attributes and their descriptions are taken from the
 * <a href="https://docs.asciidoctor.org/asciidoc/latest/">AsciiDoc Language Documentation</a>.
 */
public final class AsciiDocMacroAttributes {

  /**
   * A named attribute of a macro.
   */
  public record Attribute(@NotNull String name, @NotNull String description, @NotNull Map<String, String> values) {
    /**
     * @return the values that can be proposed.
     */
    public @NotNull List<String> suggestions() {
      return List.copyOf(values.keySet());
    }
  }

  /**
   * A macro with its positional attributes (by name, in the order of their position) and its named attributes.
   */
  public record Macro(@NotNull String name, boolean block, @NotNull String description, @NotNull List<String> positional,
                      @NotNull List<Attribute> attributes, @NotNull String docUrl) {
    public @Nullable Attribute find(@NotNull String attributeName) {
      for (Attribute attribute : attributes) {
        if (attribute.name().equals(attributeName)) {
          return attribute;
        }
      }
      return null;
    }

    /**
     * @return the zero-based position of the attribute as positional attribute, or -1 if it can't be positional
     */
    public int positionOf(@NotNull String attributeName) {
      return positional.indexOf(attributeName);
    }
  }

  /**
   * An attribute of a macro, or one of its values if {@code value} is set, used to show documentation.
   */
  public record AttributeRef(@NotNull Macro macro, @NotNull Attribute attribute, @Nullable String value) {
    public AttributeRef(@NotNull Macro macro, @NotNull Attribute attribute) {
      this(macro, attribute, null);
    }

    /**
     * @return the same attribute referring to the given value if it is one of the documented values, otherwise the attribute itself
     */
    public @NotNull AttributeRef withValue(@Nullable String text) {
      if (text != null && attribute.suggestions().contains(text.trim())) {
        return new AttributeRef(macro, attribute, text.trim());
      }
      return new AttributeRef(macro, attribute, null);
    }

    public @Nullable String getValueDescription() {
      if (value == null) {
        return null;
      }
      return attribute.values().get(value);
    }
  }

  private static final String RESOURCE = "/macros/asciidoc-macros.yml";

  private static final List<Macro> MACROS = load();

  private AsciiDocMacroAttributes() {
  }

  @SuppressWarnings("unchecked")
  private static List<Macro> load() {
    try (InputStream stream = AsciiDocMacroAttributes.class.getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Missing resource " + RESOURCE);
      }
      Map<String, Object> root = new Yaml().load(stream);
      Map<String, List<Attribute>> sets = new HashMap<>();
      Map<String, Object> rawSets = (Map<String, Object>) root.getOrDefault("sets", Map.of());
      for (Map.Entry<String, Object> set : rawSets.entrySet()) {
        sets.put(set.getKey(), readAttributes((List<Map<String, Object>>) set.getValue()));
      }
      List<Macro> macros = new ArrayList<>();
      for (Map<String, Object> raw : (List<Map<String, Object>>) root.get("macros")) {
        Map<String, Attribute> attributes = new LinkedHashMap<>();
        for (Object use : (List<Object>) raw.getOrDefault("uses", List.of())) {
          List<Attribute> set = sets.get(String.valueOf(use));
          if (set == null) {
            throw new IllegalStateException("Unknown set " + use + " in " + RESOURCE);
          }
          set.forEach(a -> attributes.put(a.name(), a));
        }
        readAttributes((List<Map<String, Object>>) raw.getOrDefault("attributes", List.of()))
          .forEach(a -> attributes.put(a.name(), a));
        List<String> positional = ((List<Object>) raw.getOrDefault("positional", List.of())).stream().map(String::valueOf).toList();
        macros.add(new Macro(String.valueOf(raw.get("name")), "block".equals(raw.get("type")),
          String.valueOf(raw.get("description")), positional,
          List.copyOf(attributes.values()), String.valueOf(raw.get("doc"))));
      }
      return List.copyOf(macros);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read " + RESOURCE, e);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Attribute> readAttributes(List<Map<String, Object>> raw) {
    List<Attribute> result = new ArrayList<>();
    for (Map<String, Object> entry : raw) {
      Map<String, String> values = new LinkedHashMap<>();
      Map<Object, Object> rawValues = (Map<Object, Object>) entry.get("values");
      if (rawValues != null) {
        rawValues.forEach((k, v) -> values.put(String.valueOf(k), String.valueOf(v)));
      }
      result.add(new Attribute(String.valueOf(entry.get("name")), String.valueOf(entry.get("description")),
        Collections.unmodifiableMap(values)));
    }
    return result;
  }

  public static @NotNull List<Macro> getMacros() {
    return MACROS;
  }

  public static @Nullable Macro find(@NotNull String name, boolean block) {
    for (Macro macro : MACROS) {
      if (macro.name().equals(name) && macro.block() == block) {
        return macro;
      }
    }
    return null;
  }

  public static @Nullable Macro find(@Nullable PsiElement macroElement) {
    if (macroElement instanceof AsciiDocBlockMacro blockMacro) {
      return find(blockMacro.getMacroName(), true);
    }
    if (macroElement instanceof AsciiDocInlineMacro inlineMacro) {
      return find(inlineMacro.getMacroName(), false);
    }
    if (macroElement instanceof AsciiDocLink link) {
      // link: and xref: are parsed as links and not as inline macros
      PsiElement start = link.getFirstChild();
      if (start != null && start.getNode().getElementType() == AsciiDocTokenTypes.LINKSTART) {
        return find(start.getText().replaceAll(":$", ""), false);
      }
    }
    return null;
  }

  /**
   * @return the macro element that contains the element as part of its attributes, or null
   */
  public static @Nullable PsiElement getMacroElement(@NotNull PsiElement element) {
    PsiElement macro = PsiTreeUtil.getParentOfType(element, AsciiDocBlockMacro.class, AsciiDocInlineMacro.class, AsciiDocLink.class);
    if (macro == null) {
      return null;
    }
    // the element needs to be after the start of the attributes
    for (PsiElement child = macro.getFirstChild(); child != null && child.getTextOffset() < element.getTextOffset(); child = child.getNextSibling()) {
      if (child.getNode().getElementType() == AsciiDocTokenTypes.ATTRS_START
        || child.getNode().getElementType() == AsciiDocTokenTypes.INLINE_ATTRS_START) {
        return macro;
      }
    }
    return null;
  }

  public static boolean isNamed(@NotNull AsciiDocAttributeInBrackets attribute) {
    return attribute.getNode().findChildByType(AsciiDocTokenTypes.ASSIGNMENT) != null;
  }

  /**
   * @return the zero-based position of the attribute in the attribute list of its macro.
   */
  public static int getPosition(@NotNull AsciiDocAttributeInBrackets attribute) {
    PsiElement macro = attribute.getParent();
    int position = 0;
    for (PsiElement child = macro.getFirstChild(); child != null && child != attribute; child = child.getNextSibling()) {
      if (child.getNode().getElementType() == AsciiDocTokenTypes.SEPARATOR && child.getText().equals(",")) {
        position++;
      }
    }
    return position;
  }

  public static @Nullable String getName(@NotNull AsciiDocAttributeInBrackets attribute) {
    if (!isNamed(attribute)) {
      return null;
    }
    return attribute.getAttrName();
  }

  /**
   * @param excluding the attribute that is currently being edited, it doesn't count as used
   * @return the names of the attributes of the macro that are already used, either as named or as positional attribute
   */
  public static @NotNull Set<String> getUsedNames(@NotNull PsiElement macroElement, @Nullable PsiElement excluding) {
    Set<String> result = new HashSet<>();
    Macro macro = find(macroElement);
    for (AsciiDocAttributeInBrackets attribute : PsiTreeUtil.getChildrenOfTypeAsList(macroElement, AsciiDocAttributeInBrackets.class)) {
      if (attribute == excluding) {
        continue;
      }
      String name = getName(attribute);
      if (name != null) {
        result.add(name);
      } else if (macro != null) {
        int position = getPosition(attribute);
        if (position >= 0 && position < macro.positional().size()) {
          result.add(macro.positional().get(position));
        }
      }
    }
    return result;
  }

  /**
   * Find the documented attribute for an attribute in the attribute list of a macro, either named or positional.
   */
  public static @Nullable AttributeRef getAttributeRef(@NotNull AsciiDocAttributeInBrackets attribute) {
    PsiElement macroElement = attribute.getParent();
    Macro macro = find(macroElement);
    if (macro == null) {
      return null;
    }
    String name = getName(attribute);
    if (name == null) {
      int position = getPosition(attribute);
      if (position < 0 || position >= macro.positional().size()) {
        return null;
      }
      name = macro.positional().get(position);
    }
    Attribute found = macro.find(name);
    if (found == null) {
      return null;
    }
    return new AttributeRef(macro, found);
  }
}
