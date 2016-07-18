/*
 * Copyright 2015 Paul Caswell.
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
package com.oldcurmudgeon.toolbox;

/**
 * Offers some optimal methods for comparables.
 *
 * @author Paul Caswell
 */
public class Comparables {

  public static <T extends Comparable> T median(T a, T b, T c) {
    switch (Integer.signum(a.compareTo(b))) {
      case -1:
        // a < b
        switch (Integer.signum(b.compareTo(c))) {
          case -1:
            // b < c
            return b;

          case 0:
            // b = c - pick one.
            return b;

          case 1:
            // b > c
            return max(a, c);

          default:
            // Should never happen.
            return null;

        }

      case 0:
        // First two the same - median must be one of them - just pick one.
        return b;

      case 1:
        // a > b
        switch (Integer.signum(a.compareTo(c))) {
          case -1:
            // a < c
            return a;

          case 0:
            // a = c - pick one.
            return a;

          case 1:
            // a > c
            return max(b, c);

          default:
            // Should never happen.
            return null;

        }

      default:
        // Should never happen.
        return null;

    }
  }

  public static <T extends Comparable> T min(T a, T b, T c) {
    switch (Integer.signum(a.compareTo(b))) {
      case -1:
        // a < b
        return min(a, c);

      case 0:
        // a = b.
        return min(a, c);

      case 1:
        // a > b
        return min(b, c);

      default:
        return null;

    }
  }

  public static <T extends Comparable> T max(T a, T b, T c) {
    switch (Integer.signum(a.compareTo(b))) {
      case -1:
        // a < b
        return max(b, c);

      case 0:
        // a = b.
        return max(a, c);

      case 1:
        // a > b
        return max(a, c);

      default:
        return null;

    }
  }

  public static <T extends Comparable> T min(T a, T b) {
    switch (Integer.signum(a.compareTo(b))) {
      case -1:
        // a < b
        return a;

      case 0:
        // Same - just pick one.
        return b;

      case 1:
        // a > b
        return b;

      default:
        return null;

    }
  }

  public static <T extends Comparable> T max(T a, T b) {
    switch (Integer.signum(a.compareTo(b))) {
      case -1:
        // a < b
        return b;

      case 0:
        // Same - just pick one.
        return b;

      case 1:
        // a > b
        return a;

      default:
        return null;

    }
  }

  private static void test(int a, int b, int c, String results) {
    boolean ok = false;
    int m = median(a, b, c);
    for (int i = 0; i < results.length(); i++) {
      switch (results.charAt(i)) {
        case 'A':
          if (m == a) {
            ok = true;
          }
          break;
        case 'B':
          if (m == b) {
            ok = true;
          }
          break;
        case 'C':
          if (m == c) {
            ok = true;
          }
          break;
      }
    }
    if (!ok) {
      System.out.println("Failed median(" + a + "," + b + "," + c + ") = " + m);
    }
  }

  public static void main(String args[]) {
    try {
      test(0, 0, 0, "ABC");
      test(0, 0, 1, "AB");
      test(0, 1, 0, "AC");
      test(1, 0, 0, "BC");
      test(1, 1, 0, "AB");
      test(1, 0, 1, "AC");
      test(0, 1, 1, "BC");
      test(0, 1, 2, "B");
      test(0, 2, 1, "C");
      test(1, 0, 2, "A");
      test(1, 2, 0, "A");
      test(2, 1, 0, "B");
      test(2, 0, 1, "C");
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}
