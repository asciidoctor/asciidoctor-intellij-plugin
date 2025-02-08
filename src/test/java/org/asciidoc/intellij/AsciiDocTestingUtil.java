package org.asciidoc.intellij;

public class AsciiDocTestingUtil {
  public static final String TEST_DATA_PATH = findTestDataPath();

  private AsciiDocTestingUtil() {
  }

  private static String findTestDataPath() {
    return "build/resources/test/testData";
  }
}
