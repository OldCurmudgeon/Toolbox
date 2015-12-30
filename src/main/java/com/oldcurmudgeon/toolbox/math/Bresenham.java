/*
 * Copyright 2015 pcaswell.
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
package com.oldcurmudgeon.toolbox.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Using Bresenham maths for lines and circles.
 *
 * @author oldcurmudgeon
 */
public class Bresenham {

  public static class Point {

    final int x;
    final int y;
    public static final Point O = new Point(0, 0);

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public Point add(Point step) {
      return new Point(this.x + step.x, this.y + step.y);
    }

    public Point sub(Point step) {
      return add(step.neg());
    }

    public Point neg() {
      return new Point(-this.x, -this.y);
    }

    public Point signum() {
      return new Point(
              this.x < 0 ? -1 : this.x == 0 ? 0 : 1,
              this.y < 0 ? -1 : this.y == 0 ? 0 : 1);
    }

    public Point abs() {
      return new Point(x < 0 ? -x : x, y < 0 ? -y : y);
    }

    @Override
    public String toString() {
      return "{" + x + "," + y + '}';
    }

    private Point mul(int x, int y) {
      return new Point(this.x * x, this.y * y);
    }

    private Point flip() {
      return new Point(y, x);
    }

    private Point add(int x, int y) {
      return new Point(this.x + x, this.y + y);
    }

  }

  public static class Line implements Iterable<Point> {

    final Point start;
    final Point step;

    public Line(Point start, Point step) {
      // Start one step back so I don't have to maintain unnecessary state.
      this.start = start.sub(step);
      this.step = step;
    }

    @Override
    public Iterator<Point> iterator() {
      return new Iterator<Point>() {
        Point p = start;

        @Override
        public boolean hasNext() {
          // we always have a next - lines never end.
          return true;
        }

        @Override
        public Point next() {
          return p = p.add(step);
        }

      };
    }

  }

  public static class Circle implements Iterable<Point> {

    // One octant of points.
    final List<Point> quarter;

    public Circle(int radius) {
      // Pre-calculate the first octant of points.
      List<Point> octant = firstOctant(radius);
      // If x == y on last point of octant - quarter is one smaller.
      Point last = octant.get(octant.size() - 1);
      // Do the octants cross at x == y?
      boolean cross = last.x == last.y;
      // Quarter is two octants.
      quarter = new ArrayList<>(octant);
      // If they cross - don't repeat the first one.
      for (int i = octant.size() - 1 - (cross ? 1 : 0); i > 0; i--) {
        quarter.add(octant.get(i).flip());
      }
      System.out.println("octant = " + octant);
      System.out.println("quarter = " + quarter);
    }

    private List<Point> firstOctant(int radius) {
      List<Point> o = new ArrayList<>();
      // Start at radius,0 and rotate ccw.
      Point p = new Point(radius, 0);
      // Error is initially 0.
      int e = 0;
      do {
        // Keep that point.
        o.add(p);
        // Make one step.
        if (e <= 0) {
          e += 2 * p.y + 1;
          p = p.add(0, 1);
        } else {
          e += 2 * (p.y - p.x) + 1;
          p = p.add(-1, 1);
        }
      } while (p.y <= p.x);
      return o;
    }

    private Point point(int i) {
      // Pick from the quarter.
      Point p = quarter.get(i % quarter.size());
      // Flip for which quarter.
      switch ((i / quarter.size()) % 4) {
        case 0:
          // Just leave it alone.
          break;
        case 1:
          // Flip and neg x.
          p = p.flip().mul(-1, 1);
          break;
        case 2:
          // Total negate.
          p = p.neg();
          break;
        case 3:
          // Flip and neg y.
          p = p.flip().mul(1, -1);
          break;
      }
      return p;
    }

    @Override
    public Iterator<Point> iterator() {
      return new Iterator<Point>() {
        // Which point we're on.
        int i = 0;

        @Override
        public boolean hasNext() {
          return i < 4 * quarter.size();
        }

        @Override
        public Point next() {
          // Where we are in the quarter.
          return point(i++);
        }

      };
    }

  }

  private static void test(int r) {
    System.out.println("Circle radius " + r);
    Circle c = new Circle(r);
    char[][] plot = new char[2 * r + 1][2 * r + 1];
    for (Point p : c) {
      plot[p.x + r][p.y + r] = 'X';
    }
    for (char[] row : plot) {
      StringBuilder s = new StringBuilder();
      for (char ch : row) {
        s.append(ch == 0 ? ' ' : ch);
      }
      System.out.println(s);
    }
  }

  public static void test() {
    for (int r = 0; r < 15; r++) {
      test(r);
    }
  }

  public static void main(String args[]) {
    try {
      test();
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}
