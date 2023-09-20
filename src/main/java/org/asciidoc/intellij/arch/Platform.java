package org.asciidoc.intellij.arch;

public enum Platform {
  LINUX_ARM(OS.LINUX, Arch.ARM, "arm64"),
  LINUX_INTEL_64(OS.LINUX, Arch.INTEL, "amd64"),
  WINDOWS_INTEL_64(OS.WINDOWS, Arch.INTEL, "x86_64"),
  MAC_ARM64(OS.MAC, Arch.ARM, "arm64"),
  MAC_INTEL_64(OS.MAC, Arch.INTEL, "x86_64"),
  OTHER(null, null, "");

  public final String archName;
  public final OS os;
  public final Arch arch;

  Platform(OS os, Arch arch, String archName) {
    this.arch = arch;
    this.os = os;
    this.archName = archName;
  }

  public static Platform identify() {
    OS os = OS.identfy();
    Arch arch = Arch.identify();
    for (Platform platform : Platform.values()) {
      if ((platform.os == os) && (platform.arch == arch)) {
        return platform;
      }
    }
    return Platform.OTHER;
  }
}
