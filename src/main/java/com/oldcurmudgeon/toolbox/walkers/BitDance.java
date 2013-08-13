/*
 * Copyright 2013 Paul Caswell.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Moves all bits in source to a specified target.
 *
 * @author Paul Caswell
 */
public class BitDance extends Filter<BigInteger> {
  private final int[] dance;

  public BitDance(int[] dance) {
    // Every position must be accounted for if it is a real dance.
    int[] test = Arrays.copyOf(dance, dance.length);
    Arrays.sort(test);
    for (int i = 0; i < test.length; i++) {
      if (test[i] != i) {
        throw new IllegalArgumentException("Invalid dance - failure at " + i);
      }
    }
    // Take a copy so they cannot mess with me.
    this.dance = Arrays.copyOf(dance, dance.length);
  }

  // Return the filtered value.
  @Override
  public BigInteger filter(BigInteger it) {
    byte[] danced = new byte[(it.bitLength() + 7) / 8];
    for (int i = 0; i < dance.length; i++) {
      if (it.testBit(i)) {
        danced[dance[i] / 8] |= 1 << (dance[i] % 8);
      }
    }
    // Retain the sign of the original.
    return new BigInteger(it.signum(), danced);
  }

  public static void main(String args[]) {
    // Print a random dance for n bits.
    List<Integer> bits = new ArrayList<>();
    for (int i = 0; i < 95; i++) {
      bits.add(i);
    }
    Collections.shuffle(bits);
    System.out.println("Sample: " + Separator.separate("{", ",", "}", bits));
    int[] dance = new int[]{
      1, 3, 5, 7, 2, 4, 6, 0
    };
    Filter<BigInteger> f = new BitDance(dance);
    BigInteger test = BigInteger.valueOf(1L << 7);
    BigInteger danced = f.filter(test);
    System.out.println("dance(" + test.toString(2) + ")=" + danced.toString(2));
    BigInteger stop = BigInteger.valueOf(256);
    for (BigInteger i = BigInteger.ZERO; i.compareTo(stop) < 0; i = i.add(BigInteger.ONE)) {
      danced = f.filter(i);
      System.out.println("dance(" + i.toString(2) + ")=" + danced.toString(2));
      if (danced.bitCount() != i.bitCount()) {
        System.out.println("bitCount " + danced.bitCount() + " != " + i.bitCount());
      }
    }
  }

}
