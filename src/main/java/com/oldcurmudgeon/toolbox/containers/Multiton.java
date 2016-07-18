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
package com.oldcurmudgeon.toolbox.containers;

import java.util.concurrent.*;

/**
 * A Multiton where the keys are an enum and each key can create its own value.
 * <p>
 * The create method of the key enum is guaranteed to only be called once.
 * <p>
 * Probably worth making your Multiton static to avoid duplication.
 *
 * @param <K> - The enum that is the key in the map and also does the creation.
 *            <p>
 *            No second parameter because it will probably not map to a single object.
 * @author OldCurmudgeon
 */
public class Multiton<K extends Enum<K> & Multiton.Creator> {
    // The map to the future.
    private final ConcurrentMap<K, Future<Object>> multitons = new ConcurrentHashMap<>();

    // The enums must create
    public interface Creator {
        public abstract Object create();

    }

    public <V> V get(final K key, Class<V> type) {
        // Has it run yet?
        Future<Object> f = multitons.get(key);
        if (f == null) {
            // No! Make the task that runs it.
            FutureTask<Object> ft = new FutureTask<>(key::create);
            // Only put if not there.
            f = multitons.putIfAbsent(key, ft);
            if (f == null) {
                // We replaced null so we successfully put. We were first!
                f = ft;
                // Initiate the task.
                ft.run();
            }
        }
        try {
            /**
             * If code gets here and hangs due to f.status = 0 (FutureTask.NEW)
             * then you are trying to get from your Multiton in your creator.
             *
             * Cannot check for that without unnecessarily complex code.
             *
             * Perhaps could use get with timeout.
             */
            // Cast here to force the right type.
            return (V) f.get();
        } catch (InterruptedException | ExecutionException ex) {
            // Hide exceptions without discarding them.
            throw new RuntimeException(ex);
        }
    }

    enum E implements Creator {
        A {
            @Override
            public String create() {
                return "Face";
            }

        },
        B {
            @Override
            public Integer create() {
                return 0xFace;
            }

        },
        C {
            @Override
            public Void create() {
                return null;
            }

        }
    }

    public static void main(String args[]) {
        try {
            Multiton<E> m = new Multiton<>();
            String face1 = m.get(E.A, String.class);
            Integer face2 = m.get(E.B, Integer.class);
            System.out.println("Face1: " + face1 + " Face2: " + Integer.toHexString(face2));
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
