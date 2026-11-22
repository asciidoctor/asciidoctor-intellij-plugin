package org.asciidoc.intellij.download;

import org.asciidoc.intellij.arch.Platform;

import java.io.File;

@SuppressWarnings("checkstyle:VisibilityModifier")
public class PandocInfo {
  // https://github.com/jgm/pandoc/releases/
  public static final PandocInfo WINDOWS_INTEL_64 = new PandocInfo(true, "0144b96e8c7b023fd382ec5ef5bdc475e873f1019d6fa72502492df2c9a94a3b", "pandoc.exe", "windows-x86_64.zip");
  public static final PandocInfo LINUX_ARM = new PandocInfo(true, "c692f3d78a415741bbbac4cdbbbe26fa7edca1f43d2301b982fbf4009cc8cf7f", "bin/pandoc", "linux-arm64.tar.gz");
  public static final PandocInfo LINUX_INTEL_64 = new PandocInfo(true, "c5f4720cdefb3f7bdc5d07202ef579feef0d1370773c873804dd8fdaa8c4f1b8", "bin/pandoc", "linux-amd64.tar.gz");
  public static final PandocInfo MAC_ARM64 = new PandocInfo(true, "58d6593884fec9d4424b641b92503c316d9c36decb2891ff8d73ae74dd3b6304", "bin/pandoc", "arm64-macOS.zip");
  public static final PandocInfo MAC_INTEL_64 = new PandocInfo(true, "891708579061e7b1c1a6ce0df30ebb3cb2b6d0ca045ed3c8554ab830c1e0c270", "bin/pandoc", "x86_64-macOS.zip");

  private static volatile PandocInfo myInfo;

  public static final Platform PLATFORM = Platform.identify();
  public static final String VERSION = "3.14";

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
    String suffix =
      switch (PLATFORM) {
        case MAC_INTEL_64 -> "-x86_64";
        case MAC_ARM64 -> "-arm64";
        default -> "";
      };
    return baseDir + File.separator + this.extractionDir + suffix + File.separator + this.binary;
  }

  public String fullArchiveFilename(String baseDir) {
    return baseDir + File.separator + archiveFilename;
  }
}
