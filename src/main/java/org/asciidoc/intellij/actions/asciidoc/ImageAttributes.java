package org.asciidoc.intellij.actions.asciidoc;

import java.util.Optional;

public interface ImageAttributes {

  Optional<Integer> getWidth();

  Optional<String> getAlt();
}
