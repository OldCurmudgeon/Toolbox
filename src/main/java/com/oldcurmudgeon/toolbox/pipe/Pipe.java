package com.oldcurmudgeon.toolbox.pipe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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

  interface Holder<T> {

    public T get();

    public void put(T it);
  }

  static class HoldBlockingQueue<T> implements Holder<T> {

    // Holding area for the things.
    private BlockingQueue<T> q = new ArrayBlockingQueue(1);

    /**
     * Consumer should take as little time as possible.
     *
     * Just push it into the queue.
     */
    private Consumer<T> consumer = Exceptions.rethrowConsumer(q::add);

    /**
     * Producer obtains the result - performing calculations as necessary.
     *
     * Pull it from the queue.
     */
    private Supplier<T> producer = Exceptions.rethrowSupplier(q::remove);

    /**
     * Transform Holder functions to BlockingQueue functions.
     */
    @Override
    public T get() {
      return producer.get();
    }

    @Override
    public void put(T it) {
      consumer.accept(it);
    }

    public static <T> Holder<T> make(Pipe p) {
      // Ignore the pipe.
      return new HoldBlockingQueue<>();
    }
  }

  static class HoldAtomic<T> implements Holder<T> {

    // Holding area for the things.
    private AtomicReference<T> r = new AtomicReference<>(null);

    /**
     * Stick it into the Atomic.
     */
    private Consumer<T> consumer = r::set;

    /**
     * Pull it out.
     */
    private Supplier<T> producer = r::get;

    /**
     * Transform Holder functions to BlockingQueue functions.
     */
    @Override
    public T get() {
      return producer.get();
    }

    @Override
    public void put(T it) {
      consumer.accept(it);
    }

    public static <T> Holder<T> make(Pipe p) {
      // Ignore the pipe.
      return new HoldAtomic<>();
    }
  }

  static class HoldDelegate<T> implements Holder<T> {

    /**
     * Pass it on.
     */
    private Consumer<T> consumer;

    /**
     * Pull it out.
     */
    private Supplier<T> producer;

    public HoldDelegate(Pipe<T> joinTo) {
      // Delegate to the joined pipe.
      consumer = joinTo::put;
      producer = joinTo::get;
    }

    /**
     * Delegate all.
     */
    @Override
    public T get() {
      return producer.get();
    }

    @Override
    public void put(T it) {
      consumer.accept(it);
    }

    public static <T> Holder<T> make(Pipe<T> p) {
      // Ignore the pipe.
      return new HoldDelegate<>(p);
    }
  }

  /**
   * Pick a holding strategy to use.
   *
   * Each is a Holder factory
   */
  enum HoldingStrategy {
    /**
     * Hold nothing.
     */
    None(new Function<Pipe, Holder>() {
      // We are stateless so we only need one supplier.
      final Holder NOTHING = new Holder() {
        /**
         * Always give null.
         */
        @Override
        public Object get() {
          return null;
        }

        /**
         * Black hole.
         */
        @Override
        public void put(Object it) {
        }

      };

      /**
       * Give them a Holder that uses this strategy.
       */
      @Override
      public Holder apply(Pipe t) {
        // Always give them the same one.
        return NOTHING;
      }
    }),
    /**
     * Writing overwrites the previous.
     *
     * Reading reads the current value.
     */
    PassThrough(HoldAtomic::make),
    /**
     * Writing blocks until space available.
     *
     * Reading blocks until something is written.
     */
    Block(HoldBlockingQueue::make),
    /**
     * Forward all requests up the chain.
     */
    Delegate(HoldDelegate::make);

    /**
     * Keeps track of the maker that makes the Holders.
     */
    final Function<Pipe, Holder> maker;

    public <T> Holder<T> holder() {
      return maker.<T>apply(NOPIPE);
    }

    public <T> Holder<T> holder(Pipe<T> from) {
      return maker.<T>apply(from);
    }

    HoldingStrategy(Function<Pipe, Holder> maker) {
      this.maker = maker;
    }
  }

  // A null pipe
  private static final Pipe NOPIPE = new Pipe();
  /**
   * What to do to the data at pull time.
   *
   * Initially do nothing.
   */
  private Function<T, T> function = i -> i;
  /**
   * I must have a Holder to hold a T while in transit.
   */
  final Holder<T> it;

  /**
   * Use a strategy.
   */
  private Pipe(HoldingStrategy strategy,
          List<String> functions,
          List<List<String>> otherFunctions) {
    // Tell the strategy to make the holder.
    it = strategy.holder();
    this.previousFunctions.addAll(otherFunctions);
    this.previousFunctions.add(functions);
  }

  private Pipe(HoldingStrategy strategy) {
    this(strategy, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
  }

  /**
   * Default to no holding at all.
   */
  public Pipe() {
    // Default to None.
    this(HoldingStrategy.None);
  }

  /**
   * Join me onto the end of an existing pipe of my same type.
   *
   * @param pipe - The pipe to feed off.
   */
  private Pipe(Pipe<T> pipe) {
    // Chain from it.
    this(HoldingStrategy.PassThrough, pipe.functions, pipe.previousFunctions);
    // TODO - Close the put pipe. this(() -> pipe.get(), CLOSED, );
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
    it.put(datum);
    return this;
  }

  /**
   * Pull whatever is available out of the pipe.
   *
   * @return - The received data.
   */
  @Override
  public T get() {
    // Get it out of the buffer and perform the function on it.
    return function.apply(it.get());
  }

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
   * Add another function to the chain.
   *
   * @param function - What more to do.
   * @param c
   * @param name - Descriptive for it.
   * @return - this.
   */
  public Pipe<T> addFunction(BiFunction<T, T, T> function, T c, String name) {
    // Wrap c in a lambda.
    return addFunction((T t) -> function.apply(t, c), name);
  }

  /**
   * Join a new pipe on the end of me.
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

  /**
   * Utility Math functions.
   */
  public static class Math {

    public static final BigDecimal PI = BigDecimal.valueOf(java.lang.Math.PI);

    public static BigInteger mul(BigInteger a, BigInteger b) {
      return a.multiply(b);
    }

    public static BigInteger div(BigInteger a, BigInteger b) {
      return a.divide(b);
    }

    public static BigInteger add(BigInteger a, BigInteger b) {
      return a.add(b);
    }

    public static BigInteger sub(BigInteger a, BigInteger b) {
      return a.subtract(b);
    }

    public static BigInteger pow(BigInteger a, BigInteger b) {
      return a.pow(b.intValue());
    }

  }
  /**
   * Closed for writing - throws an exception if you try to write to a joined pipe.
   *
   * Please remember to only write to the first pipe.
   *
   * This is attached to the consumer of the downstream pipe when it is added to an existing one.
   */
  private static final Consumer CLOSED = Exceptions.rethrowConsumer(t -> {
    throw new PipeException("Attempt to write to a joined pipe.");
  });

  // !!!! TESTING !!!!
  public void test() {
    System.out.println("--------");
    test1();
    test2();
    test3();
    test4();
    test5();
    System.out.println("--------");
    //Stream.of(1,2,3).map(mapper)
  }

  private void test5() {
    System.out.println("-- 05 -- \"Times Table\" pipe.");
    Pipe<BigInteger> timesTable[] = new Pipe[10];
    for (int i = 0; i < timesTable.length; i++) {
      final int c = i;
      timesTable[i] = new Pipe<BigInteger>()
              .addFunction(Math::mul, BigInteger.valueOf(i), "Times " + i);
    }
    for (int i = 0; i < timesTable.length; i++) {
      timesTable[i].put(BigInteger.TEN);
      System.out.println("t(" + i + ") = " + timesTable[i].get());
    }
  }

  private void test4() {
    /**
     * Failures.
     */
    System.out.println("-- 04 -- \"Failing\" pipe.");
    // Make a pipe to Math.cbrt (cube root).
    Pipe<BigInteger> cbrt = new Pipe<BigInteger>()
            // Cube root
            .addFunction(BigInteger::abs, "cbrt");
    // Joint it to stringify.
    Pipe<String> strings = new Pipe<>(cbrt, BigInteger::toString);
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
    Pipe<BigInteger> cbrt = new Pipe<BigInteger>()
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
            .addFunction(java.lang.Math::expm1, "expmt1");
    // Hack the pipe to call Math.stuff.
    // Pull out the result.
    Double got = eToTheCubeRootOfPiMinusOne.put(Math.PI).get();
    System.out.println("expected " + (java.lang.Math.expm1(java.lang.Math.cbrt(Math.PI))));
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
