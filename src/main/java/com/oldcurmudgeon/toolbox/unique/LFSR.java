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
package com.oldcurmudgeon.toolbox.unique;

import com.oldcurmudgeon.toolbox.polynomial.Polynomial;
import com.oldcurmudgeon.toolbox.twiddlers.Strings;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * Linear feedback shift register
 *
 * Taps can be found at:
 * See http://www.xilinx.com/support/documentation/application_notes/xapp052.pdf
 * See http://mathoverflow.net/questions/46961/how-are-taps-proven-to-work-for-lfsrs/46983#46983
 * See http://www.newwaveinstruments.com/resources/articles/m_sequence_linear_feedback_shift_register_lfsr.htm
 * See http://www.yikes.com/~ptolemy/lfsr_web/index.htm
 * See http://seanerikoconnor.freeservers.com/Mathematics/AbstractAlgebra/PrimitivePolynomials/overview.html
 * And on my flash.
 *
 * @author OldCurmudgeon
 */
public class LFSR implements Iterable<BigInteger> {
  // Bit pattern for taps.
  private final BigInteger tapsMask;
  // Where to start (and end).
  private final BigInteger start;

  // The poly must be prime to span the full sequence.
  public LFSR(Polynomial primePoly, BigInteger start) {
    // Where to start from (and stop).
    this.start = start;
    // Knock off the 2^0 coefficient of the polynomial for the TAP.
    this.tapsMask = primePoly.toBigInteger().shiftRight(1);
  }

  public LFSR(Polynomial primePoly) {
    // Default to start at 1.
    this(primePoly, BigInteger.ONE);
  }

  public LFSR(int bits) {
    // Default to first found prime poly.
    this(new Polynomial.PrimePolynomials(bits).iterator().next());
  }

  public LFSR(int bits, BigInteger start) {
    // Default to first prime poly.
    this(new Polynomial.PrimePolynomials(bits).iterator().next(), start);
  }

  @Override
  public Iterator<BigInteger> iterator() {
    return new LFSRIterator(start);
  }

  private class LFSRIterator implements Iterator<BigInteger> {
    // The last one we returned.
    private BigInteger last = null;
    // The next one to return.
    private BigInteger next = null;

    public LFSRIterator(BigInteger start) {
      next = start;
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        /*
         * Uses the Galois form.
         * 
         * Shift last right one.
         * 
         * If the bit shifted out was a 1 - xor with the tap mask.
         */
        boolean shiftedOutA1 = last.testBit(0);
        // Shift right.
        next = last.shiftRight(1);
        if (shiftedOutA1) {
          // Tap!
          next = next.xor(tapsMask);
        }
        // Never give them `start` again.
        if (next.equals(start)) {
          // Could set a finished flag here too.
          next = null;
        }
      }
      return next != null;
    }

    @Override
    public BigInteger next() {
      // Remember this one.
      last = hasNext() ? next : null;
      // Don't deliver it again.
      next = null;
      return last;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

  }

  public static void main(String args[]) {
    test(12);
    test(10);
    for (int bits = 3; bits <= 7; bits++) {
      test(bits);
    }
  }

  private static void test(int bits) {
    System.out.println("==== Bits " + bits + " ====");
    Polynomial p = new Polynomial.PrimePolynomials(bits,true,true).iterator().next();
    System.out.println("Poly " + p);
    LFSR lfsr = new LFSR(p);
    int count = 0;
    for (BigInteger i : lfsr) {
      System.out.println(Strings.pad(i.toString(2), Strings.zeros(bits)));
      count += 1;
    }
    System.out.println("Count " + count);
  }

}
