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

import java.util.Map;
import java.util.TreeMap;

public class PrimeFactorList extends TreeMap<Long, Long> {

  private static final long serialVersionUID = 1L;
  private Primes _primes;

  /**
   * @param i number to be factorized
   */
  private void factorizeNumber(long i) {
    // check for stupid numbers
    if (i <= 0) {
      return;
    }

    if (_primes.isPrime(i)) {
      this.put(i, getPower(i) + 1);
      return;
    }

    try {
      // we divide the original number by its primes until it is no longer divisible
      while (i > 1) {
        // check for 2 seperatly so the next loop can be optimized a bit
        if (i % 2 == 0) {
          this.put(2L, getPower(2) + 1);
          i = i / 2;
          continue;
        }
        // start with 3 and skip all even numbers (since even numbers are just powers of 2 and other primes)
        for (long x = 3; x <= i; x += 2) {
          if (((i % x) == 0) && _primes.isPrime(x)) {
                            // since we not only want to know which primes are a factor
            // but also in which power, we will add one power for each factor we find
            this.put(x, getPower(x) + 1);
            i = i / x;
            break;
          }
        }
      }
    } catch (Exception e) {
      this.clear();
    }
  }

  /**
   * @param i number to be factorized
   * @note default constructor is used to factorize a number immediatly
   */
  public PrimeFactorList(long i) {
    super();
    _primes = new Primes();
    factorizeNumber(i);
  }

  /**
   * @param n number to factorize
   * @param opt seed for the prime class
   */
  public PrimeFactorList(long n, long opt) {
    super();
    _primes = new Primes(opt);
    factorizeNumber(n);
  }

  /**
   * @param n number to factorize
   */
  public void refactorize(long n) {
    this.clear();
    factorizeNumber(n);
  }

  /**
   * @param p1 list 1
   * @param p2 list 2
   * @return a new list containing the differences in the input lists
   * @note p1 should be smaller than p2 for reasonable results
   */
  public static PrimeFactorList differFactorLists(PrimeFactorList p1, PrimeFactorList p2) {
    // init the result empty
    PrimeFactorList result = new PrimeFactorList(0L);

    // get the differences in power in the two lists
    for (Map.Entry<Long, Long> entry : p2.entrySet()) {
      if (entry == null) {
        continue;
      }

      long i1 = entry.getValue();
      long i2 = p1.getPower(entry.getKey());
      long r = Math.abs(i1 - i2);

      // if a power cancels each other out dismiss it
      if (r > 0) {
        result.put(entry.getKey(), r);
      }
    }
    return result;
  }

  public static PrimeFactorList greatestCommonDivisor(PrimeFactorList[] others) {
    PrimeFactorList result = new PrimeFactorList(0L);

    // cancel out all same powers
    for (PrimeFactorList pfl : others) {
      if (pfl == null) {
        continue;
      }

      for (Map.Entry<Long, Long> entry : pfl.entrySet()) {
        if (entry == null) {
          continue;
        }

        long key = entry.getKey();
        boolean contains = false;
        long min = Long.MAX_VALUE;

        for (PrimeFactorList pfl2 : others) {
          if (pfl == pfl2) {
            continue;
          }

          if (!pfl2.containsKey(key)) {
            continue;
          }
          contains = true;
          min = Math.min(pfl.getPower(key), pfl2.getPower(key));
        }
        if (contains) {
          result.put(entry.getKey(), min);
        }
      }
      return result;
    }
    return result;
  }

  // take a bunch of indiviual lists and make them one
  public static PrimeFactorList lowestCommonMultiple(PrimeFactorList[] others) {
    PrimeFactorList result = new PrimeFactorList(0L);

    for (PrimeFactorList pfl : others) {
      if (pfl == null) {
        continue;
      }
      for (Map.Entry<Long, Long> entry : pfl.entrySet()) {
        if (entry == null) {
          continue;
        }

                    // if the current entry is of a larger power than what we had previously
        // we need to update the result with the larger power
        // otherwise we ignore it
        if (entry.getValue() > result.getPower(entry.getKey())) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return result;
  }

  // only way to check if a factor was used (returns 0 if not, or > 0 if used)
  public long getPower(long i) {
    if (this.containsKey(i)) {
      return this.get(i);
    }
    return 0;
  }

  // get each factor and its power and just multiply them
  public long calculateValue() {
    long result = 1;

    for (Map.Entry<Long, Long> entry : this.entrySet()) {
      // there is probably a math function which does the same
      for (long i = 0; i < entry.getValue(); ++i) {
        result = result * entry.getKey();
      }
    }

    return result;
  }
}
