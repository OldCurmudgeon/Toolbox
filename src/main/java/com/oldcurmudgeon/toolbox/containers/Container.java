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

import com.oldcurmudgeon.toolbox.io.TinyLogger;
import com.oldcurmudgeon.toolbox.walkers.Separator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Container
 * ---------
 * <p>
 * A lock-free container that offers a close-to O(1) add/remove performance.
 * <p>
 * Reasoning
 * ---------
 * <p>
 * In the FileList object I need to keep a record of all threads that are
 * currently in the process of adding an item to a blocking queue. This
 * record will be used by the blocking queue when it is closed to determine
 * which threads need to be interrupted.
 * <p>
 * The pattern will therefore be:
 * <p>
 * 1. An item will be added to the container and, if the queue.put call does
 * not block the item will be immediately removed.
 * <p>
 * 2. If the queue.put blocks, the item will remain in the container.
 * <p>
 * 3. As there is a limit to the number of threads there is also a limit to the
 * number of entities in the container so the container can also run with
 * a fixed capacity.
 * <p>
 * 4. At some time in the future, all items remaining in the container must be
 * iterated over - probably for interruption.
 * <p>
 * Implementation 4 - A ring
 * -------------------------
 * <p>
 * Move the ring head forward to just past the most recently allocated node at
 * every allocate.
 * <p>
 * Adding (with my tests) is now very close to O(1) too.
 *
 * @param <T> - The type of object in the container.
 * @author OldCurmudgeon
 */

public class Container<T> implements Iterable<T> {
    // The maximum capacity of the container.
    final int capacity;
    // The list.
    final AtomicReference<Node<T>> head = new AtomicReference<>();
    // Queue of threads blocked on put.
    private final Queue<Thread> waiting = new ConcurrentLinkedQueue();

