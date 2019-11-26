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


  // IntelliJ community edition includes maven-aether-provider-3.3.9-all.jar that includes commons-lang3,
  // but this is not present for example in RubyMine
  // therefore we need to include it in the distribution if we use it
  // https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_class_loaders.html

  @ArchTest
  public static final ArchRule CHECK_COMMONS_LANG_3 = noClasses()
    .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3")
    .as("unless it is explicitly included in build.gradle, it is a IntelliJ community dependency that is not available in other IDE bundles like RubyMine");
}
