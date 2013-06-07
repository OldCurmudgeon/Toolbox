package com.oldcurmudgeon.toolbox.polynomial;

/**
 * https://code.google.com/p/rabinfingerprint/source/browse/trunk/src/org/bdwyer/galoisfield/Arithmetic.java?r=4
 *
 * @author themadcreator
 */
public interface Arithmetic<T> {
  public T plus(T o);

  public T minus(T o);

  public T times(T o);

  public T and(T o);

  public T or(T o);

  public T xor(T o);

  public T mod(T o);

  public T div(T o);

  public T gcd(T o);

}
