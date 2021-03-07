package org.asciidoc.intellij.conventions;

import com.intellij.ide.projectView.ProjectView;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.junit.runner.RunWith;

import java.util.Optional;

import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.asciidoc.intellij")
public class MethodConventionsTest {

  @ArchTest
  public static final ArchRule CHECK_OPTIONAL_USAGE = methods()
    .that(not(modifier(SYNTHETIC)))
    .should()
    .notHaveRawParameterTypes(anyElementThat(type(Optional.class)))
    .because("Optional is primarily intended for use as a method return type where there is a clear need to represent \"no result,\" and where using null is likely to cause errors.");

  @ArchTest
  public static final ArchRule CHECK_PROJECT_VIEW_INTERACTION = noClasses().that(not(belongToAnyOf(AsciiDocUtil.class)))
    .should()
    .callMethod(ProjectView.class, "changeView", String.class)
    .because("this should only be done with the safety wrapper AsciiDocUtil.selectFileInProjectView()");

}
