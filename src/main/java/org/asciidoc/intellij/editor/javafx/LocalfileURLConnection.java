package org.asciidoc.intellij.editor.javafx;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * Inspired by: http://stackoverflow.com/questions/17522343/custom-javafx-webview-protocol-handler. This is a workaround
 * for two things: <ul> <li>Local files will otherwise not be shown in WebView</li> and <li>Images will otherwise be
 * cached</li> </ul>
 */
public class LocalfileURLConnection extends URLConnection {

  private byte[] data;

  protected LocalfileURLConnection(URL url) {
    super(url);
  }

  @Override
  public void connect() throws IOException {
    if (connected) {
      return;
    }
    loadImage();
    connected = true;
  }

  public String getHeaderField(String name) {
    if ("Content-Type".equalsIgnoreCase(name)) {
      return getContentType();
    } else if ("Content-Length".equalsIgnoreCase(name)) {
      return "" + getContentLength();
    }
    return null;
  }

  public String getContentType() {
    String fileName = getURL().getFile();
    String ext = "unknown";
    if (fileName.lastIndexOf('.') != -1) {
      ext = fileName.substring(fileName.lastIndexOf('.') + 1);
      ext = ext.toLowerCase(Locale.US);
      if (ext.equals("svg")) {
        ext = "svg+xml";
      }
    }
    return "image/" + ext; // TODO: switch based on file-type
  }

  public int getContentLength() {
    return data.length;
  }

  public long getContentLengthLong() {
    return data.length;
  }

  public boolean getDoInput() {
    return true;
  }

  public InputStream getInputStream() throws IOException {
    connect();
    return new ByteArrayInputStream(data);
  }

  private void loadImage() throws IOException {
    URL url = getURL();

    String imgPath = url.toExternalForm();
    imgPath = imgPath.startsWith("localfile://") ? imgPath.substring("localfile://".length()) : imgPath.substring("localfile:".length()); // attention: triple '/' is reduced to a single '/'
    // decode URL and remove random number at the beginning
    imgPath = URLDecoder.decode(imgPath, "UTF-8").replaceAll("^[0-9a-z]*/", "");
    if (imgPath.startsWith("/")) {
      // this is needed on Linux/Mac OS, but harmful on Windows
      imgPath = "/" + imgPath;
    }
    try (InputStream stream = new URL("file:/" + imgPath).openStream()) {
      data = IOUtils.toByteArray(stream);
    }
  }

  public OutputStream getOutputStream() {
    // this might be unnecessary - the whole method can probably be omitted for our purposes
    return new ByteArrayOutputStream();
  }

  public java.security.Permission getPermission() {
    return null; // we need no permissions to access this URL
  }

}
