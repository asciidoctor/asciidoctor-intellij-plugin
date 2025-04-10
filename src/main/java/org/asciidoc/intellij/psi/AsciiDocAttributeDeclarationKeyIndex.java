/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author max
 */
package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;

public class AsciiDocAttributeDeclarationKeyIndex extends AsciiDocStringStubIndexExtension<AsciiDocAttributeDeclaration> {
  public static final StubIndexKey<String, AsciiDocAttributeDeclaration> KEY = StubIndexKey.createIndexKey("asciidocAttributeDeclaration.index");

  private static final AsciiDocAttributeDeclarationKeyIndex OUR_INSTANCE = new AsciiDocAttributeDeclarationKeyIndex();

  public static AsciiDocAttributeDeclarationKeyIndex getInstance() {
    return OUR_INSTANCE;
  }

  @Override
  @NotNull
  public StubIndexKey<String, AsciiDocAttributeDeclaration> getKey() {
    return KEY;
  }

  @Override
  public Class<AsciiDocAttributeDeclaration> requiredClass() {
    return AsciiDocAttributeDeclaration.class;
  }

  @Override
  public Collection<AsciiDocAttributeDeclaration> get(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
    try {
      return StubIndex.getElements(getKey(), key.toLowerCase(Locale.US), project, scope, requiredClass());
    } catch (NullPointerException e) {
      if (e.getMessage().startsWith("Can't find stub index extension")) {
        throw IndexNotReadyException.create();
      } else {
        throw e;
      }
    }
  }
}
