package org.asciidoc.intellij.editor.jcef;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefApp;
import org.cef.CefApp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Recover from a terminated JCEF server process.
 * This happens for example after the computer was suspended, and then every new JCEF panel fails with an NPE inside the JCEF classes.
 * As of 2026.2 and 2026.3, the IDE offers no public API to start a new JCEF server, therefore use the same method that the IDE's
 * "Restart JCEF" action uses.
 */
public final class AsciiDocJcefRecovery {

  private static final Logger LOG = Logger.getInstance(AsciiDocJcefRecovery.class);

  private AsciiDocJcefRecovery() {
  }

  /**
   * Check if the exception originates from within the JCEF classes, which is the case when the JCEF server isn't available.
   */
  public static boolean isJcefServerFailure(RuntimeException ex) {
    StackTraceElement[] trace = ex.getStackTrace();
    if (trace.length == 0) {
      return false;
    }
    if (ex instanceof IllegalStateException) {
      return String.valueOf(ex.getMessage()).contains("TERMINATED") && trace[0].getClassName().startsWith("org.cef.");
    }
    String className = trace[0].getClassName();
    return className.startsWith("com.jetbrains.cef.") || className.startsWith("org.cef.");
  }

  /**
   * Ask the IDE to start a new JCEF server.
   * The IDE has no public API for this, therefore use reflection.
   * Failures are logged and never propagated, as the internals might change in any IDE version.
   *
   * @return true if a new JCEF server has been started
   */
  public static boolean restartJcef() {
    try {
      JBCefApp app = JBCefApp.getInstance();
      boolean result;
      try {
        // IDE 2026.2 and earlier
        result = invoke(app, JBCefApp.class.getDeclaredMethod("restartJCEF", boolean.class, boolean.class));
        if (result) {
          registerSchemeHandlers();
        }
      } catch (NoSuchMethodException e) {
        // IDE 2026.3 and later: the same logic moved to a starter that is used by the IDE's notification about a disconnected JCEF process
        Field starterField = JBCefApp.class.getDeclaredField("myStarter");
        starterField.setAccessible(true);
        Object starter = starterField.get(app);
        result = invoke(starter, starter.getClass().getDeclaredMethod("initCefApp", boolean.class, boolean.class));
      }
      LOG.warn("Restarted JCEF as the JCEF process was not available, success: " + result);
      return result;
    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
      LOG.warn("Unable to restart JCEF", e);
      return false;
    }
  }

  /**
   * The IDE registers its scheme handlers (for example for loading HTML into a browser) when the context of the first CEF app is initialized.
   * On IDE 2026.2 this doesn't reach the restarted app, and the preview then shows ERR_FILE_NOT_FOUND, therefore register them again.
   */
  private static void registerSchemeHandlers() {
    try {
      CefApp cefApp = CefApp.getInstance();
      for (JBCefApp.JBCefCustomSchemeHandlerFactory factory : JBCefApp.getCefCustomSchemeHandlerFactories()) {
        cefApp.registerSchemeHandlerFactory(factory.getSchemeName(), factory.getDomainName(), factory);
      }
    } catch (RuntimeException | LinkageError e) {
      LOG.warn("Unable to register scheme handlers after restarting JCEF", e);
    }
  }

  private static boolean invoke(Object target, Method method) throws ReflectiveOperationException {
    method.setAccessible(true);
    return Boolean.TRUE.equals(method.invoke(target, false, false));
  }

}
