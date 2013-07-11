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
 * Iterates across the item(s) once.
 *
 * @author OldCurmudgeon
 */
public class OnceIterator<T> implements Iterator<T> {
  final T[] ts;
  int i = 0;

  public OnceIterator(T... ts) {
    this.ts = ts;
  }

  @Override
  public boolean hasNext() {
    return i < ts.length;
  }

  @Override
  public T next() {
    return ts[i++];
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

}
