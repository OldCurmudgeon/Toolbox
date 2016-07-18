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
package com.oldcurmudgeon.toolbox.containers;

import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author OldCurmudgeon
 */
public class JoinedArray<T> implements Iterable<T> {
  final List<T[]> joined;

  @SafeVarargs
  public JoinedArray(T[]... arrays) {
    joined = Arrays.<T[]>asList(arrays);
  }

  @Override
  public Iterator<T> iterator() {
    return new JoinedIterator<>(joined);
  }

  private class JoinedIterator<T> implements Iterator<T> {
    // The iterator across the arrays.
    Iterator<T[]> i;
    // The array I am working on.
    T[] a;
    // Where we are in it.
    int ai;
    // The next T to return.
    T next = null;

    private JoinedIterator(List<T[]> joined) {
      i = joined.iterator();
      a = i.hasNext() ? i.next() : null;
      ai = 0;
    }

    @Override
    public boolean hasNext() {
      while (next == null && a != null) {
        // a goes to null at the end of i.
        if (a != null) {
          // End of a?
          if (ai >= a.length) {
            // Yes! Next i.
            if (i.hasNext()) {
              a = i.next();
            } else {
              // Finished.
              a = null;
            }
            ai = 0;
          }
          if (a != null) {
            if (ai < a.length) {
              next = a[ai++];
            }
          }
        }
      }
      return next != null;
    }

    @Override
    public T next() {
      T n = null;
      if (hasNext()) {
        // Give it to them.
        n = next;
        next = null;
      } else {
        // Not there!!
        throw new NoSuchElementException();
      }
      return n;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

  }

  public int copyTo(T[] to, int offset, int length) {
    int copied = 0;
    // Walk each of my arrays.
    for (T[] a : joined) {
      // All done if nothing left to copy.
      if (length <= 0) {
        break;
      }
      if (offset < a.length) {
        // Copy up to the end or to the limit, whichever is the first.
        int n = Math.min(a.length - offset, length);
        System.arraycopy(a, offset, to, copied, n);
        offset = 0;
        copied += n;
        length -= n;
      } else {
        // Skip this array completely.
        offset -= a.length;
      }
    }
    return copied;
  }

  public int copyTo(T[] to, int offset) {
    return copyTo(to, offset, to.length);
  }

  public int copyTo(T[] to) {
    return copyTo(to, 0);
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    Separator comma = new Separator(",");
    for (T[] a : joined) {
      s.append(comma.sep()).append(Arrays.toString(a));
    }
    return s.toString();
  }

  public static void main(String[] args) {
    String[] zeroOne = new String[]{
      "Zero",
      "One"
    };
    String[] twoThreeFourFive = new String[]{
      "Two",
      "Three",
      "Four",
      "Five"
    };
    String[] sixSevenEightNine = new String[]{
      "Six",
      "Seven",
      "Eight",
      "Nine"
    };
    JoinedArray<String> a = new JoinedArray<>(
            new String[]{},
            new String[]{},
            zeroOne,
            new String[]{},
            twoThreeFourFive,
            sixSevenEightNine);
    for (String s : a) {
      System.out.println(s);
    }
    twoThreeFourFive[0] = "TWO";
    twoThreeFourFive[1] = null;
    for (String s : a) {
      System.out.println(s);
    }

    String[] four = new String[4];
    int copied = a.copyTo(four, 3, four.length);
    System.out.println("Copied (3," + four.length + ")=" + copied + " = " + Arrays.toString(four));

  }

}
