package org.asciidoc.intellij.settings.language;

import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * Settings on arbitrary language.
 */
@Data
// No-arg constructor required by IntelliJ XMLB for deserialization.
@NoArgsConstructor
@AllArgsConstructor
/* @Transient on generated getters/setters prevents XMLB from using the accessor path,
 avoiding double serialization alongside the field-level @XCollection annotations. */
@Getter(onMethod_ = {@Transient})
@Setter(onMethod_ = {@Transient})
public class AsciiDocScriptLanguageSetting implements Serializable {
  @Attribute("interpreterPath")
  @NotNull
  private String interpreterPath;

  @Attribute("utilPath")
  @Nullable
  private String utilPath = null;

  @XCollection(propertyElementName = "parameters", elementName = "parameter")
  @Nullable
  private List<String> parameters;

  /**
   * In languages where using temp files isn't optional,
   * but hardcoded (like Go & TypeScript, which <i>must</i> use temp files),
   * this should be always null.
   */
  @Attribute("alwaysUseTempFile")
  @Nullable
  private Boolean alwaysUseTempFile;

  /**
   * Expand parameters into single line,
   * respecting spaces in parameters and double-quote them.
   *
   * @return Expanded parameters.
   */
  @Nullable
  public String expandParameters() {
    if (parameters == null) {
      return null;
    }
    return ParametersListUtil.join(parameters);
  }

  public boolean isValid() {
    //noinspection ConstantValue
    return interpreterPath != null && !interpreterPath.isEmpty();
  }
}
