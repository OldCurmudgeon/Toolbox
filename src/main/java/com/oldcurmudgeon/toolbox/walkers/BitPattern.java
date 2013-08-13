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
package com.oldcurmudgeon.toolbox.walkers;

import java.math.BigInteger;
import java.util.Iterator;

/**
 * Iterates all bit patterns containing the specified number of bits.
 *
 * See "Compute the lexicographically next bit permutation"
 * http://graphics.stanford.edu/~seander/bithacks.html#NextBitPermutation
 *
 * @author OldCurmudgeon
 */
public class BitPattern implements Iterable<BigInteger> {
  // Useful stuff.
  private static final BigInteger ONE = BigInteger.ONE;
  private static final BigInteger TWO = ONE.add(ONE);
  // How many bits to work with.
  private final int bits;
  // Value to stop at. 2^max_bits.
  private final BigInteger stop;
  // Should we invert the output.
  private final boolean not;

  // All patterns of that many bits up to the specified number of bits - invberting if required.
  public BitPattern(int bits, int max, boolean not) {
    this.bits = bits;
    this.stop = TWO.pow(max);
    this.not = not;
  }

  // All patterns of that many bits up to the specified number of bits.
  public BitPattern(int bits, int max) {
    this(bits, max, false);
  }

  @Override
  public Iterator<BigInteger> iterator() {
    return new BitPatternIterator();
  }

  /*
   * From the link:
   * 
   * Suppose we have a pattern of N bits set to 1 in an integer and 
   * we want the next permutation of N 1 bits in a lexicographical sense. 
   * 
   * For example, if N is 3 and the bit pattern is 00010011, the next patterns would be 
   * 00010101, 00010110, 00011001,
   * 00011010, 00011100, 00100011, 
   * and so forth. 
   * 
   * The following is a fast way to compute the next permutation. 
   */
  private class BitPatternIterator implements Iterator<BigInteger> {
    // Next to deliver - initially 2^n - 1
    BigInteger next = TWO.pow(bits).subtract(ONE);
    // The last one we delivered.
    BigInteger last;

    @Override
    public boolean hasNext() {
      if (next == null) {
        // Next one!
        // t gets v's least significant 0 bits set to 1
        // unsigned int t = v | (v - 1); 
        BigInteger t = last.or(last.subtract(BigInteger.ONE));
        // Silly optimisation.
        BigInteger notT = t.not();
        // Next set to 1 the most significant bit to change, 
        // set to 0 the least significant ones, and add the necessary 1 bits.
        // w = (t + 1) | (((~t & -~t) - 1) >> (__builtin_ctz(v) + 1));
        // The __builtin_ctz(v) GNU C compiler intrinsic for x86 CPUs returns the number of trailing zeros.
        next = t.add(ONE).or(notT.and(notT.negate()).subtract(ONE).shiftRight(last.getLowestSetBit() + 1));
        if (next.compareTo(stop) >= 0) {
          // Dont go there.
          next = null;
        }
      }
      return next != null;
    }

    @Override
    public BigInteger next() {
      last = hasNext() ? next : null;
      next = null;
      return not ? last.not(): last;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
    
    @Override
    public String toString () {
      return next != null ? next.toString(2) : last != null ? last.toString(2): "";
    }

  }

  public static void main(String[] args) {
    System.out.println("BitPattern(3, 10)");
    for (BigInteger i : new BitPattern(3, 10)) {
      System.out.println(i.toString(2));
    }
    int [] dance = {58,82,76,10,88,1,28,55,27,45,0,59,4,92,54,81,36,87,19,65,68,50,94,18,42,61,17,40,33,47,83,63,78,25,93,14,5,56,8,85,41,39,73,75,72,9,16,3,84,74,52,35,67,21,2,49,37,26,11,91,57,44,48,53,20,90,6,13,89,22,29,51,62,23,31,77,71,80,79,24,46,38,30,60,34,32,64,86,15,69,66,43,70,12,7};
    System.out.println("BitDance(15, 95)");
    int count = 1000;
    for (BigInteger i : Iterables.in(BitDance.dance(new BitPattern(15, 95).iterator(), dance)) ) {
      System.out.println(i.toString(2));
      if ( --count < 0 ) {
        break;
      }
    }
  }

}
