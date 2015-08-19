package org.asciidoc.intellij.web;

import com.intellij.ide.browsers.OpenInBrowserRequest;
import com.intellij.ide.browsers.WebBrowserUrlProvider;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * @author Erik Pragt
 */
public class AsciiDocBrowserUrlProvider extends WebBrowserUrlProvider {
  @Override
  public boolean canHandleElement(OpenInBrowserRequest request) {
    if(request.getFile().getFileType() instanceof AsciiDocFileType) {
      return true;
    }

    return super.canHandleElement(request);
  }
}
