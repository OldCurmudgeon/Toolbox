package com.oldcurmudgeon.toolbox.polynomial;

import com.oldcurmudgeon.toolbox.walkers.BitPattern;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * https://code.google.com/p/rabinfingerprint/source/browse/trunk/src/org/bdwyer/galoisfield/Polynomial.java?r=4
 *
 * NB: All in Z[2]
 * 
 * Galois field http://www.ee.unb.ca/tervo/ee4253/poly.shtml
 *
 * @author themadcreator
 */
public class Polynomial implements Arithmetic<Polynomial>, Comparable< Polynomial> {
  /**
   * number of elements in the finite field GF(2^k)
   */
  private static final long Q = 2L;
  private static final BigInteger BQ = BigInteger.valueOf(Q);
  /**
   * the polynomial "x"
   */
  public static final Polynomial X = Polynomial.createFrom(2L);
  /**
   * the polynomial "1"
   */
  public static final Polynomial ONE = Polynomial.createFrom(1L);

  /**
   * a reverse comparator so that polynomials are printed out correctly
   */
  private static final class ReverseComparator implements Comparator< BigInteger> {
    @Override
    public int compare(BigInteger o1, BigInteger o2) {
      return -1 * o1.compareTo(o2);
    }

  }

  /**
   * Constructs a polynomial using the bits from a long.
   */
  public static Polynomial createFrom(long l) {
    Set< BigInteger> dgrs = createDegreesCollection();
    int i = 0;
    while (l != 0) {
      if ((l & 1) == 1) {
        dgrs.add(BigInteger.valueOf(i));
      }
      i++;
      l >>= 1;
    }
    return new Polynomial(dgrs);
  }

  /**
   * Constructs a polynomial using the bits from an array of bytes, limiting
   * the degree to the specified size.
   *
   * We set the final degree to ensure a monic polynomial of the correct
   * degree.
   */
  public static Polynomial createFrom(byte[] bytes, long degree) {
    Set< BigInteger> dgrs = createDegreesCollection();
    // Stop when we hit the byte limit too - just for safety.
    for (int i = 0; i < degree && i / 8 < bytes.length; i++) {
      int aidx = (i / 8); // byte array index
      int bidx = i % 8; // bit index
      byte b = bytes[aidx];
      if (((b >> bidx) & 1) == 1) {
        dgrs.add(BigInteger.valueOf(i));
      }
    }
    dgrs.add(BigInteger.valueOf(degree));
    return new Polynomial(dgrs);
  }

  /**
   * Constructs a polynomial using the bits from a BigInteger.
   */
  public static Polynomial createFrom(BigInteger big, long degree) {
    Set< BigInteger> dgrs = createDegreesCollection();
    // NB: BigInteger uses Big Endian.
    byte[] bytes = big.toByteArray();

    for (int i = 0; i < degree; i++) {
      int aidx = bytes.length - 1 - (i / 8); // byte array index
      if (aidx >= 0) {
        int bidx = i % 8; // bit index
        byte b = bytes[aidx];
        if (((b >> bidx) & 1) == 1) {
          dgrs.add(BigInteger.valueOf(i));
        }
      }
    }
    dgrs.add(BigInteger.valueOf(degree));
    return new Polynomial(dgrs);
  }

  /**
   * Constructs a random polynomial of degree "degree"
   */
  public static Polynomial createRandom(long degree) {
    Random random = new Random();
    byte[] bytes = new byte[(int) (degree / 8) + 1];
    random.nextBytes(bytes);
    return createFrom(bytes, degree);
  }

  /**
   * Finds a random irreducible polynomial of degree "degree"
   */
  public static Polynomial createIrreducible(long degree) {
    while (true) {
      Polynomial p = createRandom(degree);
      if (p.getReducibility() == Reducibility.IRREDUCIBLE) {
        return p;
      }
    }
  }

