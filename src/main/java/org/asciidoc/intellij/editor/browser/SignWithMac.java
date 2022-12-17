package org.asciidoc.intellij.editor.browser;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class SignWithMac {
  private final byte[] macKey;

  public SignWithMac() {
    macKey = new byte[8];
    new Random().nextBytes(macKey);
  }

  /**
   * Sign a file to be encoded in a URL inside a document.
   * The key will change every time the IDE is restarted.
   *
   * @param file filename
   * @return signed file including mac; ready to be added to a URL
   */
  public String signFile(String file) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(macKey, "HmacSHA256");
      mac.init(key);
      String hash = Base64.getEncoder().encodeToString(mac.doFinal(file.getBytes(StandardCharsets.UTF_8)));
      return URLEncoder.encode(file, StandardCharsets.UTF_8) + "&amp;mac=" + hash;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("unable to calculate mac", e);
    }
  }

  public boolean checkMac(@NotNull String file, @NotNull String mac) {
    try {
      Mac m = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(macKey, "HmacSHA256");
      m.init(key);
      String hash = Base64.getEncoder().encodeToString(m.doFinal(file.getBytes(StandardCharsets.UTF_8)));
      mac = mac.replace(" ", "+");
      return hash.equals(mac);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("unable to calculate mac", e);
    }
  }

}
