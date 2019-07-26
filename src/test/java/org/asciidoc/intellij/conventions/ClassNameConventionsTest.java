package org.asciidoc.intellij.conventions;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.asciidoc.intellij")
public class ClassNameConventionsTest {

  @ArchTest
  public static final ArchRule CHECK_PREFIX_ASCII_DOC = noClasses()
    .should().haveSimpleNameStartingWith("Asciidoc").as("The AsciiDoc prefix should have a capital d (AsciiDoc, not Asciidoc)");
}
