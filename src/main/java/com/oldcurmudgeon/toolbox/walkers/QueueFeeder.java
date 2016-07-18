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
import java.util.concurrent.BlockingQueue;

/**
 * Feeds a Queue from an Iterator.
 *
 * @author OldCurmudgeon
 */
public class QueueFeeder<T> implements Runnable {
    private final BlockingQueue<T> queue;
    private final Iterator<T> iterator;

    public QueueFeeder(BlockingQueue<T> queue, Iterator<T> iterator) {
        this.queue = queue;
        this.iterator = iterator;
    }

    @Override
    public void run() {
        try {
            // While the iterator has a next, feed the queue.
            while (iterator.hasNext()) {
                // Just finish if we wre interrupted.
                queue.put(iterator.next());
            }
        } catch (Exception e) {
            System.out.println("Failed!!");
            e.printStackTrace(System.out);
        }
    }
}
