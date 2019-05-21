package org.asciidoc.intellij;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtil;

import java.io.File;

public class AsciiDocTestingUtil {
  public static final String TEST_DATA_PATH = findTestDataPath();

  private AsciiDocTestingUtil() {
  }

  private static String findTestDataPath() {
    return "testData";
  }
}
