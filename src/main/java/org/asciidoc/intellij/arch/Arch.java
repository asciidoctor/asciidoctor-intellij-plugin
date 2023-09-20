package org.asciidoc.intellij.arch;

import java.util.Locale;

public enum Arch {
  ARM,
  INTEL;

  static Arch identify() {
    String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
    if (osArch.contains("arm") || osArch.contains("aarch64")) {
      return Arch.ARM;
    }
    return Arch.INTEL;
  }
}
