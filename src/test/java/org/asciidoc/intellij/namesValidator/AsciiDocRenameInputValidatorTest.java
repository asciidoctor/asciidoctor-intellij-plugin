package org.asciidoc.intellij.namesValidator;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocBlockIdImpl;
import org.asciidoc.intellij.psi.AsciiDocBlockIdStubImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AsciiDocRenameInputValidatorTest {

  private AsciiDocRenameInputValidator validator;
  private PsiElement anypsi;
  private AsciiDocBlockId blockid;

  @Before
  public void setup() {
    anypsi = new FakePsiElement() {
      @Override
      public PsiElement getParent() {
        return null;
      }
    };
    validator = new AsciiDocRenameInputValidator();
    blockid = new AsciiDocBlockIdImpl(new AsciiDocBlockIdStubImpl(null, "blockid"), AsciiDocElementTypes.BLOCKID);
  }

  @Test
  public void shouldValidateUmlauts() {
    Assert.assertTrue(validator.isInputValid("gärtner", anypsi, new ProcessingContext()));
    Assert.assertTrue(validator.isInputValid("gärtner", blockid, new ProcessingContext()));
  }

  @Test
  public void shouldRejectWrongCharacters() {
    Assert.assertFalse(validator.isInputValid("-gartner", blockid, new ProcessingContext()));
    Assert.assertTrue(validator.isInputValid("-gartner", anypsi, new ProcessingContext()));
    Assert.assertFalse(validator.isInputValid("gart ner", blockid, new ProcessingContext()));
    Assert.assertFalse(validator.isInputValid("gart!ner", blockid, new ProcessingContext()));
  }
}