  /**
   * An enum representing the reducibility of the polynomial
   *
   * A polynomial p(x) in GF(2^k) is called irreducible over GF[2^k] if it is
   * non-constant and cannot be represented as the product of two or more
   * non-constant polynomials from GF(2^k).
   *
   * http://en.wikipedia.org/wiki/Irreducible_element
   */
  public static enum Reducibility {
    REDUCIBLE, IRREDUCIBLE
  };
  /**
   * A (sorted) set of the degrees of the terms of the polynomial
   */
  private final TreeSet< BigInteger> degrees;

  public Polynomial() {
    this.degrees = createDegreesCollection();
  }

  protected Polynomial(Collection< BigInteger> degrees) {
    this();
    this.degrees.addAll(degrees);
  }

  public Polynomial(Polynomial p) {
    this(p.degrees);
  }

  protected static TreeSet< BigInteger> createDegreesCollection() {
    return new TreeSet<>(new ReverseComparator());
  }

  public BigInteger degree() {
    if (degrees.isEmpty()) {
      return BigInteger.ONE.negate();
    }
    return degrees.first();
  }

  public boolean isEmpty() {
    return degrees.isEmpty();
  }

  /**
   * Computes (this + that) in GF(2^k)
   */
  @Override
  public Polynomial plus(Polynomial that) {
    return xor(that);
  }

  /**
   * Computes (this - that) in GF(2^k)
   */
  @Override
  public Polynomial minus(Polynomial that) {
    return xor(that);
  }

