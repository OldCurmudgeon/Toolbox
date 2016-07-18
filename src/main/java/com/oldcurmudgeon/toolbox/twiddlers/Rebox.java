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
package com.oldcurmudgeon.toolbox.twiddlers;

import java.util.Arrays;

/**
 * Can rebox a boxed primitive array into its Object form.
 * <p>
 * Generally I HATE using instanceof because using it is usually
 * an indication that your hierarchy is completely wrong.
 * <p>
 * Reboxing - however - is an area I am ok using it.
 * <p>
 * Generally, if a primitive array is passed to a varargs it
 * is wrapped up as the first and only component of an Object[].
 * <p>
 * E.g.
 * <p>
 * public void f(T... t) {};
 * f(new int[]{1,2});
 * <p>
 * actually ends up calling f with t an Object[1] and t[0] the int[].
 * <p>
 * This unwraps it and returns the correct reboxed version.
 * <p>
 * In the above example it will return an Integer[].
 * <p>
 * Any other array types will be returned unchanged.
 *
 * @author OldCurmudgeon
 */
public class Rebox {
    public static <T> T[] rebox(T[] it) {
        // Default to return it unchanged.
        T[] result = it;
        // Special case length 1 and it[0] is primitive array.
        if (it.length == 1 && it[0].getClass().isArray()) {
            // Which primitive array is it?
            if (it[0] instanceof int[]) {
                result = rebox((int[]) it[0]);
            } else if (it[0] instanceof long[]) {
                result = rebox((long[]) it[0]);
            } else if (it[0] instanceof float[]) {
                result = rebox((float[]) it[0]);
            } else if (it[0] instanceof double[]) {
                result = rebox((double[]) it[0]);
            } else if (it[0] instanceof char[]) {
                result = rebox((char[]) it[0]);
            } else if (it[0] instanceof byte[]) {
                result = rebox((byte[]) it[0]);
            } else if (it[0] instanceof short[]) {
                result = rebox((short[]) it[0]);
            } else if (it[0] instanceof boolean[]) {
                result = rebox((boolean[]) it[0]);
            }
        }
        return result;
    }

    // Rebox each one separately.
    private static <T> T[] rebox(int[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Integer.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(long[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Long.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(float[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Float.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(double[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Double.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(char[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Character.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(byte[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Byte.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(short[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Short.valueOf(it[i]);
        }
        return boxed;
    }

    private static <T> T[] rebox(boolean[] it) {
        T[] boxed = makeTArray(it.length);
        for (int i = 0; i < it.length; i++) {
            boxed[i] = (T) Boolean.valueOf(it[i]);
        }
        return boxed;
    }

    // Trick to make a T[] of any length.
    // Do not pass any parameter for `dummy`.
    // public because this is potentially re-useable.
    public static <T> T[] makeTArray(int length, T... dummy) {
        return Arrays.copyOf(dummy, length);
    }
}
