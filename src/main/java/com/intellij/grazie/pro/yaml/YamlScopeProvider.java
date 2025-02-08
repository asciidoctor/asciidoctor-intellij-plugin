package com.intellij.grazie.pro.yaml;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Workaround until <a href="https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1851">#1851</a> is fixed.
 */
public interface YamlScopeProvider {
  @NotNull List<String> getApplicableScopes(@NotNull PsiElement element);
}
