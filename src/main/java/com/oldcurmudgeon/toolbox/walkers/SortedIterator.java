/*
 * Copyright 2013 OldCurmudgeon
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generalised iterator that can take an Iterator and deliver a sorted Iterator.
 * <p>
 * S - The type of the items the Iterator iterates over.
 *
 * @author OldCurmudgeon
 */
public class SortedIterator<S extends Comparable<S>> implements Iterator<S> {
    // My iterator across a TreeSet so sorted.
    private final Iterator<S> i;

    // Iterator version.
    public SortedIterator(Iterator<S> iter, Comparator<S> compare) {
        // Roll the whole lot into a TreeSet to sort it.
        Set<S> sorted = new TreeSet<>(compare);
        while (iter.hasNext()) {
            sorted.add(iter.next());
        }
        // Use the TreeSet iterator.
        i = sorted.iterator();
    }

    // Provide a default simple comparator.
    public SortedIterator(Iterator<S> iter) {
        this(iter, new Comparator<S>() {

            @Override
            public int compare(S p1, S p2) {
                return p1.compareTo(p2);
            }
        });
    }

    // Also available from an Iterable.
    public SortedIterator(Iterable<S> iter, Comparator<S> compare) {
        this(iter.iterator(), compare);
    }

    // Also available from an Iterable.
    public SortedIterator(Iterable<S> iter) {
        this(iter.iterator());
    }

    // Proxy.
    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    // Proxy.
    @Override
    public S next() {
        return i.next();
    }

    // Proxy.
    @Override
    public void remove() {
        i.remove();
    }
}
