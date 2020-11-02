package org.asciidoc.intellij.conventions;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import java.util.Optional;

import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.asciidoc.intellij")
public class MethodConventionsTest {

  @ArchTest
  public static final ArchRule CHECK_OPTIONAL_USAGE = methods()
    .that(not(modifier(SYNTHETIC)))
    .should()
    .notHaveRawParameterTypes(anyElementThat(type(Optional.class)))
    .as("Optional is primarily intended for use as a method return type where there is a clear need to represent \"no result,\" and where using null is likely to cause errors.");

}