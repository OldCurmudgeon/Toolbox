package com.oldcurmudgeon.toolbox.pipe;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Pipe is a source/sink of data.
 *
 * @author OldCurmudgeon
 * @param <T>
 */
public class Pipe<T> implements Input<T>, Output<T> {

  // Holding area for the things.
  private BlockingQueue<T> q = new ArrayBlockingQueue(1);

  /**
   * Consumer should take as little time as possible.
   */
  private Consumer<T> consumer = Exceptions.rethrowConsumer(t -> q.add(t));

  /**
   * Producer obtains the result - performing calculations as necessary.
   */
  private Supplier<T> producer = Exceptions.rethrowSupplier(() -> q.remove());

  /**
   * Closed for writing - throws an exception if you try to write to a joined pipe.
   *
   * Please remember to only write to the first pipe.
   */
  private static final Consumer CLOSED = Exceptions.rethrowConsumer(t -> {
    throw new PipeException("Attempt to write to a joined pipe.");
  });

  /**
   * Simple pipe.
   */
  public Pipe() {
  }

  /**
   * Special producer/consumer/functions set.
   */
  private Pipe(Supplier<T> producer,
          Consumer<T> consumer,
          List<String> functions,
          List<List<String>> otherFunctions) {
    this.producer = producer;
    this.consumer = consumer;
    this.previousFunctions.addAll(otherFunctions);
    this.previousFunctions.add(functions);
  }

  /**
   * Join me onto an existing pipe of my same type.
   *
   * @param pipe - The pipe to feed off.
   */
  private Pipe(Pipe<T> pipe) {
    // Chain from it. Close my consumer.
    this(() -> pipe.get(), CLOSED, pipe.functions, pipe.previousFunctions);
  }

  /**
   * Join me onto an existing pipe of a different type.
   *
   * @param <S> - The new type of this pipe.
   * @param pipe - The pipe to feed off.
   * @param map - How to convert between
   */
  private <S> Pipe(Pipe<S> pipe, Function<S, T> map) {
    // Chain from it using the map. Close my consumer.
    this(() -> map.apply(pipe.get()), CLOSED, pipe.functions, pipe.previousFunctions);
  }

  /**
   * Push some data into the pipe.
   *
   * @param datum - What should go down the pipe.
   * @return - the pipe - for chaining purposes.
   */
  @Override
  public Pipe<T> put(T datum) {
    // Consume it.
    consumer.accept(datum);
    return this;
  }

  /**
   * Pull whatever is available out of the pipe.
   *
   * Does nothing by default. Add flows and filters to change that.
   *
   * @return - The received data.
   */
  @Override
  public T get() {
    // Get it out of the buffer and perform the function on it.
    return function.apply(producer.get());
  }

  // Initially do nothing.
  private Function<T, T> function = i -> i;

  /**
   * Add another function to the chain.
   *
   * @param function - What more to do.
   * @param name - Descriptive for it.
   * @return - this.
   */
  public Pipe<T> addFunction(Function<T, T> function, String name) {
    // Join them.
    this.function = this.function.andThen(function);
    // Track the name.
    functions.add(name);
    // Allow for chaining.
    return this;
  }

  /**
   * Join a new pipe on the end of me.
   *
   * Used by `join` so private.
   *
   * @param <U> - The new pipe type.
   * @param map
   * @return
   */
  public <U> Pipe<U> join(Function<T, U> map) {
    // Join to the old pipe using the map.
    return new Pipe<>(this, map);
  }

  /**
   * Join a new pipe on the end of me.
   *
   * @return
   */
  public Pipe<T> join() {
    // Join directly to the old pipe.
    return new Pipe<>(this);
  }

  /**
   * Split the pipe.
   *
   * @return
   */
  public Pipe<T> split() {
    // default to split into 2.
    return split(2);
  }

  public Pipe<T> split(int howMany) {
    // Join directly to the old pipe.
    return new Pipe<>(this);
  }

  /**
   * Used by toString.
   *
   * Keep track of a descriptive name for me.
   *
   * Does not do very well ATM - changes to fed pipe is not reflected here.
   */
  private List<String> functions = new ArrayList();
  private List<List<String>> previousFunctions = new ArrayList();

