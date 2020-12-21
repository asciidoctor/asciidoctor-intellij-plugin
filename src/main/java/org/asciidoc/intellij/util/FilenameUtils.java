package org.asciidoc.intellij.util;

import java.util.Collection;

public class FilenameUtils {

  /**
   * The extension separator character.
   * @since 1.4
   */
  public static final char EXTENSION_SEPARATOR = '.';

  /**
   * The Unix separator character.
   */
  private static final char UNIX_SEPARATOR = '/';

  /**
   * The Windows separator character.
   */
  private static final char WINDOWS_SEPARATOR = '\\';

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FilenameUtils() {
    super();
  }

  /**
   * Returns the index of the last directory separator character.
   * <p>
   * This method will handle a file in either Unix or Windows format.
   * The position of the last forward or backslash is returned.
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename  the filename to find the last path separator in, null returns -1
   * @return the index of the last separator character, or -1 if there
   * is no such character
   */
  public static int indexOfLastSeparator(String filename) {
    if (filename == null) {
      return -1;
    }
    int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }

  /**
   * Returns the index of the last extension separator character, which is a dot.
   * <p>
   * This method also checks that there is no directory separator after the last dot.
   * To do this it uses {@link #indexOfLastSeparator(String)} which will
   * handle a file in either Unix or Windows format.
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename  the filename to find the last path separator in, null returns -1
   * @return the index of the last separator character, or -1 if there
   * is no such character
   */
  public static int indexOfExtension(String filename) {
    if (filename == null) {
      return -1;
    }
    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    int lastSeparator = indexOfLastSeparator(filename);
    return lastSeparator > extensionPos ? -1 : extensionPos;
  }


  /**
   * Gets the name minus the path from a full filename.
   * <p>
   * This method will handle a file in either Unix or Windows format.
   * The text after the last forward or backslash is returned.
   * <pre>{@code
   * a/b/c.txt --> c.txt
   * a.txt     --> a.txt
   * a/b/c     --> c
   * a/b/c/    --> ""
   * }</pre>
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename  the filename to query, null returns null
   * @return the name of the file without the path, or an empty string if none exists
   */
  public static String getName(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfLastSeparator(filename);
    return filename.substring(index + 1);
  }

  /**
   * Gets the base name, minus the full path and extension, from a full filename.
   * <p>
   * This method will handle a file in either Unix or Windows format.
   * The text after the last forward or backslash and before the last dot is returned.
   * <pre>{@code
   * a/b/c.txt --> c
   * a.txt     --> a
   * a/b/c     --> c
   * a/b/c/    --> ""
   * }</pre>
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename  the filename to query, null returns null
   * @return the name of the file without the path, or an empty string if none exists
   */
  public static String getBaseName(String filename) {
    return removeExtension(getName(filename));
  }

  /**
   * Gets the extension of a filename.
   * <p>
   * This method returns the textual part of the filename after the last dot.
   * There must be no directory separator after the dot.
   * <pre>{@code
   * foo.txt      --> "txt"
   * a/b/c.jpg    --> "jpg"
   * a/b.txt/c    --> ""
   * a/b/c        --> ""
   * }</pre>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename the filename to retrieve the extension of.
   * @return the extension of the file or an empty string if none exists or {@code null}
   * if the filename is {@code null}.
   */
  public static String getExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return "";
    } else {
      return filename.substring(index + 1);
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Removes the extension from a filename.
   * <p>
   * This method returns the textual part of the filename before the last dot.
   * There must be no directory separator after the dot.
   * <pre>{@code
   * foo.txt    --> foo
   * a\b\c.jpg  --> a\b\c
   * a\b\c      --> a\b\c
   * a.b\c      --> a.b\c
   * }</pre>
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @param filename  the filename to query, null returns null
   * @return the filename minus the extension
   */
  public static String removeExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return filename;
    } else {
      return filename.substring(0, index);
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Checks whether the extension of the filename is that specified.
   * <p>
   * This method obtains the extension as the textual part of the filename
   * after the last dot. There must be no directory separator after the dot.
   * The extension check is case-sensitive on all platforms.
   *
   * @param filename  the filename to query, null returns false
   * @param extension  the extension to check for, null or empty checks for no extension
   * @return true if the filename has the specified extension
   */
  public static boolean isExtension(String filename, String extension) {
    if (filename == null) {
      return false;
    }
    if (extension == null || extension.length() == 0) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    return fileExt.equals(extension);
  }

  /**
   * Checks whether the extension of the filename is one of those specified.
   * <p>
   * This method obtains the extension as the textual part of the filename
   * after the last dot. There must be no directory separator after the dot.
   * The extension check is case-sensitive on all platforms.
   *
   * @param filename  the filename to query, null returns false
   * @param extensions  the extensions to check for, null checks for no extension
   * @return true if the filename is one of the extensions
   */
  public static boolean isExtension(String filename, String[] extensions) {
    if (filename == null) {
      return false;
    }
    if (extensions == null || extensions.length == 0) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    for (String extension : extensions) {
      if (fileExt.equals(extension)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether the extension of the filename is one of those specified.
   * <p>
   * This method obtains the extension as the textual part of the filename
   * after the last dot. There must be no directory separator after the dot.
   * The extension check is case-sensitive on all platforms.
   *
   * @param filename  the filename to query, null returns false
   * @param extensions  the extensions to check for, null checks for no extension
   * @return true if the filename is one of the extensions
   */
  public static boolean isExtension(String filename, Collection<String> extensions) {
    if (filename == null) {
      return false;
    }
    if (extensions == null || extensions.isEmpty()) {
      return indexOfExtension(filename) == -1;
    }
    String fileExt = getExtension(filename);
    for (String extension : extensions) {
      if (fileExt.equals(extension)) {
        return true;
      }
    }
    return false;
  }

}
