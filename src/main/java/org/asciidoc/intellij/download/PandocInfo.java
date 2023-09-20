package org.asciidoc.intellij.download;

import org.asciidoc.intellij.arch.Platform;

import java.io.File;

public enum PandocInfo {
  // https://github.com/jgm/pandoc/releases/
  WINDOWS_INTEL_64("c3541f1a352003498979f2659c2570ac6dd227ec12533b75a76c4d109e75d218", "pandoc.exe", "windows-x86_64.zip"),
  LINUX_ARM("8ac04ce0aedae38f0c9f64bfe634910378cc326d091092395a2140a7ec819d54", "bin/pandoc", "linux-arm64.tar.gz"),
  LINUX_INTEL_64("4e1c607f7e4e9243fa1e1f5b208cd4f1d3f6fd055d5d8c39ba0cdc38644e1c35", "bin/pandoc", "linux-amd64.tar.gz"),
  MAC_ARM64("aa0eab6cf10e5d54d255d68f8fae47e08da071565a3d2b8d242be29a8c1f1460", "bin/pandoc", "arm64-macOS.zip"),
  MAC_INTEL_64("72c43b1de30e67d3a2f69bfd69881e5fcf6ed3c2583c2ad22142c390d185f0b4", "bin/pandoc", "x86_64-macOS.zip"),
  OTHER(null, null, null);

  public static final Platform PLATFORM = Platform.identify();
  public static final String VERSION = "3.1.2";

  public final String hash;
  public final String binary;
  public final String archiveFilename;
  public final String sourceUrl;
  public final String extractionDir;

  PandocInfo(String hash, String binary, String archiveFilename) {
    this.hash = hash;
    this.binary = binary;
    this.extractionDir = "pandoc-" + VERSION;
    this.archiveFilename = "pandoc-" + VERSION + "-" + archiveFilename;
    this.sourceUrl = "https://github.com/jgm/pandoc/releases/download/" + VERSION + "/" + this.archiveFilename;
  }

  public static PandocInfo identify() {
    switch (PLATFORM) {
      case WINDOWS_INTEL_64:
        return PandocInfo.WINDOWS_INTEL_64;
      case MAC_INTEL_64:
        return PandocInfo.MAC_INTEL_64;
      case MAC_ARM64:
        return PandocInfo.MAC_ARM64;
      case LINUX_INTEL_64:
        return PandocInfo.LINUX_INTEL_64;
      case LINUX_ARM:
        return PandocInfo.LINUX_ARM;
      default:
        throw new IllegalStateException("unsupported operating system/arch: " + System.getProperty("os.name"));
    }
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
