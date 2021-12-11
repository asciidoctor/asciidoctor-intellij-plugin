package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;

/**
 * Mix-in to iterate over all entries within the Index.
 */
public abstract class AsciiDocStringStubIndexExtension<T extends PsiElement> extends StringStubIndexExtension<T> {

  @Override
  public int getVersion() {
    return super.getVersion() + AsciiDocElementTypes.FILE.getStubVersion() + 1;
  }

  protected abstract Class<T> requiredClass();

  @SuppressWarnings("UnusedReturnValue")
  public boolean processAllElements(Project project, Processor<? super T> processor, GlobalSearchScope scope) {
    return StubIndex.getInstance().processAllKeys(getKey(),
      key -> {
        for (T element : StubIndex.getElements(getKey(), key, project, scope, requiredClass())) {
          if (!processor.process(element)) {
            return false;
          }
        }
        return true;
      }, scope);
  }
}
