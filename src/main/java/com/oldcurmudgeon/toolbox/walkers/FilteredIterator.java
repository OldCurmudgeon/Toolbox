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

import java.math.BigInteger;
import java.util.Iterator;

/**
 * @author OldCurmudgeon
 */
public class FilteredIterator<T> implements Iterator<T> {
    // The source.
    final Iterator<T> source;
    // The filter.
    final Filter<T> filter;
    // The next.
    T next = null;

    public FilteredIterator(Iterator<T> source, Filter<T> filter) {
        this.source = source;
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        // Read and discard unacceptable entries.
        while (next == null && source.hasNext()) {
            T it = source.next();
            if (filter.accept(it)) {
                next = filter.filter(it);
            }
        }
        return next != null;
    }

    @Override
    public T next() {
        // Standard `next` mechanism.
        T last = hasNext() ? next : null;
        next = null;
        return last;
    }

    @Override
    public void remove() {
        source.remove();
    }

    public static void main(String[] args) {
        // Normal iterator - counts BigIntegers from 0.
        Iterator<BigInteger> i = new Iterator<BigInteger>() {
            BigInteger i = BigInteger.ZERO;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public BigInteger next() {
                return i = i.add(BigInteger.ONE);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }

        };
        // Filter to remove odd values.
        Filter<BigInteger> f = new Filter<BigInteger>() {
            @Override
            public boolean accept(BigInteger it) {
                // Accept only odd numbers.
                return it.testBit(0);
            }

        };
        FilteredIterator<BigInteger> fi = new FilteredIterator<>(i, f);
        for (BigInteger bi : Iterables.in(fi)) {
            System.out.println(bi);
            if (bi.compareTo(BigInteger.TEN) > 0) {
                break;
            }
        }

    }

}
