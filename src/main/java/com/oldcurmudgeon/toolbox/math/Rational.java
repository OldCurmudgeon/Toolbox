/*
 * Copyright 2014 OldCurmudgeon.
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

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ***********************************************************************
 * Compilation: javac Rational.java Execution: java Rational
 *
 * Immutable ADT for Rational numbers.
 *
 * Invariants ----------- - gcd(num, den) = 1, i.e, the rational number is in
 * reduced form - den >= 1, the denominator is always a positive integer - 0/1
 * is the unique representation of 0
 *
 * We employ some tricks to stave of overflow, but if you need arbitrary
 * precision rationals, use BigRational.java.
 *
 ************************************************************************
 */
public class Rational extends Number implements Comparable<Rational> {

    private static final Rational ZERO = new Rational(0, 1);
    private static final Rational ONE = new Rational(1, 1);

    private long num;   // the numerator
    private long den;   // the denominator

    // create and initialize a new Rational object
    public Rational(long numerator, long denominator) {

        // deal with x/0
        //if (denominator == 0) {
        //   throw new RuntimeException("Denominator is zero");
        //}
        // reduce fraction
        long g = gcd(numerator, denominator);
        num = numerator / g;
        den = denominator / g;

        // only needed for negative numbers
        if (den < 0) {
            den = -den;
            num = -num;
        }
    }

    // return the numerator and denominator of (this)
    public long numerator() {
        return num;
    }

    public long denominator() {
        return den;
    }

    // return double precision representation of (this)
    public double toDouble() {
        return (double) num / den;
    }

    // return string representation of (this)
    @Override
    public String toString() {
        if (den == 1) {
            return num + "";
        } else {
            return num + "/" + den;
        }
    }

    // return { -1, 0, +1 } if a < b, a = b, or a > b
    @Override
    public int compareTo(Rational b) {
        Rational a = this;
        long lhs = a.num * b.den;
        long rhs = a.den * b.num;
        if (lhs < rhs) {
            return -1;
        }
        if (lhs > rhs) {
            return +1;
        }
        return 0;
    }

    // is this Rational object equal to y?
    @Override
    public boolean equals(Object y) {
        if (y == null) {
            return false;
        }
        if (y.getClass() != this.getClass()) {
            return false;
        }
        Rational b = (Rational) y;
        return compareTo(b) == 0;
    }

    // hashCode consistent with equals() and compareTo()
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (this.num ^ (this.num >>> 32));
        hash = 97 * hash + (int) (this.den ^ (this.den >>> 32));
        return hash;
    }

    // create and return a new rational (r.num + s.num) / (r.den + s.den)
    public static Rational mediant(Rational r, Rational s) {
        return new Rational(r.num + s.num, r.den + s.den);
    }

    // return gcd(|m|, |n|)
    private static long gcd(long m, long n) {
        if (m < 0) {
            m = -m;
        }
        if (n < 0) {
            n = -n;
        }
        if (0 == n) {
            return m;
        } else {
            return gcd(n, m % n);
        }
    }

    // return lcm(|m|, |n|)
    private static long lcm(long m, long n) {
        if (m < 0) {
            m = -m;
        }
        if (n < 0) {
            n = -n;
        }
        return m * (n / gcd(m, n));    // parentheses important to avoid overflow
    }

    // return a * b, staving off overflow as much as possible by cross-cancellation
    public Rational times(Rational b) {
        Rational a = this;

        // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
        Rational c = new Rational(a.num, b.den);
        Rational d = new Rational(b.num, a.den);
        return new Rational(c.num * d.num, c.den * d.den);
    }

    // return a + b, staving off overflow
    public Rational plus(Rational b) {
        Rational a = this;

        // special cases
        if (a.compareTo(ZERO) == 0) {
            return b;
        }
        if (b.compareTo(ZERO) == 0) {
            return a;
        }

        // Find gcd of numerators and denominators
        long f = gcd(a.num, b.num);
        long g = gcd(a.den, b.den);

        // add cross-product terms for numerator
        Rational s = new Rational((a.num / f) * (b.den / g) + (b.num / f) * (a.den / g),
                lcm(a.den, b.den));

        // multiply back in
        s.num *= f;
        return s;
    }

    // return -a
    public Rational negate() {
        return new Rational(-num, den);
    }

    // return a - b
    public Rational minus(Rational b) {
        Rational a = this;
        return a.plus(b.negate());
    }

    public Rational reciprocal() {
        return new Rational(den, num);
    }

    // return a / b
    public Rational divides(Rational b) {
        Rational a = this;
        return a.times(b.reciprocal());
    }

