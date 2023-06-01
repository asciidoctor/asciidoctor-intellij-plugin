package org.asciidoc.intellij.namesValidator;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocBlockIdStub;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

public class AsciiDocRenameInputValidatorTest {

  private AsciiDocRenameInputValidator validator;
  private PsiElement anypsi;
  private AsciiDocBlockId blockid;

  @Before
  public void setup() throws Exception {
    anypsi = new FakePsiElement() {
      @Override
      public PsiElement getParent() {
        return null;
      }
    };
    validator = new AsciiDocRenameInputValidator();
    blockid = new DummyAsciiDocBlockId();
  }

  @Test
  public void shouldValidateUmlauts() {
    Assert.assertTrue(validator.isInputValid("g\u00e4rtner", anypsi, new ProcessingContext()));
    Assert.assertTrue(validator.isInputValid("g\u00e4rtner", blockid, new ProcessingContext()));
  }

  @Test
  public void shouldRejectWrongCharacters() {
    Assert.assertFalse(validator.isInputValid("-gartner", blockid, new ProcessingContext()));
    Assert.assertTrue(validator.isInputValid("-gartner", anypsi, new ProcessingContext()));
    Assert.assertFalse(validator.isInputValid("gart ner", blockid, new ProcessingContext()));
    Assert.assertFalse(validator.isInputValid("gart!ner", blockid, new ProcessingContext()));
  }

  private static class DummyAsciiDocBlockId implements AsciiDocBlockId {
    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
      return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }

    @Override
    public Icon getIcon(int flags) {
      return null;
    }

    @Override
    public @NotNull Project getProject() throws PsiInvalidElementAccessException {
      return null;
    }

    @Override
    public com.intellij.lang.@NotNull Language getLanguage() {
      return null;
    }

    @Override
    public PsiManager getManager() {
      return null;
    }

    @Override
    public @NotNull PsiElement @NotNull [] getChildren() {
      return new PsiElement[0];
    }

    @Override
    public PsiElement getParent() {
      return null;
    }

    @Override
    public PsiElement getFirstChild() {
      return null;
    }

    @Override
    public PsiElement getLastChild() {
      return null;
    }

    @Override
    public PsiElement getNextSibling() {
      return null;
    }

    @Override
    public PsiElement getPrevSibling() {
      return null;
    }

    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
      return null;
    }

    @Override
    public TextRange getTextRange() {
      return null;
    }

    @Override
    public int getStartOffsetInParent() {
      return 0;
    }

    @Override
    public int getTextLength() {
      return 0;
    }

    @Override
    public @Nullable PsiElement findElementAt(int offset) {
      return null;
    }

    @Override
    public @Nullable PsiReference findReferenceAt(int offset) {
      return null;
    }

    @Override
    public int getTextOffset() {
      return 0;
    }

    @Override
    public @NlsSafe String getText() {
      return null;
    }

    @Override
    public char @NotNull [] textToCharArray() {
      return new char[0];
    }

    @Override
    public PsiElement getNavigationElement() {
      return null;
    }

    @Override
    public PsiElement getOriginalElement() {
      return null;
    }

    @Override
    public boolean textMatches(@NotNull @NonNls CharSequence text) {
      return false;
    }

    @Override
    public boolean textMatches(@NotNull PsiElement element) {
      return false;
    }

    @Override
    public boolean textContains(char c) {
      return false;
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {

    }

    @Override
    public void acceptChildren(@NotNull PsiElementVisitor visitor) {

    }

    @Override
    public PsiElement copy() {
      return null;
    }

    @Override
    public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
      return null;
    }

    @Override
    public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
      return null;
    }

    @Override
    public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
      return null;
    }

    @Override
    public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {

    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
      return null;
    }

    @Override
    public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
      return null;
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
      return null;
    }

    @Override
    public void delete() throws IncorrectOperationException {

    }

    @Override
    public void checkDelete() throws IncorrectOperationException {

    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {

    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
      return null;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public @Nullable PsiReference getReference() {
      return null;
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
      return new PsiReference[0];
    }

    @Override
    public <T> @Nullable T getCopyableUserData(@NotNull Key<T> key) {
      return null;
    }

    @Override
    public <T> void putCopyableUserData(@NotNull Key<T> key, @Nullable T value) {

    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @Nullable PsiElement lastParent, @NotNull PsiElement place) {
      return false;
    }

    @Override
    public @Nullable PsiElement getContext() {
      return null;
    }

    @Override
    public boolean isPhysical() {
      return false;
    }

    @Override
    public @NotNull GlobalSearchScope getResolveScope() {
      return null;
    }

    @Override
    public @NotNull SearchScope getUseScope() {
      return null;
    }

    @Override
    public ASTNode getNode() {
      return null;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
      return false;
    }

    @Override
    public IStubElementType getElementType() {
      return null;
    }

    @Override
    public AsciiDocBlockIdStub getStub() {
      return null;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
      return null;
    }

    @Override
    public @Nullable @NlsSafe String getName() {
      return null;
    }

    @Override
    public PsiElement setName(@NlsSafe @NotNull String name) throws IncorrectOperationException {
      return null;
    }

    @Override
    public @Nullable ItemPresentation getPresentation() {
      return null;
    }

    @Override
    public boolean patternIsValid() {
      return false;
    }
  }
}
