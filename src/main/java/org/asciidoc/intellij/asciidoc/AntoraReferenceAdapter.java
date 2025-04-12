package org.asciidoc.intellij.asciidoc;

import com.google.common.html.HtmlEscapers;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.AttributeDeclaration;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.asciidoctor.jruby.ast.impl.PhraseNodeImpl;
import org.jetbrains.annotations.NotNull;
import org.jruby.RubyObject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_AND_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraNavFiles;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class AntoraReferenceAdapter {
  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AntoraReferenceAdapter.class);

  public static void setAntoraDetails(Project project, VirtualFile antoraModuleDir, File fileBaseDir, String name) {
    AntoraReferenceAdapter.project = project;
    AntoraReferenceAdapter.antoraModuleDir = antoraModuleDir;
    AntoraReferenceAdapter.fileBaseDir = fileBaseDir;
    AntoraReferenceAdapter.name = name;
  }

  private static Project project;
  private static VirtualFile antoraModuleDir;
  private static File fileBaseDir;
  private static String name;

  public static void convertInlineAnchor(RubyObject node) {
    convertAntora(node, "inline_anchor");
  }

  public static void convertInlineImage(RubyObject node) {
    convertAntora(node, "inline_image");
  }

  public static void convertImage(RubyObject node) {
    convertAntora(node, "image");
  }

  public static void convertVideo(RubyObject node) {
    convertAntora(node, "video");
   }

  @SuppressWarnings("checkstyle:MethodLength")
  public static void convertAntora(RubyObject node, String type) {
    if (antoraModuleDir != null) {
      PhraseNodeImpl phraseNode = new PhraseNodeImpl(node);
      if (type.equals("inline_image")) {
        String nodeType = phraseNode.getType();
        if (Objects.equals(nodeType, "icon")) {
          return;
        }
      }
      if (type.equals("video")) {
        String poster = (String) phraseNode.getAttribute("poster");
        if ("youtube".equals(poster) || "vimeo".equals(poster)) {
          // don't try to resolve links if the post is set (for example youtube or vimeo)
          return;
        }
      }
      String outfileSuffix = (String) phraseNode.getDocument().getAttribute("outfilesuffix");
      String target;
      if (type.equals("image") || type.equals("video") || type.equals("audio")) {
        target = (String) phraseNode.getAttribute("target");
      } else {
        target = phraseNode.getTarget(); // example$page.html - the link, with .adoc already replaced to .html
      }
      if (target == null) {
        return;
      }
      Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(target);
      if (urlMatcher.find()) {
        return;
      }
      Matcher matcher = ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(target);
      if (matcher.find()) {
        if (matcher.group().length() == 2 && matcher.group().charAt(1) == ':' && target.length() > 2 && target.charAt(2) == '/') {
          // if the second character is a colon, this is probably an already expanded windows path name
          return;
        }
      } else {
        if (type.equals("image") && !target.startsWith("./")) {
          // it is an image, this will be looked up in the imagesdir anyway, no change necessary
          // this ensures that for example generated PlantUML will not have a full path prefix
          return;
        }
      }
      int anchorIndex = target.indexOf('#');
      String anchor = null;
      if (anchorIndex == 0) {
        return;
      }
      if (anchorIndex != -1) {
        anchor = target.substring(anchorIndex + 1);
        target = target.substring(0, anchorIndex);
      }
      if (type.equals("inline_anchor")) {
        if (!target.endsWith(outfileSuffix)) {
          return;
        }
        target = target.substring(0, target.length() - outfileSuffix.length()) + ".adoc";
      }
      String defaultFamily = switch (type) {
        case "inline_anchor" -> "page";
        case "inline_image", "image", "audio", "video" -> "image";
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
      VirtualFile sourceDir = LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir);
      List<String> replaced = AsciiDocUtil.replaceAntoraPrefix(project, antoraModuleDir, sourceDir, target, defaultFamily);
      if (replaced.size() == 1 && replaced.get(0).equals(target)) {
        // unable to replace
        return;
      }
      target = replaced.get(0);
      VirtualFile targetFile = LocalFileSystem.getInstance().findFileByPath(target);
      if (type.equals("inline_anchor") && phraseNode.getText() == null && targetFile != null && sourceDir != null && anchor == null) {
        AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
          if (!targetFile.isValid()) {
            return;
          }
          PsiFile file = null;
          if (targetFile.isValid()) {
            file = PsiManager.getInstance(project).findFile(targetFile);
          }
          if (file != null) {
            String refText = null;
            Collection<AttributeDeclaration> attributeDeclarations = AsciiDocUtil.findPageAttributes(file);
            for (AttributeDeclaration attributeDeclaration : attributeDeclarations) {
              if (attributeDeclaration.getAttributeName().equals("navtitle")) {
                VirtualFile sourceFile = sourceDir.findChild(name);
                if (sourceFile != null) {
                  Collection<VirtualFile> antoraNavFiles = findAntoraNavFiles(project, antoraModuleDir);
                  if (antoraNavFiles.contains(sourceFile)) {
                    refText = attributeDeclaration.getAttributeValue();
                    break;
                  }
                }
              }
              if (attributeDeclaration.getAttributeName().equals("reftext")) {
                refText = attributeDeclaration.getAttributeValue();
                // don't break, there might be a "navtitle"
              }
            }
            if (refText == null) {
              for (AttributeDeclaration attributeDeclaration : attributeDeclarations) {
                if (attributeDeclaration.getAttributeName().equals("doctitle")) {
                  refText = attributeDeclaration.getAttributeValue();
                  break;
                }
              }
            }
            if (refText != null) {
              String refTextResolved = AsciiDocUtil.resolveAttributes(file, refText, AsciiDocUtil.Scope.PAGEATTRIBUTES);
              if (refTextResolved != null) {
                refText = refTextResolved;
              }
              refText = simpleAsciiDocParsing(refText);
              phraseNode.setString("text", refText);
            }
          }
        });
      }
      String relativePath = null;
      if (sourceDir != null && targetFile != null) {
        if (type.equals("image") || type.equals("inline_image") || type.equals("video") || type.equals("audio")) {
          // compute relative path from imagesdir for images as Asciidoctor will prepend this
          String imagesdir = (String) phraseNode.getDocument().getAttribute("imagesdir");
          File source;
          if (imagesdir != null) {
            source = new File(fileBaseDir, imagesdir);
            try {
              // resolve all relative directories with ".." as FileUtil.getRelativePath will otherwise not work
              source = source.getCanonicalFile();
            } catch (IOException e) {
              LOG.info("unable to compute canonical file from '" + fileBaseDir + "' and '" + imagesdir + "'", e);
            }
          } else {
            source = fileBaseDir;
          }
          relativePath = FileUtil.getRelativePath(source, new File(target));
        } else {
          relativePath = FileUtil.getRelativePath(fileBaseDir, new File(target));
        }
        if (relativePath != null) {
          relativePath = relativePath.replaceAll("\\\\", "/");
        }
      }
      if (relativePath != null) {
        // can be null if on different file system
        target = relativePath;
      } else {
        target = "file:///" + target;
      }
      if (type.equals("inline_anchor")) {
        if (target.endsWith(".adoc")) {
          target = target.substring(0, target.length() - 5) + outfileSuffix;
        }
      }
      if (anchor != null) {
        target = target + "#" + anchor;
      }
      if (type.equals("image") || type.equals("video") || type.equals("audio")) {
        phraseNode.setAttribute("target", target, true);
      } else {
        phraseNode.setString("target", target);
      }
    }
  }

  private static final HashMap<IElementType, String> HTML_TOKEN_TRANSLATION = new HashMap<>();
  static {
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.ITALIC_START, "<em>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.ITALIC_END, "</em>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEITALIC_START, "<em>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEITALIC_END, "</em>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.BOLD_START, "<b>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.BOLD_END, "</b>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEBOLD_START, "<b>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEBOLD_END, "</b>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START, "");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END, "");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.MONO_START, "<code>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.MONO_END, "</code>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEMONO_START, "<code>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.DOUBLEMONO_END, "</code>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.SUBSCRIPT_START, "<sub>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.SUBSCRIPT_END, "</sub>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.SUPERSCRIPT_START, "<sup>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.SUPERSCRIPT_END, "</sup>");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START, "\u201D");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END, "\u201C");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START, "\u2019");
    HTML_TOKEN_TRANSLATION.put(AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END, "\u2018");
  }

  /**
   * This will translate simple formatting (especially quotes) to HTML to gradually improve the display of tocs and xrefs.
   */
  @NotNull
  private static String simpleAsciiDocParsing(String asciiDoc) {
    AsciiDocLexer lexer = new AsciiDocLexer();
    lexer.start(asciiDoc);
    StringBuilder sb = new StringBuilder();
    while (lexer.getTokenStart() != lexer.getBufferEnd()) {
      String replacement = HTML_TOKEN_TRANSLATION.get(lexer.getTokenType());
      if (replacement != null) {
        sb.append(replacement);
      } else if (lexer.getTokenType() == AsciiDocTokenTypes.PASSTRHOUGH_CONTENT) {
        sb.append(lexer.getTokenText());
      } else {
        sb.append(HtmlEscapers.htmlEscaper().escape(lexer.getTokenText()));
      }
      lexer.advance();
    }
    return sb.toString();
  }
}
