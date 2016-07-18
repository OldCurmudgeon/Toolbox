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
package com.oldcurmudgeon.toolbox.containers;

// A blocking queue I can close from the pull end. 

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

// Please only use put because offer does not shortcut on close.
public class CloseableBlockingQueue<E> extends ArrayBlockingQueue<E> {
    // Flag indicates closed state.
    private volatile boolean closed = false;
    // All blocked threads. Actually this is all threads that are in the process
    // of invoking a put but if put doesn't block then they disappear pretty fast.
    // NB: Container is O(1) for get and almost O(1) (depending on how busy it is) for put.
    private final Container<Thread> blocked;

    // Limited size.
    public CloseableBlockingQueue(int queueLength) {
        super(queueLength);
        // Collection of blocked threads - i.e. They've called `put` but it hasn't returned yet.
        blocked = new Container<>(queueLength);
    }

    /**
     * *
     * Shortcut to do nothing if closed.
     * <p>
     * Track blocked threads.
     */
    @Override
    public void put(E e) throws InterruptedException {
        // Do nothing if close in progress.
        if (!closed) {
            Thread t = Thread.currentThread();
            // Hold my node on the stack so removal can be trivial.
            Container.Node<Thread> n = blocked.add(t);
            try {
                super.put(e);
            } finally {
                // Not blocked anymore.
                blocked.remove(n, t);
            }
        }
    }

    /**
     * Shortcut to do nothing if closed.
     */
    @Override
    public E poll() {
        E it = null;
        // Do nothing when closed.
        if (!closed) {
            it = super.poll();
        }
        return it;
    }

    /**
     * Shortcut to do nothing if closed.
     */
    @Override
    public E poll(long l, TimeUnit tu) throws InterruptedException {
        E it = null;
        // Do nothing when closed.
        if (!closed) {
            it = super.poll(l, tu);
        }
        return it;
    }

    /**
     * isClosed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Close down everything.
     */
    public void close() {
        // Stop all new queue entries.
        closed = true;
        // Must unblock all blocked threads.

        // Walk all blocked threads and interrupt them.
        for (Thread t : blocked) {
            //log.log("! Interrupting " + t.toString());
            // Interrupt all of them.
            t.interrupt();
        }
    }

    @Override
    public String toString() {
        return blocked.toString();
    }
}
