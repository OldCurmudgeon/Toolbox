/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.toolbox.pipe;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * "Borrowed" from: http://stackoverflow.com/a/27644392/823393
 *
 * @author not pcaswell
 */
public class Exceptions {

  @FunctionalInterface
  public interface ExceptionalConsumer<T, E extends Exception> {

    void accept(T t) throws E;
  }

  @FunctionalInterface
  public interface ExceptionalBiConsumer<T, U, E extends Exception> {

    void accept(T t, U u) throws E;
  }

  @FunctionalInterface
  public interface ExceptionalFunction<T, R, E extends Exception> {

    R apply(T t) throws E;
  }

  @FunctionalInterface
  public interface ExceptionalSupplier<T, E extends Exception> {

    T get() throws E;
  }

  @FunctionalInterface
  public interface ExceptionalRunnable<E extends Exception> {

    void run() throws E;
  }

  /**
   * .forEach(rethrowConsumer(name -> System.out.println(Class.forName(name)))); or
   * .forEach(rethrowConsumer(ClassNameUtil::println));
   */
  public static <T, E extends Exception> Consumer<T> rethrowConsumer(ExceptionalConsumer<T, E> consumer) {
    return t -> {
      try {
        consumer.accept(t);
      } catch (Exception exception) {
        throwAsUnchecked(exception);
      }
    };
  }

  public static <T, U, E extends Exception> BiConsumer<T, U> rethrowBiConsumer(ExceptionalBiConsumer<T, U, E> biConsumer) {
    return (t, u) -> {
      try {
        biConsumer.accept(t, u);
      } catch (Exception exception) {
        throwAsUnchecked(exception);
      }
    };
  }

  /**
   * .map(rethrowFunction(name -> Class.forName(name))) or .map(rethrowFunction(Class::forName))
   */
  public static <T, R, E extends Exception> Function<T, R> rethrowFunction(ExceptionalFunction<T, R, E> function) {
    return t -> {
      try {
        return function.apply(t);
      } catch (Exception exception) {
        throwAsUnchecked(exception);
        return null;
      }
    };
  }

  /**
   * rethrowSupplier(() -> new StringJoiner(new String(new byte[]{77, 97, 114, 107}, "UTF-8"))),
   */
  public static <T, E extends Exception> Supplier<T> rethrowSupplier(ExceptionalSupplier<T, E> function) {
    return () -> {
      try {
        return function.get();
      } catch (Exception exception) {
        throwAsUnchecked(exception);
        return null;
      }
    };
  }

  /**
   * uncheck(() -> Class.forName("xxx"));
   */
  public static void uncheck(ExceptionalRunnable t) {
    try {
      t.run();
    } catch (Exception exception) {
      throwAsUnchecked(exception);
    }
  }

  /**
   * uncheck(() -> Class.forName("xxx"));
   */
  public static <R, E extends Exception> R uncheck(ExceptionalSupplier<R, E> supplier) {
    try {
      return supplier.get();
    } catch (Exception exception) {
      throwAsUnchecked(exception);
      return null;
    }
  }

  /**
   * uncheck(Class::forName, "xxx");
   */
  public static <T, R, E extends Exception> R uncheck(ExceptionalFunction<T, R, E> function, T t) {
    try {
      return function.apply(t);
    } catch (Exception exception) {
      throwAsUnchecked(exception);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
    throw (E) exception;
  }

}
