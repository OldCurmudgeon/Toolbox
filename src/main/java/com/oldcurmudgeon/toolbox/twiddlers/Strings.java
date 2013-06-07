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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public final class Strings {
  private Strings() {
  }

  /**
   * pad
   *
   * @param string String
   * @param pad String
   * @return String
   */
  public static String pad(String string, String pad) {
    return pad(string, pad, false);
  }

  public static String pad(String string, String pad, char keepAtStart) {
    // Assume truncate if keep specified because probvably generating fixed fields.
    return pad(string, pad, true, keepAtStart);
  }

  public static String pad(String string, String pad, boolean okToTruncate, char keepAtStart) {
    if (string.length() > 0 && string.charAt(0) == keepAtStart) {
      // Keep the character at the start (usually a '-')
      return keepAtStart + pad(string.substring(1), pad.substring(1), okToTruncate);
    } else {
      return pad(string, pad, okToTruncate);
    }
  }

  public static String pad(String string, String pad, boolean okToTruncate) {
    // E.g Pad("sss","0000"); should deliver "0sss".
    if (string == null) {
      string = "";
    }
    // Grab the two lengths.
    final int stringLen = string.length();
    final int padLen = pad.length();
    // This looks strange but its coded this way to make it obvious.
    // I add the pad to the left of string then take as many characters from the right that is the same length as the pad.
    // The 'max' is to ensure we never truncate the original field.
    int finalLen = (okToTruncate ? padLen : Math.max(padLen, stringLen));
    return (pad + string).substring(padLen + stringLen - finalLen);
  }

  public static String padRight(String string, String pad) {
    return padRight(string, pad, false);
  }

  public static String padRight(String string, String pad, boolean okToTruncate) {
    // E.g Pad(store,"0000"); should deliver "sss0".
    if (string == null) {
      string = "";
    }
    // Grab the two lengths.
    final int stringLen = string.length();
    final int padLen = pad.length();
    // This looks strange but its coded this way to make it obvious.
    // I add the pad to the left of string then take as many characters from the right that is the same length as the pad.
    // The 'max' is to ensure we never truncate the original field.
    int finalLen = (okToTruncate ? padLen : Math.max(padLen, stringLen));
    return (string + pad).substring(0, finalLen);
  }
  private static TreeMap<Integer, String> spaces = new TreeMap<>();

  public static String spaces(int n) {
    Integer i = Integer.valueOf(n);
    String s = spaces.get(n);
    if (s == null) {
      s = makeFilledString(n, ' ');
      spaces.put(i, s);
    }
    return s;
  }
  private static TreeMap<Integer, String> zeros = new TreeMap<>();

  public static String zeros(int n) {
    Integer i = Integer.valueOf(n);
    String s = zeros.get(i);
    if (s == null) {
      s = makeFilledString(n, '0');
      zeros.put(i, s);
    }
    return s;
  }

  public static String makeFilledString(int n, char c) {
    char[] ca = new char[n];
    Arrays.fill(ca, c);
    return new String(ca);
  }

  public static String makeFilledString(int n, String s) {
    StringBuilder string = new StringBuilder(n * s.length());
    for ( int i = 0; i < n; i++ ) {
      string = string.append(s);
    }
    return string.toString();
  }

  /**
   * @deprecated See Pattern.quote
   */
  public static String notRegex(String s) {
    //final String regexSpecialChars = "{^$.|?*+()\\";
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
      sb.append(notRegexC(s.charAt(i)));
    }

    return sb.toString();
  }

  /**
   * @deprecated see Pattern.quote
   */
  public static String notRegexC(final char ch) {
    StringBuilder sb = new StringBuilder();

    // Special?
    if ("{^$.|?*+()\\".indexOf(ch) >= 0) {
      // Yes! Escape it.
      sb.append('\\');
    }
    sb.append(ch);
    return sb.toString();
  }

  public static String mask(final String str) {
    String s = str;
    if (null != s) {
      s = s.trim();
      if (null != s) {
        final char[] a = new char[s.length()];
        Arrays.fill(a, '*');
        s = new String(a);
      }
    }
    return s;
  }
  public static final Set<Character> DIGITS = new HashSet<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
  public static final Set<Character> UPPERCASE = new HashSet<>(Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'));
  public static final Set<Character> LOWERCASE = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'));
  public static final Set<Character> ALPHA = new HashSet<Character>() {
    {
      addAll(UPPERCASE);
      addAll(LOWERCASE);
    }
  };
  public static final Set<Character> ALPHANUMERIC = new HashSet<Character>() {
    {
      addAll(ALPHA);
      addAll(DIGITS);
    }
  };
  public static final Set<Character> INTEGER = new HashSet<Character>() {
    {
      addAll(DIGITS);
    }
  };
  public static final Set<Character> DECIMAL = new HashSet<Character>() {
    {
      addAll(DIGITS);
      add('.');
    }
  };
  public static final Set<Character> SIGNED_INTEGER = new HashSet<Character>() {
    {
      addAll(INTEGER);
      add('+');
      add('-');
    }
  };
  public static final Set<Character> SIGNED_DECIMAL = new HashSet<Character>() {
    {
      addAll(SIGNED_INTEGER);
      add('.');
    }
  };

  public static boolean allIn(String s, Set<Character> in) {
    for (int i = 0; i < s.length(); i++) {
      if (!in.contains(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static StringBuilder propToString(final StringBuilder sb,
                                           final String name,
                                           final Object value,
                                           final boolean showNull) {
    if (value == null) {
      if (showNull) {
        sb.append(name).append('=').append(' ');
      }
    } else {
      sb.append(name).append('=');
      if (value instanceof String) {
        sb.append('\"').append(value).append('\"');
      } else {
        sb.append(value);
      }
      sb.append(' ');
    }
    return sb;
  }

  public static int nDigits(long n) {
    // Guessing 4 digit numbers will be more probable.
    // They are set in the first branch.
    if (n < 10000L) { // from 1 to 4
      if (n < 100L) { // 1 or 2
        if (n < 10L) {
          return 1;
        } else {
          return 2;
        }
      } else { // 3 or 4
        if (n < 1000L) {
          return 3;
        } else {
          return 4;
        }
      }
    } else { // from 5 a 20 (albeit longs can't have more than 18 or 19)
      if (n < 1000000000000L) { // from 5 to 12
        if (n < 100000000L) { // from 5 to 8
          if (n < 1000000L) { // 5 or 6
            if (n < 100000L) {
              return 5;
            } else {
              return 6;
            }
          } else { // 7 u 8
            if (n < 10000000L) {
              return 7;
            } else {
              return 8;
            }
          }
        } else { // from 9 to 12
          if (n < 10000000000L) { // 9 or 10
            if (n < 1000000000L) {
              return 9;
            } else {
              return 10;
            }
          } else { // 11 or 12
            if (n < 100000000000L) {
              return 11;
            } else {
              return 12;
            }
          }
        }
      } else { // from 13 to ... (18 or 20)
        if (n < 10000000000000000L) { // from 13 to 16
          if (n < 100000000000000L) { // 13 or 14
            if (n < 10000000000000L) {
              return 13;
            } else {
              return 14;
            }
          } else { // 15 or 16
            if (n < 1000000000000000L) {
              return 15;
            } else {
              return 16;
            }
          }
        } else { // from 17 to ...¿20?
          if (n < 1000000000000000000L) { // 17 or 18
            if (n < 100000000000000000L) {
              return 17;
            } else {
              return 18;
            }
          } else { // 19? Can it be?
            // 10000000000000000000L is'nt a valid long.
            return 19;
          }
        }
      }
    }
  }
}
