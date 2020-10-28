package org.asciidoc.intellij.conventions;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.asciidoc.intellij")
public class TestMethodNameConventionsTest {

  @ArchTest
  public static final ArchRule CHECK_TEST_METHOD_PREFIX = methods()
    .that().areAnnotatedWith("org.junit.Test")
    .should().haveNameMatching("[should,test](.*?)")
    .as("Test method names should begin with 'test' or 'should'");
}
