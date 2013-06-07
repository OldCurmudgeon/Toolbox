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
package com.oldcurmudgeon.toolbox.walkers;

import java.util.Iterator;

/**
 * Will allow iteration over a String in any direction with any step.
 *
 * Iterate characters through the CharacterIterator
 *
 * @author OldCurmudgeon
 */
public final class StringWalker implements Iterable<Character> {
  // The iteree
  private final String s;
  // Where to get the first character from.
  private final int start;
  // What to add to i (usually +/- 1).
  private final int step;
  // What should i be when we stop.
  private final int stop;

  // Start there by step, stop there.
  public StringWalker(String s, int start, int step, int stop) {
    // Must terminate.
    if ((step > 0 && start > stop)
            || (step < 0 && start < stop)
            || (step == 0 && start != stop)) {
      throw new IllegalArgumentException("Walk will never finish.");
    }
    // The string. Should we clone it?
    this.s = s;
    // Start there.
    this.start = start;
    // Step that far.
    this.step = step;
    // Stop there.
    this.stop = stop;
  }

  // From there step that. Direction construed from sign of step
  public StringWalker(String s, int start, int step) {
    this(s, start,
            step,
            step > 0 ? s.length() : (step < 0 ? -1 : start));
  }

  // From there, forward or backward.
  public StringWalker(String s, int start, boolean forward) {
    // Forward or backward.
    this(s, start,
            forward ? 1 : -1,
            forward ? s.length() : -1);
  }

  // Forward from start or backwards from end.
  public StringWalker(String s, boolean forward) {
    // Forward or backward.
    this(s, forward ? 0 : s.length() - 1,
            forward);
  }

  // Forward from there.
  public StringWalker(String s, int start) {
    // Forward or backward.
    this(s, start, true);
  }

  // Forward from start to end.
  public StringWalker(String s) {
    this(s, true);
  }

  // Make it iterable.
  @Override
  public Iterator<Character> iterator() {
    // Make a new one.
    return new CharacterIterator();
  }

  public class Strings implements Iterable<String> {
    final String sep;

    public Strings(String sep) {
      this.sep = sep;
    }
    // And a String iterable.

    @Override
    public Iterator<String> iterator() {
      // Make a new one.
      return new StringIterator(sep);
    }
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    Iterator i = new CharacterIterator();
    while (i.hasNext()) {
      string.append(i.next());
    }
    return string.toString();
  }

  // The Character iterator.
  private final class CharacterIterator implements Iterator<Character> {
    // Where I am.
    private int i;
    // The next character.
    private Character next = null;

    public CharacterIterator() {
      // Start at the start.
      i = start;
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        if (step > 0 ? i < stop : i > stop) {
          next = s.charAt(i);
          i += step;
        }
      }
      return next != null;
    }

    @Override
    public Character next() {
      Character n = next;
      next = null;
      return n;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported.");
    }
  }

  // The iterator itself.
  private final class StringIterator implements Iterator<String> {
    // Where I am.
    private int i;
    // The next character.
    private String next = null;
    // The separator.
    private final String separator;

    public StringIterator(String separator) {
      // Start at the start.
      i = start;
      // Remember my separator.
      this.separator = separator;
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        if (step > 0 ? i < stop : i > stop) {
          // Remember where we start.
          int f = i;
          // Search for the next separator in the indicated direction.
          if (step > 0 && i >= 0) {
            // Find the next forward.
            i = s.indexOf(separator, i);
            if (i >= 0) {
              // That's the string.
              next = s.substring(f, Math.min(i, stop));
              // Next time around, start after that.
              i += separator.length();
            } else {
              // Give them the rest of the string.
              if ( f >= 0 && f < stop ) {
                next = s.substring(f, stop);
                i = stop;
              }
            }
          } else {
            // Backwards.
            i = s.lastIndexOf(separator, i);
            if (i >= 0) {
              // That's the string.
              next = s.substring(Math.max(i + separator.length(), stop), f+1);
              // Next time around, start before that.
              i -= separator.length();
            } else {
              // Give them the rest of the string.
              if ( f > stop ) {
                next = s.substring(stop+1, f+1);
                i = stop;
              }
            }
          }
        }
      }
      return next != null;
    }

    @Override
    public String next() {
      String n = next;
      next = null;
      return n;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported.");
    }
  }

  // TEST ONLY
  private static String walk(Iterable<Character> i) {
    StringBuilder s = new StringBuilder();
    for (Character c : i) {
      s.append(c);
    }
    return s.toString();
  }

  static StringBuffer testBuffer = new StringBuffer();
  private static String swalk(Iterable<String> i) {
    testBuffer.setLength(0);
    Separator sep = new Separator("|");
    for (String s : i) {
      testBuffer.append(sep.sep()).append(s);
    }
    return testBuffer.toString();
  }

  public static void main(String[] args) {
    String s = "Now is the time for all good men to come to the aid of the party.";
    try {
      System.out.println("== Characters ==");
      System.out.println("Forward  : " + walk(new StringWalker(s)));
      System.out.println("Backward : " + walk(new StringWalker(s, false)));
      System.out.println("Part     : " + walk(new StringWalker(s, 7)));
      System.out.println("Part back: " + walk(new StringWalker(s, 2, -1)));
      System.out.println("Part back: " + walk(new StringWalker(s, 2, false)));
      System.out.println("Step 2   : " + walk(new StringWalker(s, 7, 2)));
      System.out.println("Step -2  : " + walk(new StringWalker(s, s.length() - 1, -2)));
      System.out.println("Empty    : " + walk(new StringWalker(null, 0, 0, 0)));
      System.out.println("== Strings ==");
      System.out.println("Forward  : " + swalk(new StringWalker(s).new Strings(" ")));
      System.out.println("Backward : " + swalk(new StringWalker(s, false).new Strings(" ")));
      System.out.println("Part     : " + swalk(new StringWalker(s, 7).new Strings(" ")));
      System.out.println("Part back: " + swalk(new StringWalker(s, 2, -1).new Strings(" ")));
      System.out.println("Part back: " + swalk(new StringWalker(s, 2, false).new Strings(" ")));
      System.out.println("Step 2   : " + swalk(new StringWalker(s, 7, 2).new Strings(" ")));
      System.out.println("Step -2  : " + swalk(new StringWalker(s, s.length() - 1, -2).new Strings(" ")));
      System.out.println("Empty    : " + swalk(new StringWalker(null, 0, 0, 0).new Strings(" ")));
    } catch (Exception e) {
      System.err.println("Buffer: "+testBuffer);
      e.printStackTrace();
    }
  }
}
