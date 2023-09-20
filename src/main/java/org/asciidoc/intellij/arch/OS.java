package org.asciidoc.intellij.arch;

import com.intellij.openapi.util.SystemInfoRt;

public enum OS {
  LINUX(SystemInfoRt.isLinux),
  MAC(SystemInfoRt.isMac),
  WINDOWS(SystemInfoRt.isWindows),
  OTHER(true);

  public final boolean active;

  OS(boolean active) {
    this.active = active;
  }

  /**
   * Get active OS.
   */
  public static OS identfy() {
    for (OS os : OS.values()) {
      if (os.active) {
        return os;
      }
    }
    return OS.OTHER;
  }
}
