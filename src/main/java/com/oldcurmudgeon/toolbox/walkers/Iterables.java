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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author OldCurmudgeon
 */
public class Iterables {
  /**
   * Adapts an {@link Iterator} to an {@link Iterable} for use in enhanced for loops.
   *
   * If {@link Iterable#iterator()} is invoked more than once, an
   * {@link IllegalStateException} is thrown.
   */
  public static <T> java.lang.Iterable<T> in(final Iterator<T> i) {
    assert i != null;
    class SingleUseIterable implements java.lang.Iterable<T> {
      private boolean used = false;

      @Override
      public Iterator<T> iterator() {
        if (used) {
          throw new IllegalStateException("SingleUseIterable already invoked");
        }
        used = true;
        return i;
      }
    }
    return new SingleUseIterable();
  }

  public static <T> java.lang.Iterable<T> in(final Enumeration<T> e) {
    assert e != null;
    class SingleUseIterable implements java.lang.Iterable<T> {
      private boolean used = false;

      @Override
      public Iterator<T> iterator() {
        if (used) {
          throw new IllegalStateException("SingleUseIterable already invoked");
        }
        used = true;
        return new Iterator<T>() {
          @Override
          public boolean hasNext() {
            return e.hasMoreElements();
          }

          @Override
          public T next() {
            return e.nextElement();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("Not supported.");
          }
        };
      }
    }
    return new SingleUseIterable();
  }

  // Iterable across any set of objects calling their toString.
  public static java.lang.Iterable<String> in(final Set<? extends Object> s) {
    assert s != null;
    final Iterator<? extends Object> i = s.iterator();

    class SingleUseIterable implements java.lang.Iterable<String> {
      private boolean used = false;

      @Override
      public Iterator<String> iterator() {
        if (used) {
          throw new IllegalStateException("SingleUseIterable already invoked");
        }
        used = true;
        return new Iterator<String>() {
          @Override
          public boolean hasNext() {
            return i.hasNext();
          }

          @Override
          public String next() {
            return i.next().toString();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("Not supported.");
          }
        };
      }
    }
    return new SingleUseIterable();

  }

  // Unique iterable.
  public static <T> Iterable<T> unique(final Iterable<T> i) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          // The feed iterator.
          Iterator<T> it = i.iterator();
          // Next/
          T next = null;
          // Check for duplicates.
          Set<T> done = new HashSet<>();

          @Override
          public boolean hasNext() {
            while (next == null && it.hasNext()) {
              T n = it.next();
              if (done.contains(n)) {
                // Already done that one.
                n = null;
              } else {
                // Done it now.
                done.add(n);
              }
              next = n;
            }
            return next != null;
          }

          @Override
          public T next() {
            // Standard - gove it and null it.
            T n = next;
            next = null;
            return n;
          }

          @Override
          public void remove() {
            // Could have strange effects.
            it.remove();
          }
        };
      }
    };
  }

  // Empty iterable.
  public static <T> java.lang.Iterable<T> emptyIterable() {
    return in(Iterables.<T>emptyIterator());
  }

  // Empty iterator.
  public static <T> java.util.Iterator<T> emptyIterator() {
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public T next() {
        return null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Not supported.");
      }
    };
  }

  public static <T extends Comparable<T>> int compare(Iterator<T> i1, Iterator<T> i2) {
    int diff = 0;
    while (i1.hasNext() && diff == 0) {
      T it1 = i1.next();
      if (i2.hasNext()) {
        T it2 = i2.next();
        // Are we equal?
        diff = it1.compareTo(it2);
      } else {
        // i2 exhausted! i1 is greater!
        diff = 1;
      }
    }
    if (diff == 0) {
      // i1 exhausted! i2 is less!
      diff = -1;
    }
    // All the same!
    return 0;
  }
}
