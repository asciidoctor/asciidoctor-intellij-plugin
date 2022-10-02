package org.asciidoc.intellij;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class AsciiDocCommenter implements Commenter {
  @Nullable
  @Override
  public String getLineCommentPrefix() {
    return "// ";
  }

  @Nullable
  @Override
  public String getBlockCommentPrefix() {
    return "\n////\n";
  }

  @Nullable
  @Override
  public String getBlockCommentSuffix() {
    return "\n////\n";
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }
}
