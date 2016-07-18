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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Allows a producer to append to the list while the consumer can pull the 
 * whole current list for processing.
 * 
 * @author OldCurmudgeon
 */
public class DoubleBufferedList<T> {
  // Atomic reference so I can atomically swap it through.
  // Mark = true means I am adding to it so momentarily unavailable for iteration.
  private AtomicMarkableReference<List<T>> list = new AtomicMarkableReference<>(newList(), false);

  // Factory method to create a new list - may be best to abstract this.
  protected List<T> newList() {
    return new ArrayList<>();
  }

  // Get and replace with empty the current list - can return null - does not mean failed.
  public List<T> get() {
    // Atomically grab and replace the list with an empty one.
    List<T> empty = newList();
    List<T> it;
    // Replace an unmarked list with an empty one.
    if (!list.compareAndSet(it = list.getReference(), empty, false, false)) {
      // Failed to replace! 
      // It is probably marked as being appended to but may have been replaced by another thread.
      // Return empty and come back again soon.
      return Collections.<T>emptyList();
    }
    // Successfull replaced an unmarked list with an empty list!
    return it;
  }

  // Grab and lock the list in preparation for append.
  private List<T> grab() {
    List<T> it;
    // We cannot fail so spin on get and mark.
    while (!list.compareAndSet(it = list.getReference(), it, false, true)) {
      // Spin on mark - waiting for another grabber to release (which it must).
    }
    return it;
  }

  // Release the list.
  private void release(List<T> it) {
    // Unmark it - should this be a compareAndSet(it, it, true, false)?
    if (!list.attemptMark(it, false)) {
      // Should never fail because once marked it will not be replaced.
      throw new IllegalMonitorStateException("It changed while we were adding to it!");
    }
  }

  // Add an entry to the list.
  public void add(T entry) {
    List<T> it = grab();
    try {
      // Successfully marked! Add my new entry.
      it.add(entry);
    } finally {
      // Always release after a grab.
      release(it);
    }
  }

  // Add many entries to the list.
  public void add(List<T> entries) {
    List<T> it = grab();
    try {
      // Successfully marked! Add my new entries.
      it.addAll(entries);
    } finally {
      // Always release after a grab.
      release(it);
    }
  }

  // Add a number of entries.
  @SafeVarargs
  public final void add(T... entries) {
    // Make a list of them.
    add(Arrays.<T>asList(entries));
  }

  public static void main(String args[]) {
    Tester.test();
  }
}

class Tester {
  // TESTING.
  // How many testers to run.
  static final int N = 10;
  // The next one we're waiting for.
  static final AtomicInteger[] seen = new AtomicInteger[N];
  // The ones that arrived out of order.
  static final ConcurrentSkipListSet<Widget>[] queued = Generics.<ConcurrentSkipListSet<Widget>>newArray(N);

  static class Generics {
    // A new Generics method for when we switch to Java 7.
    @SafeVarargs
    static <E> E[] newArray(int length, E... array) {
      return Arrays.copyOf(array, length);
    }
  }

  static {
    // Populate the arrays.
    for (int i = 0; i < N; i++) {
      seen[i] = new AtomicInteger();
      queued[i] = new ConcurrentSkipListSet<>();
    }
  }

  // Thing that is produced and consumed.
  private static class Widget implements Comparable<Widget> {
    // Who produced it.
    public final int producer;
    // Its sequence number.
    public final int sequence;

    public Widget(int producer, int sequence) {
      this.producer = producer;
      this.sequence = sequence;
    }

    @Override
    public String toString() {
      return producer + "\t" + sequence;
    }

    @Override
    public int compareTo(Widget o) {
      // Sort on producer
      int diff = Integer.compare(producer, o.producer);
      if (diff == 0) {
        // And then sequence
        diff = Integer.compare(sequence, o.sequence);
      }
      return diff;
    }
  }

  // Produces Widgets and feeds them to the supplied DoubleBufferedList.
  private static class TestProducer implements Runnable {
    // The list to feed.
    final DoubleBufferedList<Widget> list;
    // My ID
    final int id;
    // The sequence we're at
    int sequence = 0;
    // Set this at true to stop me.
    public volatile boolean stop = false;

