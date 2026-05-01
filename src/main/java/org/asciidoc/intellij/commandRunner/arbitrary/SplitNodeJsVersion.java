package org.asciidoc.intellij.commandRunner.arbitrary;

import com.google.common.base.Splitter;
import com.google.common.primitives.ImmutableIntArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.List;

record SplitNodeJsVersion(@NotNull File directory, ImmutableIntArray numbers,
                          @Nullable String suffix) implements Comparable<SplitNodeJsVersion> {

  public static final Splitter DASH_SPLITTER = Splitter.on('-').trimResults().omitEmptyStrings();
  public static final Splitter DOT_SPLITTER = Splitter.on('.').trimResults().omitEmptyStrings();

  @Nullable
  static SplitNodeJsVersion parse(@NotNull File directory) {
    String version = directory.getName();
    if (!version.startsWith("v")) {
      return null;
    }
    // Split of release candidates.
    List<String> suffixParts = DASH_SPLITTER.splitToList(version.substring(1));
    // Parse the pre-release-candidates.
    List<String> numberParts = DOT_SPLITTER.splitToList(suffixParts.getFirst());
    int[] numbers = new int[numberParts.size()];
    for (int i = 0; i < numberParts.size(); i++) {
      try {
        numbers[i] = Integer.parseInt(numberParts.get(i));
      } catch (NumberFormatException e) {
        // Just leave it at the default: 0.
        break;
      }
    }
    return new SplitNodeJsVersion(directory, ImmutableIntArray.builder().addAll(numbers).build(),
      suffixParts.size() > 1 ? suffixParts.get(1) : null);
  }

  @Override
  public int compareTo(@NonNull SplitNodeJsVersion other) {
    for (int i = 0; i < Math.max(numbers.length(), other.numbers.length()); i++) {
      int numA = i < numbers.length() ? numbers.get(i) : 0;
      int numB = i < other.numbers.length() ? other.numbers.get(i) : 0;
      if (numA != numB) {
        // Descending.
        return numB - numA;
      }
    }
    if (suffix != null) {
      if (other.suffix != null) {
        return suffix.compareToIgnoreCase(other.suffix) * -1;
      } else {
        return 1;
      }
    } else if (other.suffix != null) {
      return 1;
    } else {
      return 0;
    }
  }
}
