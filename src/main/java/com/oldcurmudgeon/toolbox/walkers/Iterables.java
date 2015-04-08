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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import nu.xom.Element;
import nu.xom.Elements;
import org.slf4j.LoggerFactory;

/**
 * Tools for iterating.
 *
 * @author OldCurmudgeon
 */
public final class Iterables {
  // Logger.
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Iterables.class);

  // Static.
  private Iterables() {
  }

  /**
   * Adapts an {@link Iterator} to an {@link Iterable} for use in enhanced for loops.
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

  public static <T> Iterable<T> in(final Enumeration<T> e) {
    return new SingleUseIterable<>(new EnumerationIterator<>(e));
  }

  public static Iterable<Element> in(final Elements e) {
    return new SingleUseIterable<>(new ElementsIterator(e));
  }

  public static <T> Iterable<T> in(final Iterable<T>... them) {
    return new Strider<>(them);
  }

  private static class Strider<T> implements Iterable<T> {
    private final Iterable<T>[] them;

    public Strider(final Iterable<T>... them) {
      this.them = them;
    }

    @Override
    public Iterator<T> iterator() {
      return new IterableIterator();
    }

    private class IterableIterator extends IN<T> {
      private int i = 0;
      private Iterator<T> it = null;

      @Override
      public T getNext() {
        while ((it == null || !it.hasNext()) && i < them.length) {
          it = them[i++].iterator();
        }
        if (it != null && it.hasNext()) {
          return it.next();
        }
        return null;
      }

      @Override
      public void remove() {
        // Forward it.
        it.remove();
      }

    }

  }

  public static <P, Q> Iterable<Q> adapt(final Iterable<P> i, final Adapter<P, Q> adapter) {
    return () -> new IA<>(i.iterator(), adapter);

  }

  class IterableIterable<T> implements Iterable<T> {

    private final Iterable<? extends Iterable<T>> i;

    public IterableIterable(Iterable<? extends Iterable<T>> i) {
      this.i = i;
    }

    @Override
    public Iterator<T> iterator() {
      return new IIT();
    }

    private class IIT implements Iterator<T> {

      // Pull an iterator.
      final Iterator<? extends Iterable<T>> iit = i.iterator();
      // The current Iterator<T>
      Iterator<T> it = null;
      // The current T.
      T next = null;

      @Override
      public boolean hasNext() {
        boolean finished = false;
        while (next == null && !finished) {
          if (it == null || !it.hasNext()) {
            if (iit.hasNext()) {
              it = iit.next().iterator();
            } else {
              // All over when we've exhausted the list of lists.
              finished = true;
            }
          }
          if (it != null && it.hasNext()) {
            // Get another from the current list.
            next = it.next();
          }
        }
        return next != null;
      }

      @Override
      public T next() {
        T n = next;
        next = null;
        return n;
      }

    }

  }

  /**
   * An Iterator over XOM Elements.
   *
   * @param <T>
   */
  private static class ElementsIterator extends IN<Element> {
    final Elements elements;
    int i = 0;
    Element next = null;

    public ElementsIterator(Elements elements) {
      this.elements = elements;
    }

    @Override
    public Element getNext() {
      if (i < elements.size()) {
        return elements.get(i++);
      }
      return null;
    }

  }

  /**
   * An Iterator over an Enumeration.
   *
   * @param <T>
   */
  public static class EnumerationIterator<T> extends IN<T> {

    private final Enumeration<T> it;

    public EnumerationIterator(Enumeration<T> it) {
      this.it = it;
    }

    @Override
    T getNext() {
      return it.hasMoreElements() ? it.nextElement() : null;
    }

  }

  public static Iterable<ResultSet> in(final ResultSet r) {
    return new SingleUseIterable<>(new ResultSetIterator(r));

  }

  public static class ResultSetIterator extends IN<ResultSet> {
    private final ResultSet r;

    public ResultSetIterator(ResultSet r) {
      this.r = r;
    }

    @Override
    ResultSet getNext() throws SQLException {
      return r.next() ? r : null;
    }

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
  public static final Adapter<Object, String> objectToString = (Object p) -> p.toString();

  /**
   * Iterator with a close.
   *
   * @param <T>
   */
  public static interface CloseableIterator<T> extends Iterator<T> {
    public void close();

  }

  public static interface CloseableIterable<T> extends Iterable<T> {
    @Override
    CloseableIterator<T> iterator();

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
   * An IN does next for you - thus formalising the Iterator contract.
   *
   * Just implement getNext.
   *
   * @param <T>
   */
  public abstract static class IN<T> implements Iterator<T> {

    private T next = null;

    abstract T getNext() throws Exception;

    @Override
    public final boolean hasNext() {
      if (next == null) {
        try {
          next = getNext();
        } catch (Exception ex) {
          throw new RuntimeException("Exception in Iterator", ex);
        }
      }
      return next != null;
    }

    @Override
    public final T next() {
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

// Empty iterable.
  public static <T> Iterable<T> emptyIterable() {
    return Collections.<T>emptyList();
  }

  // Empty iterator.
  public static <T> Iterator<T> emptyIterator() {
    return Collections.<T>emptyList().iterator();
  }

}
