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

import com.oldcurmudgeon.toolbox.walkers.Iterables;
import java.util.Iterator;

/**
 * @author OldCurmudgeon
 * @param <P>
 * @param <Q>
 */
public class Pair<P extends Comparable<P>, Q extends Comparable<Q>> implements Comparable<Pair<P, Q>> {
  // Exposing p & q directly for simplicity. They are final so this is safe.
  public final P p;
  public final Q q;

  public Pair(P p, Q q) {
    this.p = p;
    this.q = q;
  }

  public P getP() {
    return p;
  }

  public Q getQ() {
    return q;
  }

  @Override
  public String toString() {
    return "<" + (p == null ? "" : p.toString()) + "," + (q == null ? "" : q.toString()) + ">";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair it = (Pair) o;
    return p == null ? it.p == null : p.equals(it.p) && q == null ? it.q == null : q.equals(it.q);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + (this.p != null ? this.p.hashCode() : 0);
    hash = 97 * hash + (this.q != null ? this.q.hashCode() : 0);
    return hash;
  }

  @Override
  public int compareTo(Pair<P, Q> o) {
    int diff = p == null ? (o.p == null ? 0 : -1) : p.compareTo(o.p);
    if (diff == 0) {
      diff = q == null ? (o.q == null ? 0 : -1) : q.compareTo(o.q);
    }
    return diff;
  }

  // Iterate across Pairs - returns items of type T.
  private abstract static class I<P extends Comparable<P>, Q extends Comparable<Q>, T> implements Iterator<T> {
    protected final Iterator<Pair<P, Q>> i;

    public I(Iterator<Pair<P, Q>> i) {
      this.i = i;
    }

    @Override
    public boolean hasNext() {
      return i.hasNext();
    }

    @Override
    public void remove() {
      i.remove();
    }

  }

  // Given an Iterable<Pair<P,Q>> - returns an Iterable<Q>.
  public static <P extends Comparable<P>, Q extends Comparable<Q>> Iterable<Q> iq(final Iterable<Pair<P, Q>> it) {
    return Iterables.in(new I<P, Q, Q>(it.iterator()) {
      @Override
      public Q next() {
        return i.next().getQ();
      }

    });
  }

  // Given an Iterable<Pair<P,Q>> - returns an Iterable<P>.
  public static <P extends Comparable<P>, Q extends Comparable<Q>> Iterable<P> ip(Iterable<Pair<P, Q>> it) {
    return Iterables.in(new I<P, Q, P>(it.iterator()) {
      @Override
      public P next() {
        return i.next().getP();
      }

    });
  }

}
