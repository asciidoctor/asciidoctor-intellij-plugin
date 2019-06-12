package org.asciidoc.intellij.editor.javafx;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class LocalfileURLStreamHandlerFactory implements URLStreamHandlerFactory {

  public URLStreamHandler createURLStreamHandler(String protocol) {
    if (protocol.equals("localfile")) {
      return new LocalfileURLHandler();
    }
    return null;
  }
}
