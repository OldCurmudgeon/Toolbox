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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

public class ContinuedFraction {

  ArrayList<Integer> e_frac;
  final static int MAX_FRACTION_LENGTH = 250;

  public ContinuedFraction() {
    e_frac = null;
  }

  public ContinuedFraction(int[] frac) {
    e_frac = new ArrayList<Integer>();
    for (int i : frac) {
      e_frac.add(i);
    }
  }

  public ContinuedFraction(ArrayList<Integer> frac) {
    e_frac = new ArrayList<Integer>();
    e_frac.addAll(frac);
  }

  public ContinuedFraction(BigDecimal bd) {
    e_frac = new ArrayList<Integer>();
    for (int n = 0; n < MAX_FRACTION_LENGTH; n++) {
      // Take the integer part.
      BigDecimal i = bd.setScale(0, RoundingMode.DOWN);
      e_frac.add(i.intValue());
      bd = BigDecimal.ONE.divide(bd.subtract(i), MAX_FRACTION_LENGTH, RoundingMode.HALF_UP);
    }
  }

  public ContinuedFraction(String s) {
    this(new BigDecimal(s));
  }

  public String toString() {
    if (e_frac == null) {
      return "[]";
    }
    return e_frac.toString().replaceFirst(",", ";");
  }

  public int get(int index) {
    // [3,1,1,1,1,6] 0=>3 1=>1 2=>1 3=>1 4=>1 5=>6 6=>1 7=>1 8=>1 9=>1 10=>6
    if (index >= e_frac.size()) {
      --index;
      // remove a0 from the calculations
      index = (index % (e_frac.size() - 1));
      ++index;
      return e_frac.get(index);
    }
    return e_frac.get(index);
  }

  public int periodSize() {
    if (e_frac != null) {
      return e_frac.size();
    }
    return 0;
  }

  /**
   * @param e_frac the sequence of continued fractions of e
   * @param n the precision to which to calculate
   * @return the value of the numerator at index n
   * @note http://en.wikipedia.org/wiki/Continued_fraction#Infinite_continued_fractions
   */
  BigInteger h(int i) {
    ArrayList<BigInteger> p = new ArrayList<BigInteger>();
    p.add(BigInteger.ZERO);
    p.add(BigInteger.ONE);

            // use the recursive theorem h(n) = a(n) * h(n-1) + h(n-2)
    // replaced recursion by loop, because recursion was taking far too long
    for (int n = 0; n <= i; ++n) {
      BigInteger an = BigInteger.valueOf(get(n));
      BigInteger pn_1 = p.get(n + 1);
      BigInteger pn_2 = p.get(n);

      p.add(an.multiply(pn_1).add(pn_2));
    }
    return p.get(p.size() - 1);
  }

  /**
   * @param e_frac the sequence of continued fractions of e
   * @param n the precision to which to calculate
   * @return the value of the denominator at index n
   * @note http://en.wikipedia.org/wiki/Continued_fraction#Infinite_continued_fractions
   */
  BigInteger k(int i) {
    ArrayList<BigInteger> q = new ArrayList<BigInteger>();
    q.add(BigInteger.ONE);
    q.add(BigInteger.ZERO);
    q.add(BigInteger.ONE);

            // use the recursive theorem k(n) = k(n) * k(n-1) + k(n-2)
    // replaced recursion by loop, because recursion was taking far too long
    for (int n = 1; n <= i; ++n) {
      BigInteger an = BigInteger.valueOf(get(n));
      q.add(an.multiply(q.get(n + 1)).add(q.get(n)));
    }
    return q.get(q.size() - 1);
  }

  public Fraction getConvergent(int n) {
    if (e_frac == null) {
      throw new IllegalArgumentException("Fraction period needed");
    }
    return new Fraction(h(n), k(n));
  }

  /**
   * @param S the number to calculate the square root from
   * @return the pattern of the periodic fraction
   * @throws Exception if not pattern can be found (increase MAX_FRACTION_LENGTH if this happens!)
   */
  private static ArrayList<Integer> getPeriod(int S) throws Exception {
    // get the fraction digits
    ArrayList<Integer> fraction = getContinuedFraction(S);
    // create a result array
    ArrayList<Integer> result = new ArrayList<Integer>();

    // add a0 to the result (and remove it from the original fraction)
    result.add(fraction.remove(0));
    int a0 = result.get(0);
            // now add as many digits from the fraction until we find one that is exactly twice the a0
    // i am not sure why this is so, but taking a quick look at the periodic fractions it becomes
    // immediately obvious that it is so
    for (int i : fraction) {
      result.add(i);
      if (i == 2 * a0) {
        break;
      }
    }
    // check again for even square roots and the 2 * a0
    if (fraction.size() > 1 && 2 * a0 != result.get(result.size() - 1)) {
      throw new Exception("no pattern found for " + S);
    }
    return result;
  }

  /**
   * @param S the number to calculate the fraction from
   * @return the continued fraction for MAX_FRACTION_LENGTH digits
   * @note http://en.wikipedia.org/wiki/Methods_of_computing_square_roots#Example.2C_square_root_of_114_as_a_continued_fraction
   */
  private static ArrayList<Integer> getContinuedFraction(int S) {
    // the algorithm to compute square roots using continued fraction is adapted from wikipedia
    int a0 = (int) Math.sqrt(S);
    int d0 = 1;
    int m0 = 0;
    int mn = m0;
    int dn = d0;
    int an = a0;

    // add a0 immediately
    ArrayList<Integer> result = new ArrayList<Integer>();
    result.add(an);

    // if S is an even root, there is no fraction to calculate
    if (S == a0 * a0) {
      return result;
    }

    // now generate all digits of the fraction and add them to the result array
    for (int i = 0; i < MAX_FRACTION_LENGTH; ++i) {
      mn = dn * an - mn;
      dn = (S - mn * mn) / dn;
      an = (a0 + mn) / dn;
      result.add(an);
    }
    return result;
  }

  public static ContinuedFraction fromSquare(int S) throws Exception {
    ArrayList<Integer> frac = getPeriod(S);
    ContinuedFraction cf = new ContinuedFraction(frac);
    return cf;
  }
}
