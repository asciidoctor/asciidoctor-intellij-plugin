package org.asciidoc.intellij;

import org.junit.Assert;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;

public class AsciiDocSpellcheckingStrategyTest {

  private static <T extends Throwable> T withLanguagesFrame(T throwable) {
    throwable.setStackTrace(new StackTraceElement[]{new StackTraceElement("org.languagetool.Languages", "get", "Languages.java", 82)});
    return throwable;
  }

  @Test
  public void shouldDetectWrappedLanguageToolRace() {
    Throwable wrapped = new RuntimeException(new ExecutionException(withLanguagesFrame(new NullPointerException("lang is null"))));
    Assert.assertTrue(AsciiDocSpellcheckingStrategy.isLanguageToolRace(wrapped));
    Assert.assertTrue(AsciiDocSpellcheckingStrategy.isLanguageToolRace(withLanguagesFrame(new ConcurrentModificationException())));
  }

  @Test
  public void shouldNotHideOtherFailures() {
    Assert.assertFalse(AsciiDocSpellcheckingStrategy.isLanguageToolRace(new NullPointerException()));
    Assert.assertFalse(AsciiDocSpellcheckingStrategy.isLanguageToolRace(withLanguagesFrame(new IllegalStateException())));
  }
}
