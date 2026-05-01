package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSetting;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Run adhoc JavaScript code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForJavaScript extends AsciiDocRunnerArbitrary {
  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerForJavaScript.class);

  private static final String JAVA_SCRIPT_LANGUAGE_ID = "javascript";

  /**
   * Parameter for input-type module.
   */
  public static final String INPUT_TYPE_MODULE = "--input-type=module";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(Language language) {
    return language != null //
      && language.getID().equalsIgnoreCase(JAVA_SCRIPT_LANGUAGE_ID) //
      && hasInterpreter();
  }

  @Override
  @NotNull List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    var result = super.codeRunParameters(languageSetting);
    if (!useTemporaryFile(languageSetting)) {
      result.add("-e");
    }
    return result;
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.javascript");
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingJavaScript();
  }

  @Nullable
  @Override
  String findInterpreter() {
    if (interpreterCache != null) {
      return interpreterCache;
    }

    interpreterCache = findJavaScriptInterpreter();
    return interpreterCache;
  }

  /**
   * Find the JavaScript interpreter (Node.js) in the following order:
   * <ol>
   *  <li>PATH environment variable.
   *  <li>Common nvm paths on *nix.
   * </ol>
   *
   * @return the path to the JavaScript interpreter, or null if not found
   */
  @Nullable
  public static String findJavaScriptInterpreter() {
    /*
     Using com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager to find the Node.js interpreter,
     configured in IntelliJ, is working basically very well.
     But it isn't a reasonable option, because then the plugin is tied hard to a very specific IntelliJ version,
     and may crash during runtime with a ClassNotFoundException. Even if the class actually exists,
     but is just in another version of the JetBrains JavaScript plugin.
    */

    // Search in PATH
    String[] candidates = SystemInfo.isWindows ? new String[]{"node.exe"} : new String[]{"node", "nodejs"};
    for (String candidate : candidates) {
      File file = PathEnvironmentVariableUtil.findInPath(candidate);
      if (file != null) {
        return file.getAbsolutePath();
      }
    }

    // Check common nvm paths on *nix.
    if (!SystemInfo.isWindows) {
      String home = System.getProperty("user.home");
      String nvmDir = home + "/.nvm/versions/node";
      File nvmRoot = new File(nvmDir);
      if (nvmRoot.isDirectory()) {
        // All node subdirectories have names like "v12.34.56", "v12.34", "v12.34.56-rc.1", etc.
        File[] versions = nvmRoot.listFiles(file -> file.isDirectory() && file.getName().startsWith("v"));
        if (ArrayUtils.isNotEmpty(versions)) {
          SplitNodeJsVersion[] splitNodeJsVersions = Arrays.stream(versions).map(SplitNodeJsVersion::parse)
            .filter(Objects::nonNull).sorted().toArray(SplitNodeJsVersion[]::new);
          for (SplitNodeJsVersion version : splitNodeJsVersions) {
            File nodeCandidate = new File(version.directory(), "bin/node");
            if (nodeCandidate.canExecute()) {
              return nodeCandidate.getAbsolutePath();
            }
          }
        }
      }
    }
    LOG.debug("Couldn't find any JavaScript interpreter anywhere.");
    return null;
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    return List.of(//
      new AsciiDocSuggestedParameter(INPUT_TYPE_MODULE,
        "Use top-level import and await statements.", "14.0"),
      new AsciiDocSuggestedParameter("--no-warnings", "No warnings.", null) //
    );
  }

  @Override
  @NotNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".mjs");
  }
}
