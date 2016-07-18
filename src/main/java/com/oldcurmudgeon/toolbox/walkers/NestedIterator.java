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

import java.util.*;

/**
 * @author OldCurmudgeon
 */
public class NestedIterator<T> implements Iterator<T> {
    // Outer iterator. Goes null when exhausted.
    Iterator<Iterator<T>> i2 = null;
    // Current Inner iterator. Goes null when exhausted - at which point we pull another one from i2.
    Iterator<T> i1 = null;
    // Next value.
    T next = null;

    // Takes a depth-2 iterator.
    public NestedIterator(Iterator<Iterator<T>> i2) {
        this.i2 = i2;
    }

    @Override
    public boolean hasNext() {
        // Is there one waiting?
        if (next == null) {
            // No! Look for one.
            // Has i1 got one?
            if (i1 == null || !i1.hasNext()) {
                // i1 is exhausted! Get a new one from i2.
                if (i2 != null && i2.hasNext()) {
                    /// Get next.
                    i1 = i2.next();
                }
            }
            // Get a new next from i1.
            if (i1 != null && i1.hasNext()) {
                // get next.
                next = i1.next();
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

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    // Utility builders for nested iterators.
    // Iterating across Maps of Maps of Maps.
    public static <K1, K2, K3, V> Iterator<Iterator<Iterator<V>>> iiiV(Map<K1, Map<K2, Map<K3, V>>> i) {
        final Iterator<Map<K2, Map<K3, V>>> iV = iV(i);
        return new Iterator<Iterator<Iterator<V>>>() {
            @Override
            public boolean hasNext() {
                return iV.hasNext();
            }

            @Override
            public Iterator<Iterator<V>> next() {
                return iiV(iV.next());
            }

            @Override
            public void remove() {
                iV.remove();
            }
        };
    }

    // Iterating across Maps of Maps.
    public static <K1, K2, V> Iterator<Iterator<V>> iiV(Map<K1, Map<K2, V>> i) {
        final Iterator<Map<K2, V>> iV = iV(i);
        return new Iterator<Iterator<V>>() {
            @Override
            public boolean hasNext() {
                return iV.hasNext();
            }

            @Override
            public Iterator<V> next() {
                return iV(iV.next());
            }

            @Override
            public void remove() {
                iV.remove();
            }
        };
    }

    // Iterating across Map values.
    public static <K, V> Iterator<V> iV(final Map<K, V> map) {
        // Make an iterator over the entrySet.
        return iV(map.entrySet().iterator());
    }

    // Iterating across Map.Entry Iterators.
    public static <K, V> Iterator<V> iV(final Iterator<Map.Entry<K, V>> i) {
        return new Iterator<V>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public V next() {
                return i.next().getValue();
            }

            @Override
            public void remove() {
                i.remove();
            }
        };
    }

    // **** TESTING ****
    public static void main(String[] args) {
        // Two way test.
        testTwoWay();
        System.out.flush();
        System.err.flush();
        // Three way test.
        testThreeWay();
        System.out.flush();
        System.err.flush();
        // MapMap test
        testMapMap();
        System.out.flush();
        System.err.flush();
        // MapMapMap test
        testMapMapMap();
        System.out.flush();
        System.err.flush();
    }

    private static void testMapMap() {
        Map<String, String> m = new TreeMap<>();
        m.put("M-1", "V-1");
        m.put("M-2", "V-2");
        Map<String, Map<String, String>> mm = new TreeMap<>();
        mm.put("MM-1", m);
        mm.put("MM-2", m);
        System.out.println("MapMap - 2 * V-1,V-2");
        Iterator<Iterator<String>> iiV = iiV(mm);
        for (Iterator<String> i = new NestedIterator<>(iiV); i.hasNext(); ) {
            System.out.print(i.next() + ",");
        }
        System.out.println();
    }

    private static void testMapMapMap() {
        Map<String, String> m = new TreeMap<>();
        m.put("M-1", "V-1");
        m.put("M-2", "V-2");
        m.put("M-3", "V-3");
        Map<String, Map<String, String>> mm = new TreeMap<>();
        mm.put("MM-1", m);
        mm.put("MM-2", m);
        Map<String, Map<String, Map<String, String>>> mmm = new TreeMap<>();
        mmm.put("MMM-1", mm);
        mmm.put("MMM-2", mm);
        System.out.println("MapMapMap - 2 * 2 = 4 * V-1,V-2,V-3");
        Iterator<Iterator<Iterator<String>>> iiiV = iiiV(mmm);
        for (Iterator<String> i = new NestedIterator<>(new NestedIterator<>(iiiV)); i.hasNext(); ) {
            System.out.print(i.next() + ",");
        }
        System.out.println();
    }

    enum I {
        I1, I2, I3
    }

    private static void testThreeWay() {
        // Three way test.
        System.out.println("Three way - 3 * 4 = 12 * I1,I2,I3");
        List<Iterator<I>> lii1 = Arrays.asList(
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator());
        List<Iterator<I>> lii2 = Arrays.asList(
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator());
        List<Iterator<I>> lii3 = Arrays.asList(
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator());
        Iterator<Iterator<Iterator<I>>> liii = Arrays.asList(
                lii1.iterator(),
                lii2.iterator(),
                lii3.iterator()).iterator();
        // Grow a 3-nest.
        // Unroll it.
        for (Iterator<I> ii = new NestedIterator<>(new NestedIterator<>(liii)); ii.hasNext(); ) {
            I it = ii.next();
            System.out.print(it + ",");
        }
        System.out.println();
    }

    private static void testTwoWay() {
        System.out.println("Two way - 3 * I1,I2,I3");
        List<Iterator<I>> lii = Arrays.asList(
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator(),
                EnumSet.allOf(I.class).iterator());
        for (Iterator<I> ii = new NestedIterator<>(lii.iterator()); ii.hasNext(); ) {
            I it = ii.next();
            System.out.print(it + ",");
        }
        System.out.println();
    }
}