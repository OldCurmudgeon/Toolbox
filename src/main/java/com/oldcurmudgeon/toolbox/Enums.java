/*
 * Copyright 2015 Paul Caswell.
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
package com.oldcurmudgeon.toolbox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * @author Paul Caswell
 */
public class Enums {

    // ALL enums have a name.
    public interface HasName {

        public String name();

    }

    public interface PoliteEnum extends HasName {

        default String politeName() {
            return name().replace("_", " ");
        }

    }

    public interface Lookup<P, Q> {

        public Q lookup(P p) throws Exception;

    }

    public interface ReverseLookup<E extends Enum<E>> extends Lookup<String, E> {

        // Map of all classes that have lookups.
        ConcurrentMap<Class, Map<String, Enum>> lookups = new ConcurrentHashMap<>();

        // What I need from the Enum.
        Class<E> getDeclaringClass();

        public static <E extends Enum<E>> E lookup(Class<E> c, String name) {
            // Get the map - creating it if necessary.
            final Map<String, Enum> lookup = lookups.computeIfAbsent(c, k -> new HashMap<>());
            if (lookup.isEmpty()) {
                // Populate it.
                for (E e : c.getEnumConstants()) {
                    lookup.put(e.name(), e);
                    // Also add the polite versions if they are there.
                    if (e instanceof PoliteEnum) {
                        String politeName = ((PoliteEnum) e).politeName();
                        lookup.put(politeName, e);
                        lookup.put(politeName.toLowerCase(), e);
                        lookup.put(politeName.toUpperCase(), e);

                    }
                }
            }
            // Look it up.
            return c.cast(lookup.get(name));
        }

        @Override
        default E lookup(String name) throws InterruptedException, ExecutionException {
            // Use the static one.
            return lookup(getDeclaringClass(), name);
        }

    }

    // Use the above interfaces to add to the enum.
    public enum X implements PoliteEnum, ReverseLookup<X> {

        A_For_Ism,
        B_For_Mutton,
        C_Forth_Highlanders
    }

    public void test() throws InterruptedException, ExecutionException {
        System.out.println("Hello");
        System.out.println(X.C_Forth_Highlanders.lookup("A For Ism"));
        System.out.println(ReverseLookup.lookup(X.class, "B For Mutton"));
    }

    public static void main(String args[]) {
        try {
            new Enums().test();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
