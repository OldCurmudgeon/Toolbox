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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author OldCurmudgeon
 */
public class Pair<P, Q> {

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
    return "{" + p + "," + q + "}";
  }

  // Given an Iterable<Pair<P,Q>> - returns an Iterable<Q>.
  public static <P, Q> Iterable<Q> iq(final Iterable<Pair<P, Q>> it) {
    final Iterator<Pair<P, Q>> i = it.iterator();
    
    return Iterables.in(new Iterator<Q> () {
      
      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public Q next() {
        return i.next().getQ();
      }

      @Override
      public void remove() {
        i.remove();
      }
    });
  }

  // Given an Iterable<Pair<P,Q>> - returns an Iterable<P>.
  public static <P, Q> Iterable<P> ip(Iterable<Pair<P, Q>> it) {
    final Iterator<Pair<P, Q>> i = it.iterator();
    
    return Iterables.in(new Iterator<P> () {
      
      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public P next() {
        return i.next().getP();
      }

      @Override
      public void remove() {
        i.remove();
      }
    });
  }
}
