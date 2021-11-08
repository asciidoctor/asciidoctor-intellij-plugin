package org.asciidoc.intellij.psi;

/**
 * Each time the subtree changes, this counter increases.
 * Used for caching attributes that are based on a subtree, for example in
 * {@link org.asciidoc.intellij.grazie.AsciiDocGrazieTextExtractor}.
 * To pass this as a dependency to {@link com.intellij.psi.util.CachedValueProvider.Result#create},
 * create a {@link com.intellij.openapi.util.ModificationTracker} first, as passing a {@link com.intellij.psi.PsiElement}
 * would otherwise invalidate the cache if anything in the file changes.
 * <pre>{@code
 * dep = (ModificationTracker) () -> ((AsciiDocModificationTracker) root).getModificationCount()
 * }</pre>
 * }
 */
public interface AsciiDocModificationTracker {
  long getModificationCount();
}
