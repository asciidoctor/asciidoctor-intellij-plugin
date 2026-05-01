package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSetting;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Run adhoc TypeScript code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForTypeScript extends AsciiDocRunnerArbitrary {
  public static final String NPX_EXECUTABLE = SystemInfo.isWindows ? "npx.ps1" : "npx";

  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerForTypeScript.class);

  private static final String TYPE_SCRIPT_LANGUAGE_ID = "typescript";
  private static final String PATH_KEY = "PATH";

  private static String interpreterCache = null;
  private static String npxScriptCache = null;

  @Override
  public boolean isApplicable(Language language) {
    return language != null //
      && language.getID().equalsIgnoreCase(TYPE_SCRIPT_LANGUAGE_ID) //
      && hasInterpreter();
  }

  @Override
  @Nullable List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    final List<String> strings;
    if (SystemInfo.isWindows) {
      String utilPath = Optional.ofNullable(
          AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getScriptLanguageSettings())
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingTypeScript)
        .map(AsciiDocScriptLanguageSetting::getUtilPath)
        .orElse(null);
      String npxScript;
      if (utilPath == null) {
        npxScript = findNpxScript();
      } else {
        npxScript = utilPath;
      }
      if (StringUtils.isBlank(npxScript)) {
        return null;
      }
      // Under Windows, it's "powershell.exe -File npx.ps1 tsx ...".
      strings = new ArrayList<>(List.of("-File", npxScript));
    } else {
      // Under Linux, it's "npx tsx ...".
      strings = new ArrayList<>();
    }
    strings.add("tsx");
    if (languageSetting != null && languageSetting.getParameters() != null) {
      strings.addAll(languageSetting.getParameters());
    }
    if (!useTemporaryFile(languageSetting)) {
      strings.add("-e");
    }
    return strings;
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.typescript");
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingTypeScript();
  }

  @Nullable
  @Override
  String findInterpreter() {
    if (interpreterCache != null) {
      return interpreterCache;
    }

    interpreterCache = findNodePackageExecutor();
    return interpreterCache;
  }

  @Nullable
  public static String findNodePackageExecutor() {
    if (SystemInfo.isWindows) {
      return AsciiDocRunnerForPowershell.findPowerShellInterpreter();
    } else {
      String npxScript = findNpxScript();
      if (npxScript != null) {
        return npxScript;
      }
    }

    // Search in PATH
    File npxFile = PathEnvironmentVariableUtil.findInPath(NPX_EXECUTABLE);
    if (npxFile != null) {
      return npxFile.getAbsolutePath();
    }

    /* Check common nvm (node version manager) paths on *nix.
     * If this is Windows, the method is already left early. */
    String home = System.getProperty("user.home");
    String nvmDir = home + "/.nvm/versions/node";
    File nvmRoot = new File(nvmDir);
    if (nvmRoot.isDirectory()) {
      // All node subdirectories have names like "v12.34.56", "v12.34", "v12.34.56-rc.1", etc.
      File[] versions = nvmRoot.listFiles(file -> file.isDirectory() && file.getName().startsWith("v"));
      if (ArrayUtils.isNotEmpty(versions)) {
        SplitNodeJsVersion[] splitNodeJsVersions = Arrays.stream(versions).map(SplitNodeJsVersion::parse).filter(
          Objects::nonNull).sorted().toArray(SplitNodeJsVersion[]::new);
        for (SplitNodeJsVersion version : splitNodeJsVersions) {
          File npxCandidate = new File(version.directory(), "bin/npx");
          if (npxCandidate.canExecute()) {
            return npxCandidate.getAbsolutePath();
          }
        }
      }
    }
    LOG.debug("Couldn't find any Node.js Package Executor (npx) anywhere.");
    return null;
  }

  @Nullable
  public static String findNpxScript() {
    if (npxScriptCache != null) {
      return npxScriptCache;
    }
    // Search for "npx" in the "node" path (if Node.js is configured at all).
    String node = AsciiDocRunnerForJavaScript.findJavaScriptInterpreter();
    if (node != null) {
      File nodeFile = new File(node);
      File nodeDir = nodeFile.getParentFile();
      if (nodeDir != null && nodeDir.isDirectory()) {
        File npxFile = new File(nodeDir, NPX_EXECUTABLE);
        if (npxFile.canExecute()) {
          LOG.info("Found npx in Node.js directory: \"" + npxFile.getAbsolutePath() + "\"");
          npxScriptCache = npxFile.getAbsolutePath();
          return npxScriptCache;
        }
      }
    }
    return null;
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    List<AsciiDocSuggestedParameter> parameters = new ArrayList<>(List.of(//
      new AsciiDocSuggestedParameter("--env-file=<path>", "Set environment variables via file.", null)));
    /* Under Windows scripts aren't executed via inline "-e" evaluation, but written into an extra file,
    where the "--input-type=module" parameter isn't available. */
    if (!SystemInfo.isWindows) {
      parameters.add(
        new AsciiDocSuggestedParameter("--input-type=module", "Use top-level import and await statements. Incompatible w/ temp-file.",
          "14.0"));
    }
    parameters.addAll(List.of(
      new AsciiDocSuggestedParameter("--no-cache", "Disable TypeScript transpilation cache.", null),
      new AsciiDocSuggestedParameter("--no-warnings", "No warnings.", null),
      new AsciiDocSuggestedParameter("--tsconfig=<path>", "Specify tsconfig.json path.", null))
    );
    return parameters;
  }

  @Override
  @Nullable Map.Entry<String, String> specialEnvironment() {
    String interpreter = findInterpreter();
    if (interpreter != null) {
      File nodeDir = new File(interpreter).getParentFile();
      if (nodeDir != null && nodeDir.isDirectory()) {
        String systemPath = System.getenv(PATH_KEY);
        if (systemPath != null) {
          String absoluteNodePath = nodeDir.getAbsolutePath();
          if (!systemPath.contains(absoluteNodePath)) {
            String pathSeparator = SystemInfo.isWindows ? ";" : ":";
            return new AbstractMap.SimpleImmutableEntry<>(PATH_KEY, absoluteNodePath + pathSeparator + systemPath);
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns
   * <ul>
   * <li><code>true</code> under Windows.
   * <li><code>false</code> under Linux / macOS.
   * </ul>
   * Windows (double-) quotes handling is so bad, that even with the
   * best script file (<code>npx.ps1</code>, not <code>npx.cmd</code>),
   * it's still too error-prone.
   * The whole code <em>must</em> be written to a temporary file and then be executed.
   * Using <code>node.exe</code> under Windows directly, with the <code>npx-cli.js</code>,
   * isn't working reliably too.
   */
  @Override
  boolean mustUseTemporaryFile() {
    return SystemInfo.isWindows;
  }

  @Override
  @NonNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".mts");
  }

  /**
   * PowerShell (only under Windows) waits forever for an input with interactivity enabled.<br>
   * Under *nix the <code>npx</code> bash script is used and no PowerShell involved.
   *
   * @return <code>System != Windows</code>.
   */
  @Override
  boolean isInteractiveCommand() {
    return !SystemInfo.isWindows;
  }
}
