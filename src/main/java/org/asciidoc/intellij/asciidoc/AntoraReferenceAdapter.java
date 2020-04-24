package org.asciidoc.intellij.asciidoc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoctor.jruby.ast.impl.PhraseNodeImpl;
import org.jruby.RubyObject;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_AND_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraNavFiles;

public class AntoraReferenceAdapter {

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

  public static void convertAntora(RubyObject node, String type) {
    if (antoraModuleDir != null) {
      PhraseNodeImpl phraseNode = new PhraseNodeImpl(node);
      String outfileSuffix = (String) phraseNode.getDocument().getAttribute("outfilesuffix");
      String target;
      if (type.equals("image")) {
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
      }
      int anchorIndex = target.indexOf('#');
      String anchor = null;
      if (anchorIndex == 0) {
        return;
      }
      if (anchorIndex != -1) {
        anchor = target.substring(anchorIndex + 1);
        target = target.substring(0, anchorIndex - 1);
      }
      if (type.equals("inline_anchor")) {
        if (!target.endsWith(outfileSuffix)) {
          return;
        }
        target = target.substring(0, target.length() - outfileSuffix.length()) + ".adoc";
      }
      String defaultFamily;
      switch (type) {
        case "inline_anchor":
          defaultFamily = "page";
          break;
        case "inline_image":
        case "image":
          defaultFamily = "image";
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + type);
      }
      List<String> replaced = AsciiDocUtil.replaceAntoraPrefix(project, antoraModuleDir, target, defaultFamily);
      if (replaced.size() == 1 && replaced.get(0).equals(target)) {
        // unable to replace
        return;
      }
      target = replaced.get(0);
      VirtualFile sourceDir = LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir);
      VirtualFile targetFile = LocalFileSystem.getInstance().findFileByIoFile(new File(target));
      if (type.equals("inline_anchor") && phraseNode.getText() == null && targetFile != null && sourceDir != null) {
        ApplicationManager.getApplication().runReadAction(() -> {
          PsiFile file = PsiManager.getInstance(project).findFile(targetFile);
          if (file != null) {
            String refText = null;
            Collection<AsciiDocAttributeDeclaration> attributeDeclarations = AsciiDocUtil.findPageAttributes(file);
            for (AsciiDocAttributeDeclaration attributeDeclaration : attributeDeclarations) {
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
              Collection<AsciiDocSection> sections = PsiTreeUtil.findChildrenOfType(file, AsciiDocSection.class);
              for (AsciiDocSection section : sections) {
                if (section.getHeadingLevel() == 1) {
                  refText = section.getTitle();
                }
                // only look at first section == title of document
                break;
              }
            }
            if (refText != null) {
              phraseNode.setString("text", refText);
            }
          }
        });
      }
      String relativePath = null;
      if (sourceDir != null && targetFile != null) {
        relativePath = FileUtil.getRelativePath(fileBaseDir, new File(target));
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
      if (type.equals("image")) {
        phraseNode.setAttribute("target", target, true);
      } else {
        phraseNode.setString("target", target);
      }
    }
  }
}
