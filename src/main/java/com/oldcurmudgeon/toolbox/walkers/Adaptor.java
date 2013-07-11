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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Adapts one type to a similar type.
 *
 * Extend this and implement 'as' to convert F to T and
 * it will allow you to iterate over collections of type F
 * as if they were type T.
 *
 * @author OldCurmudgeon
 */
// 
public abstract class Adaptor<F, T> implements Iterable<T> {
  // The iterable.
  final Iterable<F> list;

  public Adaptor(F[] fs) {
    list = Arrays.asList(fs);
  }

  public Adaptor(List<F> l) {
    list = l;
  }

  public Adaptor(Set<F> s) {
    list = Iterables.in(s.iterator());
  }

  // Make a T out of an F - You write this.
  public abstract T as(F f);

  @Override
  public Iterator<T> iterator() {
    return new AdaptingIterator(list.iterator());
  }

  // Iterate across, converting on the fly.
  private class AdaptingIterator implements Iterator<T> {
    // The iterator I am adapting.
    final Iterator<F> adapted;

    public AdaptingIterator(Iterator<F> adapt) {
      adapted = adapt;
    }

    @Override
    public boolean hasNext() {
      // Forward hasNext to the underlying.
      return adapted.hasNext();
    }

    @Override
    public T next() {
      // Convert it on the fly.
      return as(adapted.next());
    }

    @Override
    public void remove() {
      // Forward remove to the underlying.
      adapted.remove();
    }

  }
}
