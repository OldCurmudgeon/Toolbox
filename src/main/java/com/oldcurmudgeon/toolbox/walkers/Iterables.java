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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author OldCurmudgeon
 */
public class Iterables {

  private Iterables() {
  }

  /**
   * Adapts an {@link Iterator} to an {@link Iterable} for use in enhanced for
   * loops.
   *
   * If {@link Iterable#iterator()} is invoked more than once, an
   * {@link IllegalStateException} is thrown.
   *
   * @param <T>
   * @param i
   * @return
   */
  public static <T> Iterable<T> in(final Iterator<T> i) {
    return new SingleUseIterable<>(i);
  }

  /**
   * Makes an Enumeration iterable.
   *
   * @param <T>
   * @param e
   * @return
   */
  public static <T> Iterable<T> in(final Enumeration<T> e) {
    return new SingleUseIterable<>(new EnumerationIterator<>(e));
  }

  /**
   * Makes an Iterable<String> out of an Iterable<Object> by calling toString on
   * each object.
   *
   * @param i
   * @return
   */
  public static Iterable<String> in(final Iterable<Object> i) {
    return in(new IA<Object, String>(i.iterator(), objectToString));
  }

  /**
   * Adapts Objects to Strings.
   */
  public static final Adapter<Object, String> objectToString = new Adapter<Object, String>() {

    public String adapt(Object p) {
      return p.toString();
    }

  };

  /**
   * Makes sure the iterator is never used again - even though it is wrapped in
   * an Iterable.
   *
   * @param <T>
   */
  public static class SingleUseIterable<T> implements Iterable<T> {

    protected boolean used = false;
    protected final Iterator<T> it;

    public SingleUseIterable(Iterator<T> it) {
      this.it = it;
    }

    public SingleUseIterable(Iterable<T> it) {
      this(it.iterator());
    }

    @Override
    public Iterator<T> iterator() {
      if (used) {
        throw new IllegalStateException("SingleUseIterable already invoked");
      }
      used = true;
      // Only let them have it once.
      return it;
    }

  }

  /**
   * A classic adaptor pattern.
   *
   * @param <P>
   * @param <Q>
   */
  public static interface Adapter<P, Q> {

    public Q adapt(P p);

  }

  /**
   * An I walks an iterator of one type but delivers items of a different type.
   *
   * Please fill in the `next()` method. Use an Adaptor for convenience.
   *
   * @param <S>
   * @param <T>
   */
  public abstract static class I<S, T> implements Iterator<T> {

    protected final Iterator<S> it;

    public I(Iterator<S> it) {
      this.it = it;
    }

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public void remove() {
      it.remove();
    }

  }

  /**
   * Use an adaptor to transform one type into another.
   *
   * @param <S>
   * @param <T>
   */
  public static class IA<S, T> extends I<S, T> {

    private final Adapter<S, T> adaptor;

    public IA(Iterator<S> it, Adapter<S, T> adaptor) {
      super(it);
      this.adaptor = adaptor;
    }

    @Override
    public T next() {
      return adaptor.adapt(it.next());
    }

  }

  /**
   * An Iterator over an Enumeration.
   *
   * @param <T>
   */
  public static class EnumerationIterator<T> implements Iterator<T> {

    private final Enumeration<T> it;

    public EnumerationIterator(Enumeration<T> it) {
      this.it = it;
    }

    @Override
    public boolean hasNext() {
      return it.hasMoreElements();
    }

    @Override
    public T next() {
      return it.nextElement();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

  }

  // Empty iterable - Deprecated! - Please use Collections.<T>emptyList();
  @Deprecated
  public static <T> Iterable<T> emptyIterable() {
    return Collections.<T>emptyList();
  }

  // Empty iterator - Deprecated! - Please use Collections.<T>emptyIterator();
  @Deprecated
  public static <T> Iterator<T> emptyIterator() {
    return Collections.<T>emptyList().iterator();
  }

  public abstract static class Walker<T> implements Iterator<T> {

    // The next item.
    private T next = get();

    // Get the next.
    abstract T get();

    @Override
    public final boolean hasNext() {
      if (next == null) {
        next = get();
      }
      return next != null;
    }

    @Override
    public final T next() {
      // Standard - give it and null it.
      T n = next;
      next = null;
      return n;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
  }

  // Unique iterable.
  public static <T> Iterable<T> unique(final Iterable<T> i) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new UniqueIterator<>(i.iterator());
      }

    };
  }

  private static class UniqueIterator<T> extends Walker<T> {

    // The feed iterator.
    private final Iterator<T> i;
    // Check for duplicates.
    private final Set<T> done = new HashSet<>();

    public UniqueIterator(Iterator<T> i) {
      this.i = i;
    }

    @Override
    T get() {
      T next = null;
      while (next == null && i.hasNext()) {
        T n = i.next();
        if (n != null) {
          // Already done that one?
          if (!done.contains(n)) {
            // Done it now.
            done.add(n);
            // Pick that one.
            next = n;
          }
        }

      }
      return next;
    }

    @Override
    public void remove() {
      // Could have strange effects.
      i.remove();
    }

  }

  /**
   * Compare two iterators for equality.
   *
   * @param <T>
   * @param i1
   * @param i2
   * @return
   */
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
    if (diff == 0 && i2.hasNext()) {
      // i1 exhausted! i2 is less!
      diff = -1;
    }
    // All the same!
    return 0;
  }

  /**
   * Makes a number of Iterables iterable.
   *
   * @param <T>
   * @param them
   * @return
   */
  public static <T> Iterable<T> in(final Iterable<T>... them) {
    return new Iterable<T>() {
      // Counter across them.
      private int i = 0;
      // The Iterator on the current one.
      private Iterator<T> it = nextIterator();

      public Iterator<T> iterator() {
        return new Walker<T>() {

          @Override
          T get() {
            T next = null;
            // It is exhausted?
            if (!it.hasNext()) {
              // Get the next iterator.
              it = nextIterator();
            }
            if (it != null && it.hasNext()) {
              // Pull next out.
              next = it.next();
            }
            return next;
          }

          public void remove() {
            it.remove();
          }

        };
      }

      private Iterator<T> nextIterator() {
        if (them == null || i >= them.length) {
          return null;
        }
        Iterator<T> n = null;
        do {
          Iterable<T> next = them[i++];
          if ( next != null ) {
            n = next.iterator();
          }
        } while (i < them.length && (n == null || !n.hasNext()));
        return n;
      }

    };
  }

  public static void main(String args[]) {
    try {
      List<String> a = Arrays.asList("One", "Two", "Three");
      List<String> b = Arrays.asList("Four", "Five", "Six");
      for (String s : in(a, null, Collections.<String>emptyList(), b)) {
        System.out.println(s);
      }
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}
