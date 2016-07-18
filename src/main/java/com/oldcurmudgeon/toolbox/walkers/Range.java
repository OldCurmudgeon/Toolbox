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
 * A range of integers.
 * <p>
 * ToDo: Allow for stopping *before* stop instead of *on* stop.
 *
 * @author OldCurmudgeon
 */
public class Range implements Iterable<Integer> {
    private final int start;
    private final int step;
    private final int stop;

    public Range(int start, int stop, int step) {
        // Probably a good idea to do some parameter checking.
        if (start < stop && step <= 0
                || start > stop && step >= 0
                || start != stop && step == 0) {
            throw new IllegalArgumentException("Invalid range selected (" + start + "," + step + "," + stop + ")");
        }

        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public Range(int start, int stop) {
        this(start, stop, start < stop ? 1 : -1);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Ranger(start);
    }

    private class Ranger implements Iterator<Integer> {
        int last;
        Integer next;

        Ranger(int start) {
            last = start;
            next = start;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                int n = last + step;
                if (step > 0 && n <= stop || step < 0 && n >= stop) {
                    next = n;
                }
            }
            return next != null;
        }

        @Override
        public Integer next() {
            last = hasNext() ? next : null;
            next = null;
            return last;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

    }

    @Override
    public String toString() {
        return Separator.separate("(", ",", ")", start, stop, step);
    }

    private static void test(Range r) {
        System.out.println(r + "=" + Separator.separate("[", ",", "]", r));
    }

    private static void test() {
        test(new Range(1, 10));
        test(new Range(1, -10));
        test(new Range(1, -10, -2));
    }

    public static void main(String args[]) {
        try {
            test();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
