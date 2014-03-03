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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Holds a thread-safe map of unique create-once items.
 *
 * Contract:
 *
 * Only one object will be made for each key presented.
 *
 * Thread safe.
 *
 * @author OldCurmudgeon
 * @param <K>
 * @param <V>
 */
public class Multiton<K, V> {

    // Map from the key to the futures of the items.
    private final ConcurrentMap<K, Future<V>> multitons = new ConcurrentHashMap<>();
    // The creator can create an item of type V.
    private final Creator<K, V> creator;

    public Multiton(Creator<K, V> creator) {
        this.creator = creator;
    }

    /**
     * There can be only one.
     *
     * Use a FutureTask to do the creation to ensure only one construction.
     *
     * @param key
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public V get(final K key) throws InterruptedException, ExecutionException {
        // Already made?
        Future<V> f = multitons.get(key);
        if (f == null) {
            // Plan the future but do not create as yet.
            FutureTask<V> ft = new FutureTask<>(() -> creator.create(key));
            // Store it.
            f = multitons.putIfAbsent(key, ft);
            if (f == null) {
                // It was successfully stored - it is the first (and only)
                f = ft;
                // Make it happen.
                ft.run();
            }
        }
        // Wait for it to finish construction and return the constructed.
        return f.get();
    }

    /**
     * User provides one of these to do the construction.
     *
     * @param <K>
     * @param <V>
     */
    public abstract static class Creator<K, V> {

        // Return a new item under the key.
        abstract V create(K key) throws ExecutionException;

    }

}
