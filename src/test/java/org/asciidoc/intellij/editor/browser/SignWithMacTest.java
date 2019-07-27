package org.asciidoc.intellij.editor.browser;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SignWithMacTest {

  @Test
  public void signedFileMacShouldBeValid() {
    SignWithMac signWithMac = new SignWithMac();
    String file = "file.adoc";
    String fileWithMac = signWithMac.signFile(file);
    String mac = fileWithMac.replaceAll(".*mac=", "");
    assertTrue("mac should match", signWithMac.checkMac(file, mac));
  }

  @Test
  public void signedFileMacShouldBeInvalidIfMacHasChanged() {
    SignWithMac signWithMac = new SignWithMac();
    String file = "file.adoc";
    String fileWithMac = signWithMac.signFile(file);
    String mac = fileWithMac.replaceAll(".*mac=", "");
    assertFalse("mac should not match as mac has changed", signWithMac.checkMac(file, mac + "1"));
  }

  @Test
  public void signedFileMacShouldBeInvalidIfFileHasChanged() {
    SignWithMac signWithMac = new SignWithMac();
    String file = "file.adoc";
    String fileWithMac = signWithMac.signFile(file);
    String mac = fileWithMac.replaceAll(".*mac=", "");
    assertFalse("mac should not match as file as changed", signWithMac.checkMac(file + "1", mac));
  }

  @Test
  public void signedFileMacBeDifferentForEveryInstanceOfSignWithMac() {
    String file = "file.adoc";

    SignWithMac signWithMac1 = new SignWithMac();
    String fileWithMac1 = signWithMac1.signFile(file);
    String mac1 = fileWithMac1.replaceAll(".*mac=", "");

    SignWithMac signWithMac2 = new SignWithMac();
    String fileWithMac2 = signWithMac2.signFile(file);
    String mac2 = fileWithMac2.replaceAll(".*mac=", "");

    assertNotEquals("mac should should be different", mac1, mac2);
  }

}