  @Override
  public String toString() {
    // Build dynamically so changes up-stream are reflected.
    return (previousFunctions.isEmpty() ? "" : previousFunctions + " & ")
            + (functions.isEmpty() ? "" : functions);
  }

  /**
   * The contracts I must adhere to.
   */
  private final Set<Contract> contracts = EnumSet.noneOf(Contract.class);

  /**
   * Makes demands of a pipe.
   *
   * Configures the pipe to have specific features.
   *
   * @param features - List of features to demand.
   * @return - the pipe - for chaining purposes.
   */
  public Pipe<T> demand(Iterable<Feature> features) {
    for (Feature f : features) {
      contracts.addAll(f.getContracts());
    }
    // Allow for chaining.
    return this;

  }

  // !!!! TESTING !!!!
  public void test() {
    System.out.println("--------");
    test1();
    test2();
    test3();
    test4();
    System.out.println("--------");
    //Stream.of(1,2,3).map(mapper)
  }

  private void test4() {
    /**
     * Failures.
     */
    System.out.println("-- 04 -- \"Failing\" pipe.");
    // Make a pipe to Math.cbrt (cube root).
    Pipe<Double> cbrt = new Pipe<Double>()
            // Cube root
            .addFunction(Math::cbrt, "cbrt");
    // Joint it to stringify.
    Pipe<String> strings = new Pipe<>(cbrt, (d) -> Double.toString(d));
    // Should fail because cbrt feeds strings.
    try {
      strings.put("Ooops!");
    } catch (Exception e) {
      System.out.println("Bad write to joined pipe - " + e);
    }
  }

  private void test3() {
    /**
     * Add some joints.
     */
    System.out.println("-- 03 -- \"Joint\" pipe.");
    // Make a pipe to Math.cbrt (cube root).
    Pipe<Double> cbrt = new Pipe<Double>()
            // Cube root
            .addFunction(Math::cbrt, "cbrt");
    // What does it look like?
    cbrt.put(Math.PI);
    System.out.println("Pipe: " + cbrt + " = " + cbrt.get());
    // Join to a String pipe.
    Pipe<String> strings = cbrt.join((d) -> Double.toString(d))
            .addFunction((s) -> s.replace('0', '1'), "0 -> 1")
            .addFunction((s) -> s.replace('1', '_'), "1 -> _")
            .addFunction((s) -> s.replace('.', '|'), ". -> |");
    // Pull out the result.
    cbrt.put(Math.PI);
    System.out.println("Pipe: " + strings + " joined " + strings.get());
    // Add some more functions.
    cbrt.addFunction((d) -> d + 1, "+ 1");
    strings.addFunction((s) -> s.replace('5', '*'), "5 -> *");
    cbrt.put(Math.PI);
    System.out.println("Pipe: " + strings + " joined " + strings.get());
  }

  private void test2() {
    /**
     * Add some math.
     */
    System.out.println("-- 02 -- \"Math\" pipe.");
    // Make a pipe to Math.cbrt (cube root).
    Pipe<Double> eToTheCubeRootOfPiMinusOne = new Pipe<Double>()
            // Cube root
            .addFunction(Math::cbrt, "cbrt")
            // e^x - 1
            .addFunction(Math::expm1, "expmt1");
    // Hack the pipe to call Math.stuff.
    // Pull out the result.
    Double got = eToTheCubeRootOfPiMinusOne.put(Math.PI).get();
    System.out.println("expected " + (Math.expm1(Math.cbrt(Math.PI))));
    System.out.println("Pipe: " + eToTheCubeRootOfPiMinusOne + " got      " + got);
  }

  private void test1() {
    /**
     * To start with a Pipe just does nothing, returns the put item untouched.
     */
    System.out.println("-- 01 -- \"Hello\" pipe.");
    Pipe<String> pipe = new Pipe<>();
    // Push anything at it and get it back.
    System.out.println("Pipe: " + pipe + " Got: " + pipe.put("Hello").get());
  }

  public static void main(String args[]) {
    //<editor-fold defaultstate="collapsed" desc="test">
    try {
      new Pipe<String>().test();
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
    //</editor-fold>
  }

}

class PipeException extends RuntimeException {

  PipeException(String msg) {
    super(msg);
  }

  PipeException(String msg, Throwable cause) {
    super(msg, cause);
  }

  PipeException(Throwable cause) {
    super(cause);
  }
}
