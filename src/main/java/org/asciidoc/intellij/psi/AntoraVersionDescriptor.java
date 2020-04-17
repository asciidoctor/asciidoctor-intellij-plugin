package org.asciidoc.intellij.psi;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntoraVersionDescriptor implements Comparable<AntoraVersionDescriptor> {

  private static final Pattern SEMANTIC_PATTERN = Pattern.compile("v?(?<major>[0-9])+\\..*");

  private final String version;
  private final String prerelease;

  private Scheme scheme;

  public enum Scheme implements Comparable<Scheme> {
    NAMED,
    SEMANTIC
  }

  public AntoraVersionDescriptor(String version, String prerelease) {
    this.version = version;
    this.prerelease = prerelease;
    if (version != null) {
      Matcher matcher = SEMANTIC_PATTERN.matcher(version);
      if (matcher.matches()) {
        scheme = Scheme.SEMANTIC;
      } else {
        scheme = Scheme.NAMED;
      }
    }
  }

  @Override
  public int compareTo(@NotNull AntoraVersionDescriptor o) {
    if (version == null && o.version != null) {
      return -1;
    }
    if (version != null && o.version == null) {
      return 1;
    }
    if (version == null) {
      return 0;
    }
    if (prerelease == null && o.prerelease != null) {
      return 1;
    } else if (prerelease != null && o.prerelease == null) {
      return -1;
    } else if (prerelease != null) {
      return prerelease.compareToIgnoreCase(o.prerelease);
    }
    int result = -scheme.compareTo(o.scheme);
    if (result != 0) {
      return result;
    }
    if (scheme == Scheme.NAMED) {
      return version.compareToIgnoreCase(o.version);
    } else {
      return compareSemantic(version, prerelease, o.version, o.prerelease);
    }
  }

  private int compareSemantic(String a, String preA, String b, String preB) {
    StringTokenizer numsA = new StringTokenizer(a, ".");
    StringTokenizer numsB = new StringTokenizer(b, ".");
    for (int i = 0; i < 3; i++) {
      double numA;
      try {
        if (numsA.hasMoreElements()) {
          numA = Double.parseDouble(numsA.nextToken());
        } else {
          numA = 0;
        }
      } catch (NumberFormatException e) {
        numA = Double.NaN;
      }
      double numB;
      try {
        if (numsB.hasMoreElements()) {
          numB = Double.parseDouble(numsB.nextToken());
        } else {
          numB = 0;
        }
      } catch (NumberFormatException e) {
        numB = Double.NaN;
      }
      if (numA > numB) {
        return 1;
      } else if (numB > numA) {
        return -1;
      } else if (Double.isNaN(numA)) {
        if (!Double.isNaN(numB)) {
          return -1;
        }
      } else if (Double.isNaN(numB)) {
        return 1;
      }
    }
    return 0;
  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AntoraVersionDescriptor)) {
      return false;
    }
    return Objects.equals(((AntoraVersionDescriptor) obj).version, version);
  }

  @Override
  public String toString() {
    return version;
  }
}