  /**
   * Computes (this * that) in GF(2^k)
   */
  @Override
  public Polynomial times(Polynomial that) {
    Set< BigInteger> dgrs = createDegreesCollection();
    for (BigInteger pa : this.degrees) {
      for (BigInteger pb : that.degrees) {
        BigInteger sum = pa.add(pb);
        // xor the result
        if (dgrs.contains(sum)) {
          dgrs.remove(sum);
        } else {
          dgrs.add(sum);
        }
      }
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this & that) in GF(2^k)
   */
  @Override
  public Polynomial and(Polynomial that) {
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.retainAll(that.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this | that) in GF(2^k)
   */
  @Override
  public Polynomial or(Polynomial that) {
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.addAll(that.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this ^ that) in GF(2^k)
   */
  @Override
  public Polynomial xor(Polynomial that) {
    Polynomial or = this.or(that);
    Polynomial and = this.and(that);
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(or.degrees);
    dgrs.removeAll(and.degrees);
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this % that) in GF(2^k)
   */
  @Override
  public Polynomial mod(Polynomial that) {
    BigInteger da = this.degree();
    BigInteger db = that.degree();
    Polynomial mod = new Polynomial(this);
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      if (mod.hasDegree(i.add(db))) {
        mod = mod.xor(that.shiftLeft(i));
      }
    }
    return mod;
  }

  /**
   * Computes (this / that) in GF(2^k)
   * Probably not the most efficient.
   */
  @Override
  public Polynomial div(Polynomial that) {
    BigInteger da = this.degree();
    BigInteger db = that.degree();
    Polynomial mod = new Polynomial(this);
    Polynomial div = new Polynomial();
    for (BigInteger i = da.subtract(db); i.compareTo(BigInteger.ZERO) >= 0; i = i.subtract(BigInteger.ONE)) {
      if (mod.hasDegree(i.add(db))) {
        mod = mod.xor(that.shiftLeft(i));
        div = div.or(ONE.shiftLeft(i));
      }
    }
    return div;
  }

  /**
   * Computes (this << shift) in GF(2^k)
   */
  public Polynomial shiftLeft(BigInteger shift) {
    Set< BigInteger> dgrs = createDegreesCollection();
    for (BigInteger degree : degrees) {
      BigInteger shifted = degree.add(shift);
      dgrs.add(shifted);
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes (this >> shift) in GF(2^k)
   */
  public Polynomial shiftRight(BigInteger shift) {
    Set< BigInteger> dgrs = createDegreesCollection();
    for (BigInteger degree : degrees) {
      BigInteger shifted = degree.subtract(shift);
      if (shifted.compareTo(BigInteger.ZERO) < 0) {
        continue;
      }
      dgrs.add(shifted);
    }
    return new Polynomial(dgrs);
  }

  /**
   * Tests if there exists a term with degree k
   */
  public boolean hasDegree(BigInteger k) {
    return degrees.contains(k);
  }

  /**
   * Sets the coefficient of the term with degree k to 1
   */
  public Polynomial setDegree(BigInteger k) {
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.add(k);
    return new Polynomial(dgrs);
  }

  /**
   * Sets the coefficient of the term with degree k to 0
   */
  public Polynomial clearDegree(BigInteger k) {
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    dgrs.remove(k);
    return new Polynomial(dgrs);
  }

  /**
   * Toggles the coefficient of the term with degree k
   */
  public Polynomial toggleDegree(BigInteger k) {
    Set< BigInteger> dgrs = createDegreesCollection();
    dgrs.addAll(this.degrees);
    if (dgrs.contains(k)) {
      dgrs.remove(k);
    } else {
      dgrs.add(k);
    }
    return new Polynomial(dgrs);
  }

  /**
   * Computes x^e mod m.
   *
   * This algorithm requires at most this.degree() + m.degree() space.'
   *
   * http://en.wikipedia.org/wiki/Modular_exponentiation
   */
  public Polynomial modPow(BigInteger e, Polynomial m) {

    Polynomial result = Polynomial.ONE;
    Polynomial b = new Polynomial(this);

    while (e.bitCount() != 0) {
      if (e.testBit(0)) {
        result = result.times(b).mod(m);
      }
      e = e.shiftRight(1);
      b = b.times(b).mod(m);
    }

    return result;
  }

  /**
   * Computes the greatest common divisor between polynomials using Euclid's algorithm
   *
   * http://en.wikipedia.org/wiki/Euclids_algorithm
   */
  @Override
  public Polynomial gcd(Polynomial b) {
    Polynomial a = new Polynomial(this);
    while (!b.isEmpty()) {
      Polynomial t = new Polynomial(b);
      b = a.mod(b);
      a = t;
    }
    return a;
  }

  /**
   * Construct a BigInteger whose value represents this polynomial. This can
   * lose information if the degrees of the terms are larger than
   * Integer.MAX_VALUE;
   */
  public BigInteger toBigInteger() {
    BigInteger b = BigInteger.ZERO;
    for (BigInteger degree : degrees) {
// technically accurate but slow as hell:
//                      BigInteger term = BigInteger.ONE;
//                      for ( BigInteger i = BigInteger.ONE; i.compareTo( degree ) >= 0; i = i.add( BigInteger.ONE ) ) {
//                              term = term.shiftLeft( 1 );
//                      }
      b = b.setBit((int) degree.longValue());
    }
    return b;
  }

  /**
   * Returns a string of hex characters representing this polynomial
   */
  public String toHexString() {
    return toBigInteger().toString(16).toUpperCase();
  }

  /**
   * Returns a string of digits presenting this polynomial
   */
  public String toDecimalString() {
    return toBigInteger().toString();
  }

  /**
   * Returns a string of binary digits presenting this polynomial
   */
  public String toBinaryString() {
    StringBuilder str = new StringBuilder();
    for (BigInteger deg = degree(); deg.compareTo(BigInteger.ZERO) >= 0; deg = deg.subtract(BigInteger.ONE)) {
      if (degrees.contains(deg)) {
        str.append("1");
      } else {
        str.append("0");
      }
    }
    return str.toString();
  }

  /**
   * Returns standard ascii representation of this polynomial in the form:
   *
   * e.g.: x^8 + x^4 + x^3 + x + 1
   */
  public String toPolynomialString() {
    StringBuilder str = new StringBuilder();
    for (BigInteger degree : degrees) {
      if (str.length() != 0) {
        str.append(" + ");
      }
      if (degree.compareTo(BigInteger.ZERO) == 0) {
        str.append("1");
      } else {
        str.append("x");
        if (degree.compareTo(BigInteger.ONE) > 0) {
          str.append("^").append(degree);
        }
      }
    }
    return str.toString();
  }

  /**
   * Default toString override uses the ascii representation
   */
  @Override
  public String toString() {
    return toPolynomialString();
  }

  /**
   * Tests the reducibility of the polynomial
   */
  public boolean isReducible() {
    return getReducibility() == Reducibility.REDUCIBLE;
  }

  /*
   * An irreducible polynomial of degree m, 
   * F(x) over GF(p) for prime p, is a primitive polynomial 
   * if the smallest positive integer n such that 
   * F(x) divides x^n - 1 is n = p^m − 1.
   * 
   * ToDo: Somehow retain primality.
   * 
   * By discovery: x^12 + x^3 + 1 is NOT primitive but IS prime.
   * 
   * http://theory.cs.uvic.ca/inf/neck/PolyInfo.html
   * 
   * A polynomial over GF(2) is primitive if it has order 2^n-1. 
   * For example, x2+x+1 has order 3 = 2^2-1 since (x^2+x+1)(x+1) = x^3+1. 
   * Thus x^2+x+1 is primitive. 
   * 
   * http://maths-people.anu.edu.au/~brent/pd/rpb199.pdf
   * 
   * A polynomial P(x) of degree r > 1 is primitive if P(x) is irreducible
   * and x^j != 1 mod P(x) for 0 < j < 2^r - 1.
   * 
   * http://mathworld.wolfram.com/PrimitivePolynomial.html
   * 
   * A polynomial of degree n over the finite field GF(2) 
   * (i.e., with coefficients either 0 or 1) is primitive if it has 
   * polynomial order 2^n-1
   * 
   * http://mathworld.wolfram.com/PolynomialOrder.html
   * 
   * In particular, the order of a polynomial P(x) with P(0)!=0 is the 
   * smallest integer e for which P(x) divides x^e+1 (Lidl and Niederreiter 1994).
   */
  public boolean isPrimitive() {
    try {
      return Primitivity.test(this);
    } catch (InterruptedException | ExecutionException ex) {
      // Rethrow it as a runtime exception.
      throw new RuntimeException(ex);
    }
  }

  private static class Primitivity {
    // The common pool.
    private static ForkJoinPool pool = new ForkJoinPool();
    // How many calculations per task.
    private static final BigInteger GRANULARITY = BigInteger.valueOf(1000);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    // Test failed.
    volatile static boolean failed = false;

    // Tests primitivity of a Polynomial using the pool.
    private static boolean test(Polynomial p) throws InterruptedException, ExecutionException {
      // The required order o = 2^r - 1
      BigInteger o = BQ.pow(p.degree().intValue()).subtract(BigInteger.ONE);
      // Build the task.
      Task task = new Task(p, BigInteger.ONE, o);
      // Initially not failed.
      failed = false;
      // Process it in the pool.
      pool.invoke(task);
      // Deliver the answer.
      return task.get();
    }

    private static class Task extends RecursiveTask<Boolean> {
      // The polynomial we are testing.
      final Polynomial it;
      // Where to start.
      final BigInteger start;
      // Where to stop.
      final BigInteger stop;

      public Task(Polynomial it, BigInteger start, BigInteger stop) {
        this.it = it;
        this.start = start;
        this.stop = stop;
      }

      @Override
      protected Boolean compute() {
        // Do nothing if failed already.
        if (!failed) {
          // Compute or fork?
          if (stop.subtract(start).compareTo(GRANULARITY) < 0) {
            return computeDirectly();
          } else {
            // Fork!
            BigInteger split = stop.subtract(start).divide(TWO).add(start);
            Task is1 = new Task(it, start, split);
            Task is2 = new Task(it, split, stop);
            is1.fork();
            // Join.
            return is2.compute().booleanValue() && is1.join().booleanValue();
          }
        }
        // Definitely not if failed.
        return false;
      }

      protected Boolean computeDirectly() {
        // Do it for myself.
        for (BigInteger e = start; e.compareTo(stop) < 0 && !failed; e = e.add(BigInteger.ONE)) {
          // p = (x^e + 1)
          Polynomial p = ONE.shiftLeft(e).plus(ONE);
          if (p.mod(it).isEmpty()) {
            // We failed.
            failed = true;
            System.out.println("Reject " + it + " = (" + p + ")/(" + p.div(it)+")");
          }
        }
        return !failed;
      }

    }
  }

  /**
   * Tests the reducibility of the polynomial
   */
  public Reducibility getReducibility() {
    return getReducibilityBenOr();
  }

  /**
   * BenOr Reducibility Test
   *
   * Tests and Constructions of Irreducible Polynomials over Finite Fields
   * (1997) Shuhong Gao, Daniel Panario
   *
   * http://citeseer.ist.psu.edu/cache/papers/cs/27167/http:zSzzSzwww.math.clemson.eduzSzfacultyzSzGaozSzpaperszSzGP97a.pdf/gao97tests.pdf
   */
  protected Reducibility getReducibilityBenOr() {
    final long degree = this.degree().longValue();
    for (int i = 1; i <= (int) (degree / 2); i++) {
      Polynomial b = reduceExponent(i);
      Polynomial g = this.gcd(b);
      if (g.compareTo(Polynomial.ONE) != 0) {
        return Reducibility.REDUCIBLE;
      }
    }

    return Reducibility.IRREDUCIBLE;
  }

  /**
   * Rabin's Reducibility Test
   *
   * This requires the distinct prime factors of the degree, so we don't use
   * it. But this could be faster for prime degree polynomials
   */
  protected Reducibility getReducibilityRabin(int[] factors) {
    int degree = (int) degree().longValue();
    for (int i = 0; i < factors.length; i++) {
      //int n_i = factors[i];
      Polynomial b = reduceExponent(i);
      Polynomial g = this.gcd(b);
      if (g.compareTo(Polynomial.ONE) != 0) {
        return Reducibility.REDUCIBLE;
      }
    }

    Polynomial g = reduceExponent(degree);
    if (!g.isEmpty()) {
      return Reducibility.REDUCIBLE;
    }

    return Reducibility.IRREDUCIBLE;
  }

  /**
   * Computes ( x^(2^p) - x ) mod f
   *
   * This function is useful for computing the reducibility of the polynomial
   */
  private Polynomial reduceExponent(final int p) {
    // compute (x^q^p mod f)
    BigInteger q_to_p = BQ.pow(p);
    Polynomial x_to_q_to_p = X.modPow(q_to_p, this);

    // subtract (x mod f)
    return x_to_q_to_p.xor(X).mod(this);
  }

  /**
   * Compares this polynomial to the other
   */
  @Override
  public int compareTo(Polynomial o) {
    int cmp = degree().compareTo(o.degree());
    if (cmp != 0) {
      return cmp;
    }
    // get first degree difference
    Polynomial x = this.xor(o);
    if (x.isEmpty()) {
      return 0;
    }
    return this.hasDegree(x.degree()) ? 1 : 0;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof Polynomial)) {
      return false;
    }
    // All degrees are the same.
    return degrees.equals(((Polynomial) that).degrees);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.degrees);
    return hash;
  }

  // Iterate over prime polynomials.
  public static class PrimePolynomials implements Iterable<Polynomial> {
    // TODO: Use a FilteredIterator.
    private final int degree;
    private final boolean primitive;
    private final boolean reverse;

    public PrimePolynomials(int degree, boolean primitive, boolean reverse) {
      this.degree = degree;
      this.primitive = primitive;
      this.reverse = reverse;
    }

    public PrimePolynomials(int degree, boolean primitive) {
      this(degree, primitive, false);
    }

    public PrimePolynomials(int degree) {
      this(degree, false, false);
    }

    private class PrimeIterator implements Iterator<Polynomial> {
      // How many bits we are working on right now.
      int bits = 1;
      // The current pattern.
      Iterator<BigInteger> pattern = new BitPattern(bits, degree - 1, reverse).iterator();
      // Next one to deliver.
      Polynomial next = null;
      Polynomial last = null;
      // Have we finished?
      boolean finished = false;

      @Override
      public boolean hasNext() {

        while (next == null && !finished) {
          // Turn the wheels.
          if (!pattern.hasNext()) {
            // Exhausted! Step bits.
            bits += 1;
            // Note when we've finished.
            finished = bits >= degree;
            // Next bit pattern.
            if (!finished) {
              pattern = new BitPattern(bits, degree - 1, reverse).iterator();
            }
          }
          if (!finished && pattern.hasNext()) {
            // Roll a polynomial of base + this pattern.
            // i.e. 2^d + ... + 1
            Polynomial p = Polynomial.createFrom(pattern.next().multiply(TWO), degree).plus(Polynomial.ONE);
            // Is it a prime poly?
            boolean ok = !p.isReducible();
            if (ok && primitive) {
              ok &= p.isPrimitive();
            }
            if (ok) {
              next = p;
            }
          }
        }
        return next != null;
      }

      @Override
      public Polynomial next() {
        Polynomial it = hasNext() ? next : null;
        next = null;
        last = it;
        return it;
      }

      @Override
      public void remove() {
        // To change body of generated methods, choose Tools | Templates.
        throw new UnsupportedOperationException("Not supported.");
      }

      @Override
      public String toString() {
        return next != null ? next.toString() : last != null ? last.toString() : "";
      }

    }

    @Override
    public Iterator<Polynomial> iterator() {
      return new PrimeIterator();
    }

  }
  public static final BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);

