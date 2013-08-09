/*
 * Copyright 2013 OldCurmudgeon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldcurmudgeon.toolbox.twiddlers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class RegexFilenameFilter implements FilenameFilter, DirectoryStream.Filter<Path> {
  /**
   * Only file name that match this regex are accepted by this filter
   */
  String regex = null; // setting the filter regex to null causes any name to be accepted (same as ".*")

  public RegexFilenameFilter() {
  }

  public RegexFilenameFilter(String filter) {
    setWildcard(filter);
  }

  /**
   * Set the filter from a wildcard expression as known from the windows command line
   * ("?" = "any character", "*" = zero or more occurances of any character")
   *
   * @param sWild the wildcard pattern
   *
   * @return this
   */
  public final RegexFilenameFilter setWildcard(String sWild) {
    regex = wildcardToRegex(sWild);

    // throw PatternSyntaxException if the pattern is not valid
    // this should never happen if wildcardToRegex works as intended,
    // so thiw method does not declare PatternSyntaxException to be thrown
    Pattern.compile(regex);
    return this;
  }

  /**
   * Set the regular expression of the filter
   *
   * @param regex the regular expression of the filter
   *
   * @return this
   */
  public RegexFilenameFilter setRegex(String regex) throws java.util.regex.PatternSyntaxException {
    this.regex = regex;
    // throw PatternSyntaxException if the pattern is not valid
    Pattern.compile(regex);

    return this;
  }

  /**
   * Tests if a specified file should be included in a file list.
   *
   * @param dir the directory in which the file was found.
   *
   * @param name the name of the file.
   *
   * @return true if and only if the name should be included in the file list; false otherwise.
   */
  @Override
  public boolean accept(File dir, String name) {
    return regex == null ? true : name.toLowerCase().matches(regex);
  }

  @Override
  public boolean accept(Path p) throws IOException {
    return regex == null ? true : p.toString().toLowerCase().matches(regex);
  }
  
  /**
   * Converts a windows wildcard pattern to a regex pattern
   *
   * @param wild - Wildcard patter containing * and ?
   *
   * @return - a regex pattern that is equivalent to the windows wildcard pattern
   */
  private static String wildcardToRegex(String wild) {
    if (wild == null) {
      return null;
    }
    StringBuilder buffer = new StringBuilder();

    char[] chars = wild.toLowerCase().toCharArray();

    for (int i = 0; i < chars.length; ++i) {
      if (chars[i] == '*') {
        buffer.append(".*");
      } else if (chars[i] == '?') {
        buffer.append('.');
      } else if (chars[i] == ';') {
        buffer.append('|');
      } else if ("+()^$.{}[]|\\".indexOf(chars[i]) != -1) {
        buffer.append('\\').append(chars[i]); // prefix all metacharacters with backslash
      } else {
        buffer.append(chars[i]);
      }
    }

    return buffer.toString();
  }

}