    public TestProducer(DoubleBufferedList<Widget> list, int id) {
      this.list = list;
      this.id = id;
    }

    @Override
    public void run() {
      // Just pump the list.
      while (!stop) {
        list.add(new Widget(id, sequence++));
      }
    }
  }

  // Consumes Widgets from the suplied DoubleBufferedList
  private static class TestConsumer implements Runnable {
    // The list to bleed.
    final DoubleBufferedList<Widget> list;
    // My ID
    final int id;
    // Set this at true to stop me.
    public volatile boolean stop = false;

    public TestConsumer(DoubleBufferedList<Widget> list, int id) {
      this.list = list;
      this.id = id;
    }

    @Override
    public void run() {
      // The list I am working on.
      List<Widget> l = list.get();
      // Stop when stop == true && list is empty
      while (!(stop && l.isEmpty())) {
        // Record all items in list as arrived.
        arrived(l);
        // Grab another list.
        l = list.get();
      }
    }

    private void arrived(List<Widget> l) {
      for (Widget w : l) {
        // Mark each one as arrived.
        arrived(w);
      }
    }

    // A Widget has arrived.
    private static void arrived(Widget w) {
      // Which one is it?
      AtomicInteger n = seen[w.producer];
      // Don't allow multi-access to the same producer data or we'll end up confused.
      synchronized (n) {
        // Is it the next to be seen?
        if (n.compareAndSet(w.sequence, w.sequence + 1)) {
          // It was the one we were waiting for! See if any of the ones in the queue can now be consumed.
          for (Iterator<Widget> i = queued[w.producer].iterator(); i.hasNext();) {
            Widget it = i.next();
            // Is it in sequence?
            if (n.compareAndSet(it.sequence, it.sequence + 1)) {
              // Done with that one too now!
              i.remove();
            } else {
              // Found a gap! Stop now.
              break;
            }
          }
        } else {
          // Out of sequence - Queue it.
          queued[w.producer].add(w);
        }
      }
    }
  }

  // Main tester
  public static void test() {
    try {
      System.out.println("DoubleBufferedList:Test");
      // Create my test buffer.
      DoubleBufferedList<Widget> list = new DoubleBufferedList<>();
      // All running threads - Producers then Consumers.
      List<Thread> running = new LinkedList<>();
      // Start some producer tests.
      List<TestProducer> producers = new ArrayList<>();
      for (int i = 0; i < N; i++) {
        TestProducer producer = new TestProducer(list, i);
        Thread t = new Thread(producer);
        t.setName("Producer " + i);
        t.start();
        producers.add(producer);
        running.add(t);
      }

      // Start the same number of consumers (could do less or more if we wanted to).
      List<TestConsumer> consumers = new ArrayList<>();
      for (int i = 0; i < N; i++) {
        TestConsumer consumer = new TestConsumer(list, i);
        Thread t = new Thread(consumer);
        t.setName("Consumer " + i);
        t.start();
        consumers.add(consumer);
        running.add(t);
      }
      // Wait for a while.
      Thread.sleep(5000);
      // Close down all.
      for (TestProducer p : producers) {
        p.stop = true;
      }
      for (TestConsumer c : consumers) {
        c.stop = true;
      }
      // Wait for all to stop.
      for (Thread t : running) {
        System.out.println("Joining " + t.getName());
        t.join();
      }
      // What results did we get?
      int totalMessages = 0;
      for (int i = 0; i < N; i++) {
        // How far did the producer get?
        int gotTo = producers.get(i).sequence;
        // The consumer's state
        int seenTo = seen[i].get();
        totalMessages += seenTo;
        Set<Widget> queue = queued[i];
        if (seenTo == gotTo && queue.isEmpty()) {
          System.out.println("Producer " + i + " ok.");
        } else {
          // Different set consumed as produced!
          System.out.println("Producer " + i + " Failed: gotTo=" + gotTo + " seenTo=" + seenTo + " queued=" + queue);
        }
      }
      System.out.println("Total messages " + totalMessages);

    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }
}