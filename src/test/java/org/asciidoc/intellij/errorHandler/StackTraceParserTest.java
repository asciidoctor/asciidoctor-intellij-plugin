package org.asciidoc.intellij.errorHandler;

import org.junit.Assert;
import org.junit.Test;

public class StackTraceParserTest {

  @Test
  public void shouldParseFramesAndCauses() {
    Throwable t = StackTraceParser.parse("""
      java.lang.IllegalStateException: failed
      \tat org.asciidoc.Foo.bar(Foo.java:42)
      \tat java.base/java.lang.Thread.run(Thread.java:1474)
      Caused by: java.io.IOException: disk
      \tat org.asciidoc.Baz.<init>(Baz.java:7)
      \t... 2 more
      """);
    Assert.assertEquals("java.lang.IllegalStateException: failed", t.getMessage());
    Assert.assertEquals(2, t.getStackTrace().length);
    Assert.assertEquals("org.asciidoc.Foo", t.getStackTrace()[0].getClassName());
    Assert.assertEquals("bar", t.getStackTrace()[0].getMethodName());
    Assert.assertEquals(42, t.getStackTrace()[0].getLineNumber());
    Assert.assertEquals("java.lang.Thread", t.getStackTrace()[1].getClassName());
    Assert.assertNotNull(t.getCause());
    Assert.assertEquals("java.io.IOException: disk", t.getCause().getMessage());
    Assert.assertEquals("<init>", t.getCause().getStackTrace()[0].getMethodName());
  }

  @Test
  public void shouldKeepTextIfNoStackTrace() {
    Assert.assertNotNull(StackTraceParser.parse(""));
  }
}