    // Constructor
    public Container(int capacity) {
        // Sanity checks.
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }
        this.capacity = capacity;
        // Construct the list.
        Node<T> h = new Node<>();
        Node<T> it = h;
        // One created, now add (capacity - 1) more
        for (int i = 0; i < capacity - 1; i++) {
            // Add it.
            it.next = new Node<>();
            // Step on to it.
            it = it.next;
        }
        // Make it a ring.
        it.next = h;
        // Install it.
        head.set(h);
    }

    // Add a new one - throw an exception if no room.
    public Node<T> add(T element) {
        // Get a free node.
        Node<T> freeNode = getFree();
        if (freeNode != null) {
            // Attach the element.
            return freeNode.attach(element);
        } else {
            // Failed!
            throw new IllegalStateException("Capacity exhausted.");
        }
    }

    // Add a new one - return null if no room.
    public Node<T> offer(T element) {
        // Get a free node.
        Node<T> freeNode = getFree();
        if (freeNode != null) {
            // Attach the element.
            return freeNode.attach(element);
        } else {
            // Failed!
            return null;
        }
    }

    // Add a new one - block if no room.
    public Node<T> put(T element) throws InterruptedException {
        do {
            // Get a free node.
            Node<T> freeNode = getFree();
            if (freeNode != null) {
                // Attach the element.
                return freeNode.attach(element);
            } else {
                // Block.
                waitForFree();
            }
            // Forever.
        } while (true);
    }

    // Find the next free element and mark it not free.
    private Node<T> getFree() {
        Node<T> freeNode = head.get();
        int skipped = 0;
        // Stop when we hit the end of the list
        // ... or we successfully transit a node from free to not-free.
        while (skipped < capacity && !freeNode.free.compareAndSet(true, false)) {
            skipped += 1;
            freeNode = freeNode.next;
        }
        if (skipped < capacity) {
            // Put the head as next.
            // Doesn't matter if it fails. That would just mean someone else was doing the same.
            head.set(freeNode.next);
        } else {
            // We hit the end! No more free nodes.
            freeNode = null;
        }
        return freeNode;
    }

    // Mark it free.
    public void remove(Node<T> it, T element) {
        // Remove the element first.
        it.detach(element);
        // Mark it as free.
        if (!it.free.compareAndSet(false, true)) {
            throw new IllegalStateException("Freeing a freed node.");
        }
        // Signal all threads blocked on put.
        signalFree();
    }

    // Use this to block for a while.
    private final Blocked block = new Blocked();

    /**
     * Wait for a time when it is likely that a free slot is available.
     */
    private void waitForFree() throws InterruptedException {
        // Still full?
        while (isFull()) {
            // Park me 'till something is removed.
            block.await();
        }
    }

    /**
     * A slot has been freed up. If anyone is waiting, let the next one know.
     */
    private void signalFree() {
        block.release();
    }

    // Counts how many there are currently in the container.
    public int size() {
        int found = 0;
        Node<T> it = head.get();
        // Do a raw count.
        for (int i = 0; i < capacity; i++) {
            if (!it.free.get()) {
                found += 1;
            }
            it = it.next;
        }
        return found;
    }

    // Are we empty.
    public boolean isEmpty() {
        return size() == 0;
    }

    // Are we empty.
    public boolean isFull() {
        return size() == capacity;
    }

    /**
     * clear ... NOT thread safe.
     * <p>
     * There is little I can guarantee from this method.
     * <p>
     * It is strongly recommended that this method is only used
     * when it is known that no other thread will be accessing the
     * container.
     */
    public void clear() {
        Node<T> it = head.get();
        for (int i = 0; i < capacity; i++) {
            // Trash the element
            it.element = null;
            // Mark it free.
            it.free.set(true);
            it = it.next;
        }
    }

    // The Node class. It is static so needs the <T> repeated.
    public static class Node<T> {
        // The element in the node.
        private T element;
        // Are we free?
        private AtomicBoolean free = new AtomicBoolean(true);
        /*
         * The next reference in whatever list I am in.
         *
         * Does not need to be volatile as it is only set in the constructor of a Container
         * and so will only ever be accessed by one thread.
         *
         */
        private Node<T> next;

        // Construct a node of the list
        private Node() {
            // Start empty.
            element = null;
        }

        // Attach the element to a free node.
        public Node<T> attach(T element) {
            // Sanity check.
            if (this.element == null) {
                this.element = element;
            } else {
                throw new IllegalArgumentException("There is already an element attached.");
            }
            // Useful for chaining.
            return this;
        }

        // Detach the element from a node.
        public Node<T> detach(T element) {
            // Sanity check.
            if (this.element == element) {
                this.element = null;
            } else {
                throw new IllegalArgumentException("Removal of wrong element.");
            }
            // Useful for chaining.
            return this;
        }

        // Just to be helpful.
        public T get() {
            return element;
        }

        @Override
        public String toString() {
            return element != null ? element.toString() : "null";
        }

    }

    // Provides an iterator across all items in the container.
    @Override
    public Iterator<T> iterator() {
        return new UsedNodesIterator<>(this);
    }

    /*
     * Iterates across used nodes.
     *
     * Note that this will do the best it can to iterate across all entries in the container.
     *
     * The only contract this iterator can achieve is that it will return every entry that is
     * in the container from the start time to the finish time of the iteration. Any entries
     * that are added or removed during this time may or may not be returned.
     *
     * No statement is made on the order of the items returned.
     *
     * It will never return null.
     *
     * It will always complete.
     *
     */
    private static class UsedNodesIterator<T> implements Iterator<T> {
        // Where next to look for the next used node.
        Node<T> it;
        // The capacity at creation time. Counts down to zero as we walk.
        int stop = 0;
        // The next entry to return.
        T next = null;

        public UsedNodesIterator(Container<T> c) {
            // Remember that the head node can move so Snapshot the head node at this time.
            it = c.head.get();
            // Remember where to stop too.
            stop = c.capacity;
        }

        // Returns true of there is another element available.
        @Override
        public boolean hasNext() {
            // Made into a `while` loop to fix issue reported by @Nim
            // In a while loop because we may find an entry with 'null' in it and we don't want that.
            while (next == null && stop > 0) {
                // Scan to the next non-free node.
                while (stop > 0 && it.free.get() == true) {
                    it = it.next;
                    // Step down 1.
                    stop -= 1;
                }
                if (stop > 0) {
                    next = it.element;
                }
            }
            return next != null;
        }

        // Returns the next available element.
        @Override
        public T next() {
            T n = null;
            if (hasNext()) {
                // Give it to them.
                n = next;
                next = null;
                // Step forward.
                it = it.next;
                stop -= 1;
            } else {
                // Not there!!
                throw new NoSuchElementException();
            }
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Separator comma = new Separator(",");
        // Keep counts too.
        int usedCount = 0;
        int freeCount = 0;
        // I will iterate the list myself as I want to count free nodes too.
        Node<T> it = head.get();
        int count = 0;
        s.append("[");
        // Scan to the end.
        while (count < capacity) {
            // Is it in-use?
            if (it.free.get() == false) {
                // Grab its element.
                T e = it.element;
                // Is it null?
                if (e != null) {
                    // Good element.
                    s.append(comma.sep()).append(e.toString());
                    // Count them.
                    usedCount += 1;
                } else {
                    // Probably became free while I was traversing.
                    // Because the element is detached before the entry is marked free.
                    freeCount += 1;
                }
            } else {
                // Free one.
                freeCount += 1;
            }
            // Next
            it = it.next;
            count += 1;
        }
        // Decorate with counts "]used+free".
        s.append("]").append(usedCount).append("+").append(freeCount);
        if (usedCount + freeCount != capacity) {
            // Perhaps something was added/freed while we were iterating.
            s.append("?");
        }
        return s.toString();
    }

    // ***** Following only needed for testing. *****
    private static final TinyLogger log = new TinyLogger("Container");
    static volatile boolean testing = true;
    static AtomicInteger nTesters = new AtomicInteger();

    // Tester object to exercise the container.
    static class Tester<T> implements Runnable {
        // My name.
        T me;
        // The container I am testing.
        Container<T> c;

        public Tester(Container<T> container, T name) {
            nTesters.incrementAndGet();
            me = name;
            c = container;
        }

        private void pause() {
            try {
                Thread.sleep(0);
            } catch (InterruptedException ex) {
                testing = false;
            }
        }

        @Override
        public void run() {
            // Spin on add/remove until stopped.
            while (testing) {
                // Add it.
                Node<T> n = c.offer(me);
                if (n != null) {
                    log.log("Added " + me + ": " + c.toString());
                    pause();
                    // Remove it.
                    c.remove(n, me);
                    log.log("Removed " + me + ": " + c.toString());
                    pause();
                } else {
                    // Just go around again.
                    pause();
                }
            }
            nTesters.decrementAndGet();
        }

    }

    static final String[] strings = {
            "One", "Two", "Three", "Four", "Five",
            "Six", "Seven", "Eight", "Nine", "Ten"
    };
    static final int TEST_THREADS = Math.min(10, strings.length);

    public static void main(String[] args) throws InterruptedException {
        log.setDebug(true);
        Container<String> c = new Container<>(10);

        // Simple add/remove
        log.log(true, "Simple test");
        Node<String> it = c.add(strings[0]);
        log.log("Added " + c.toString());
        c.remove(it, strings[0]);
        log.log("Removed " + c.toString());

        // Capacity test.
        log.log(true, "Capacity test");
        ArrayList<Node<String>> nodes = new ArrayList<>(strings.length);
        // Fill it.
        for (int i = 0; i < strings.length; i++) {
            nodes.add(i, c.add(strings[i]));
            log.log("Added " + strings[i] + " " + c.toString());
        }
        // Add one more.
        try {
            c.add("Wafer thin mint!");
        } catch (IllegalStateException ise) {
            log.log("Full!");
        }
        c.clear();
        log.log("Empty: " + c.toString());

        // Iterate test.
        log.log(true, "Iterator test");
        for (int i = 0; i < strings.length; i++) {
            nodes.add(i, c.add(strings[i]));
        }
        StringBuilder all = new StringBuilder();
        Separator sep = new Separator(",");
        for (String s : c) {
            all.append(sep.sep()).append(s);
        }
        log.log("All: " + all);
        for (int i = 0; i < strings.length; i++) {
            c.remove(nodes.get(i), strings[i]);
        }
        sep.reset();
        all.setLength(0);
        for (String s : c) {
            all.append(sep.sep()).append(s);
        }
        log.log("None: " + all.toString());

        // Multiple add/remove
        log.log(true, "Multi test");
        for (int i = 0; i < strings.length; i++) {
            nodes.add(i, c.add(strings[i]));
            log.log("Added " + strings[i] + " " + c.toString());
        }
        log.log("Filled " + c.toString());
        for (int i = 0; i < strings.length - 1; i++) {
            c.remove(nodes.get(i), strings[i]);
            log.log("Removed " + strings[i] + " " + c.toString());
        }
        c.remove(nodes.get(strings.length - 1), strings[strings.length - 1]);
        log.log("Empty " + c.toString());

        // Multi-threaded add/remove
        log.log(true, "Threads test");
        c.clear();
        for (int i = 0; i < TEST_THREADS; i++) {
            Thread t = new Thread(new Tester<>(c, strings[i]));
            t.setName("Tester " + strings[i]);
            log.log("Starting " + t.getName());
            t.start();
        }
        // Wait for the tests to complete.
        completeTests(c, 10);

    /*
     // Heavyweight tests.
     testing = true;
     final int n = 100;
     Container<Integer> d = new Container<>(n);
     for (int i = 0; i < 10 * n; i++) {
     Thread t = new Thread(new Tester<>(d, i));
     t.setName("Tester " + i);
     log.log("Starting " + t.getName());
     t.start();
     }
     completeTests(d, 30);
     */
    }

    static void completeTests(Container c, int seconds) throws InterruptedException {
        // Wait for 10 seconds.
        log.log(true, "Waiting " + seconds + " seconds with " + nTesters.intValue() + " testers.");
        long stop = System.currentTimeMillis() + (seconds * 1000);
        while (System.currentTimeMillis() < stop) {
            Thread.sleep(100);
        }
        // Stop the testers.
        testing = false;
        // Wait some more.
        while (nTesters.intValue() > 0) {
            Thread.sleep(100);
        }

    }

}
