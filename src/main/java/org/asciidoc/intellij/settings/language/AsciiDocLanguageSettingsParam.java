package org.asciidoc.intellij.settings.language;

import lombok.Builder;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocSuggestedParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Parameters for setting up language settings, used in {@link AsciiDocScriptLanguagesForm}.
 *
 * @param getInterpreter              Gets the default interpreter path for the language
 *                                    (like <code>C:\nvm4w\nodejs\node.exe</code> for JavaScript under Windows).
 * @param getUtil                     Gets the default util path for the language, if applicable
 *                                    (like <code>C:\nvm4w\nodejs\npx.ps1</code> for TypeScript under Windows).
 * @param suggestedParameterExtractor Gets a list of the suggested parameters.
 * @param explanationText             Explanation text to the language execution.
 */
@Builder
record AsciiDocLanguageSettingsParam(@NotNull Supplier<String> getInterpreter,
                                     @Nullable Supplier<String> getUtil,
                                     @NotNull Supplier<List<AsciiDocSuggestedParameter>> suggestedParameterExtractor,
                                     @NotNull String explanationText,
                                     boolean mustUseTemporaryFile) {
}
