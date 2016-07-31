package org.asciidoc.intellij.editor.javafx;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class LocalfileURLHandler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new LocalfileURLConnection(url);
  }
}
