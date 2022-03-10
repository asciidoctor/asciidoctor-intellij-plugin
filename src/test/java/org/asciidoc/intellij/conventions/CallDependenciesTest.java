package org.asciidoc.intellij.conventions;

import com.intellij.psi.PsiElement;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.asciidoc.intellij")
public class CallDependenciesTest {

  @ArchTest
  public static final ArchRule CHECK_CALLS_IN_TITLE = noMethods()
    .that().areDeclaredInClassesThat().implement(PsiElement.class).should(new ArchCondition<>("calling to foldedSummary") {
      @Override
      public void check(JavaMethod item, ConditionEvents events) {
        if (item.getMethodCallsFromSelf().stream().anyMatch(javaMethodCall -> javaMethodCall.getName().equals("getFoldedSummary"))) {
          events.add(new SimpleConditionEvent(item, true, "calls getFoldedSummary in " + item.getFullName()));
        }
      }
    })
    .as("this could lead to a recursion");

}
