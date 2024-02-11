package org.asciidoc.intellij.download;

import org.asciidoc.intellij.arch.Platform;

import java.io.File;

@SuppressWarnings("checkstyle:VisibilityModifier")
public class PandocInfo {
  // https://github.com/jgm/pandoc/releases/
  public static final PandocInfo WINDOWS_INTEL_64 = new PandocInfo(true, "c3541f1a352003498979f2659c2570ac6dd227ec12533b75a76c4d109e75d218", "pandoc.exe", "windows-x86_64.zip");
  public static final PandocInfo LINUX_ARM = new PandocInfo(true, "8ac04ce0aedae38f0c9f64bfe634910378cc326d091092395a2140a7ec819d54", "bin/pandoc", "linux-arm64.tar.gz");
  public static final PandocInfo LINUX_INTEL_64 = new PandocInfo(true, "4e1c607f7e4e9243fa1e1f5b208cd4f1d3f6fd055d5d8c39ba0cdc38644e1c35", "bin/pandoc", "linux-amd64.tar.gz");
  public static final PandocInfo MAC_ARM64 = new PandocInfo(true, "aa0eab6cf10e5d54d255d68f8fae47e08da071565a3d2b8d242be29a8c1f1460", "bin/pandoc", "arm64-macOS.zip");
  public static final PandocInfo MAC_INTEL_64 = new PandocInfo(true, "72c43b1de30e67d3a2f69bfd69881e5fcf6ed3c2583c2ad22142c390d185f0b4", "bin/pandoc", "x86_64-macOS.zip");

  private static volatile PandocInfo myInfo;

  public static final Platform PLATFORM = Platform.identify();
  public static final String VERSION = "3.1.2";

  public final boolean supported;
  public final String hash;
  public final String binary;
  public final String archiveFilename;
  public final String sourceUrl;
  public final String extractionDir;

  PandocInfo(boolean supported, String hash, String binary, String archiveFilename) {
    this.supported = supported;
    this.hash = hash;
    this.binary = binary;
    this.extractionDir = "pandoc-" + VERSION;
    this.archiveFilename = "pandoc-" + VERSION + "-" + archiveFilename;
    this.sourceUrl = "https://github.com/jgm/pandoc/releases/download/" + VERSION + "/" + this.archiveFilename;
  }

  public static PandocInfo identify() {
    if (myInfo == null) {
      synchronized (PandocInfo.class) {
        switch (PLATFORM) {
          case WINDOWS_INTEL_64:
            myInfo = PandocInfo.WINDOWS_INTEL_64;
            break;
          case WINDOWS_ARM:
            if ("Windows 10".equals(System.getProperty("os.name"))) {
              myInfo = new PandocInfo(false, null, null, null);
            }
            // Windows 11+ on ARM is able to run unmodified x64 Windows apps
            myInfo = PandocInfo.WINDOWS_INTEL_64;
            break;
          case MAC_INTEL_64:
            myInfo = PandocInfo.MAC_INTEL_64;
            break;
          case MAC_ARM64:
            myInfo = PandocInfo.MAC_ARM64;
            break;
          case LINUX_INTEL_64:
            myInfo = PandocInfo.LINUX_INTEL_64;
            break;
          case LINUX_ARM:
            myInfo = PandocInfo.LINUX_ARM;
            break;
          default:
            myInfo = new PandocInfo(false, null, null, null);
        }
      }
    }
    return myInfo;
  }

  public String fullBinaryPath(String baseDir) {
    String suffix = "";
    switch (PLATFORM) {
      case MAC_INTEL_64:
        suffix = "-x86_64";
        break;
      case MAC_ARM64:
        suffix = "-arm64";
        break;
      default:
        break;
    }
    return baseDir + File.separator + this.extractionDir + suffix + File.separator + this.binary;
  }

  public String fullArchiveFilename(String baseDir) {
    return baseDir + File.separator + archiveFilename;
  }
}