  public static void main(String[] args) {
    Polynomial q = Polynomial.createFrom(0x5);
    Polynomial d = Polynomial.createFrom(0x3);
    // Should be 0
    Polynomial mod = q.mod(d);
    // Should be x + 1
    Polynomial div = q.div(d);
    // Big!
    //generatePrimitivePolys(95, 2, true);

    //generateMinimalPrimitivePolys(4, 5);
    //generateMinimalPrimitivePolys(12, 5);
    //generateMinimalPrimePolys(5);
    generatePrimitivePolysUpToDegree(13, 3, true);
    generatePrimitivePolysUpToDegree(13, 3, false);
    //generateMinimalPrimePolysUpToDegree(96);
    generatePrimitivePolys(95, 1, true);
    generatePrimitivePolys(95, 1, false);
    generatePrimitivePolys(96, 1, false);
    generatePrimitivePolys(255, 1, false);
    generatePrimitivePolys(256, 1, false);
  }

  private static void generatePrimePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree < d; degree++) {
      generatePrimePolys(degree, max, minimal);
    }
  }

  private static void generatePrimePolys(int degree, int count, boolean minimal) {
    System.out.println("Degree: " + degree + (minimal?" minimal":" maximal"));
    int seen = 0;
    for (Polynomial p : new PrimePolynomials(degree, false, minimal ? false : true)) {
      // Prime Polynomials!
      System.out.println("Prime poly: " + p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        System.out.println("...");
        break;
      }

    }
  }

  private static void generatePrimitivePolysUpToDegree(int d, int max, boolean minimal) {
    for (int degree = 2; degree < d; degree++) {
      generatePrimitivePolys(degree, max, minimal);
    }
  }

  private static void generatePrimitivePolys(int degree, int count, boolean minimal) {
    System.out.println("Degree: " + degree + (minimal?" minimal":" maximal"));
    int seen = 0;
    for (Polynomial p : new PrimePolynomials(degree, true, minimal ? false : true)) {
      // Prime Polynomials!
      System.out.println("Primitive poly: " + p);
      seen += 1;
      if (seen >= count) {
        // Stop after the 1st 10 for speed - one day enumerate all.
        System.out.println("...");
        break;
      }

    }
  }

}