// Default delta to apply.
    public static final double DELTA = 0.000001;

    public static Rational valueOf(double dbl) {
        return valueOf(dbl, DELTA);
    }

    // Create a good rational for the value within the delta supplied.
    public static Rational valueOf(double dbl, double delta) {
        // Primary checks.
        if ( delta <= 0.0 ) {
            throw new IllegalArgumentException("Delta must be > 0.0");
        }
        // Remove the integral part.
        long integral = (long) Math.floor(dbl);
        dbl -= integral;
        // The value we are looking for.
        final Rational d = new Rational((long) ((dbl) / delta), (long) (1 / delta));
        // Min value = d - delta.
        final Rational min = new Rational((long) ((dbl - delta) / delta), (long) (1 / delta));
        // Max value = d + delta.
        final Rational max = new Rational((long) ((dbl + delta) / delta), (long) (1 / delta));
        // Start the fairey sequence.
        Rational l = ZERO;
        Rational h = ONE;
        Rational found = null;
        // Keep slicing until we arrive within the delta range.
        do {
            // Either between min and max -> found it.
            if (found == null && min.compareTo(l) <= 0 && max.compareTo(l) >= 0) {
                found = l;
            }
            if (found == null && min.compareTo(h) <= 0 && max.compareTo(h) >= 0) {
                found = h;
            }
            if (found == null) {
                // Make the mediant.
                Rational m = mediant(l, h);
                // Replace either l or h with mediant.
                if (m.compareTo(d) < 0) {
                    l = m;
                } else {
                    h = m;
                }
            }

        } while (found == null);

        // Bring back the sign and the integral.
        if (integral != 0) {
            found = found.plus(new Rational(integral, 1));
        }
        // That's me.
        return found;
    }    

    public BigDecimal toDecimal() {
        return toDecimal(4);
    }

    public BigDecimal toDecimal(int digits) {
        return new BigDecimal(num).divide(new BigDecimal(den), digits, RoundingMode.DOWN).stripTrailingZeros();
    }

    @Override
    public int intValue() {
        return (int) toDouble();
    }

    @Override
    public long longValue() {
        return (long) toDouble();
    }

    @Override
    public float floatValue() {
        return (float) toDouble();
    }

    @Override
    public double doubleValue() {
        return toDouble();
    }

    // test client
    private static void test3(double d) {
        test3(d, DELTA);
    }
    private static void test3(double d, double delta) {
        Rational r = valueOf(d, delta);
        System.out.println("valueOf("+d+")"+"="+r+" - "+r.toDecimal());
    }

    private static void test2() {
        test3(-Math.PI);
        test3(0.100000001490116119384765625);
        test3(1.0/3.0);
        test3(Math.PI);
        test3(Math.E);
        test3(Math.PI, 0.00000000001);
        test3(Math.E, 0.00000000001);
    }

    private static void test1() {
        Rational x, y, z;

        // 1/2 + 1/3 = 5/6
        x = new Rational(1, 2);
        y = new Rational(1, 3);
        z = x.plus(y);
        System.out.println(z);

        // 8/9 + 1/9 = 1
        x = new Rational(8, 9);
        y = new Rational(1, 9);
        z = x.plus(y);
        System.out.println(z);

        // 1/200000000 + 1/300000000 = 1/120000000
        x = new Rational(1, 200000000);
        y = new Rational(1, 300000000);
        z = x.plus(y);
        System.out.println(z);

        // 1073741789/20 + 1073741789/30 = 1073741789/12
        x = new Rational(1073741789, 20);
        y = new Rational(1073741789, 30);
        z = x.plus(y);
        System.out.println(z);

        //  4/17 * 17/4 = 1
        x = new Rational(4, 17);
        y = new Rational(17, 4);
        z = x.times(y);
        System.out.println(z);

        // 3037141/3247033 * 3037547/3246599 = 841/961 
        x = new Rational(3037141, 3247033);
        y = new Rational(3037547, 3246599);
        z = x.times(y);
        System.out.println(z);

        // 1/6 - -4/-8 = -1/3
        x = new Rational(1, 6);
        y = new Rational(-4, -8);
        z = x.minus(y);
        System.out.println(z);
    }

    public static void main(String[] args) {
        // test1();
        test2();
    }

}
