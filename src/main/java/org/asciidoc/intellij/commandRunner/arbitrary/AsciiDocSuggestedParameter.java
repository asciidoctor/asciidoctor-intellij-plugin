package org.asciidoc.intellij.commandRunner.arbitrary;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Suggested parameters that can be very helpful to pass, everytime the interpreter runs.
 *
 * @param parameter        Parameter to pass.
 * @param description      Description of parameter.
 * @param minVersionStable Minimum version of interpreter where this feature became fully stable,
 *                         not when it was first introduced.
 */
public record AsciiDocSuggestedParameter(@NotNull String parameter, @NotNull String description,
                                         @Nullable String minVersionStable) implements Serializable {
}
