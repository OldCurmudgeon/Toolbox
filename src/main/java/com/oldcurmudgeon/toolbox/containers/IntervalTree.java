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

import com.oldcurmudgeon.toolbox.twiddlers.ProcessTimer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author OldCurmudgeon
 * @param <T>
 */
public class IntervalTree<T extends IntervalTree.Interval> {
  // My intervals.
  private final List<T> intervals;
  // My center value. All my intervals contain this center.
  private final long center;
  // My interval range.
  private final long lBound;
  private final long uBound;
  // My left tree. All intervals that end below my center.
  private final IntervalTree<T> left;
  // My right tree. All intervals that start above my center.
  private final IntervalTree<T> right;

  public IntervalTree(List<T> intervals) {
    if (intervals == null) {
      throw new NullPointerException();
    }

    // Initially, my root contains all intervals.
    this.intervals = intervals;

    // Find my center.
    center = findCenter();

    /*
     * Builds lefts out of all intervals that end below my center.
     * Builds rights out of all intervals that start above my center.
     * What remains contains all the intervals that contain my center.
     */

    // Lefts contains all intervals that end below my center point.
    final List<T> lefts = new ArrayList<>();
    // Rights contains all intervals that start above my center point.
    final List<T> rights = new ArrayList<>();

    // Track my bounds while distributing.
    long uB = Long.MIN_VALUE;
    long lB = Long.MAX_VALUE;
    for (T i : intervals) {
      long start = i.getStart();
      long end = i.getEnd();
      if (end < center) {
        // It ends below me - move it to my left.
        lefts.add(i);
      } else if (start > center) {
        // It starts above me - move it to my right.
        rights.add(i);
      } else {
        // One of mine.
        lB = Math.min(lB, start);
        uB = Math.max(uB, end);
      }
    }

    // Remove all those not mine.
    intervals.removeAll(lefts);
    intervals.removeAll(rights);
    // Record my bounds.
    uBound = uB;
    lBound = lB;

    // Build the subtrees.
    left = lefts.size() > 0 ? new IntervalTree<>(lefts) : null;
    right = rights.size() > 0 ? new IntervalTree<>(rights) : null;

    // Build my ascending and descending arrays.
    /**
     * @todo Build my ascending and descending arrays.
     */
  }

  /*
   * Returns a list of all intervals containing the point.
   */
  List<T> query(long point) {
    // Check my range.
    if (point >= lBound) {
      if (point <= uBound) {
        // In my range but remember, there may also be contributors from left or right.
        List<T> found = new ArrayList<>();
        // Gather all intersecting ones.
        // Could be made faster (perhaps) by holding two sorted lists by start and end.
        for (T i : intervals) {
          if (i.getStart() <= point && point <= i.getEnd()) {
            found.add(i);
          }
        }

        // Gather others.
        if (point < center && left != null) {
          found.addAll(left.query(point));
        }
        if (point > center && right != null) {
          found.addAll(right.query(point));
        }

        return found;
      } else {
        // To right.
        return right != null ? right.query(point) : Collections.<T>emptyList();
      }
    } else {
      // To left.
      return left != null ? left.query(point) : Collections.<T>emptyList();
    }

  }

  private long findCenter() {
    //return average();
    return median();
  }

  protected long median() {
    // Choose the median of all centers. Could choose just ends etc or anything.
    long[] points = new long[intervals.size()];
    int x = 0;
    for (T i : intervals) {
      // Take the mid point.
      points[x++] = (i.getStart() + i.getEnd()) / 2;
    }
    Arrays.sort(points);
    return points[points.length / 2];
  }

  /*
   * What an interval looks like.
   */
  public interface Interval {
    public long getStart();

    public long getEnd();
  }

  /*
   * A simple implemementation of an interval.
   */
  public static class SimpleInterval implements Interval {
    private final long start;
    private final long end;

    public SimpleInterval(long start, long end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public long getStart() {
      return start;
    }

    @Override
    public long getEnd() {
      return end;
    }

    @Override
    public String toString() {
      return "{" + start + "," + end + "}";
    }
  }
  
  public static void main(String[] args) {
    // Make some test data.
    final int testEntries = 1 * 100;
    ArrayList<SimpleInterval> intervals = new ArrayList<>();
    Random random = new Random();
    for ( int i = 0; i < testEntries; i++ ) {
      // Make a random interval.
      long start = random.nextLong();
      intervals.add(new SimpleInterval(start, start + 1000));
    }
    ProcessTimer timer = new ProcessTimer();
    IntervalTree<SimpleInterval> tree = new IntervalTree<>(intervals);
    System.out.println("Took "+timer);
  }
}