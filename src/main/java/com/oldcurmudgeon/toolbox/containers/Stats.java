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
package com.oldcurmudgeon.toolbox.containers;

import com.oldcurmudgeon.toolbox.Objects;
import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hold statistics.
 *
 * @author OldCurmudgeon
 */
public class Stats {
  // A logger.
  private static final Logger log = LoggerFactory.getLogger("Stats");
  // A single statistic.
  public static class Stat<T> {
    protected T value;
    protected final String name;

    public Stat(String name) {
      this(name, null);
    }

    public Stat(String name, T value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String toString() {
      return name + "=" + Objects.asString(value, "null");
    }

    public void setValue(T value) {
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public T getValue() {
      return value;
    }

  }

  // A statistic holding a number.
  public static class NumberStat extends Stat<AtomicLong> {
    public NumberStat(String name, long value) {
      super(name, new AtomicLong(value));
    }

    public NumberStat(String name) {
      this(name, 0);
    }

    public long clr() {
      return value.getAndSet(0);
    }

    public long add(long n) {
      return value.addAndGet(n);
    }

    public long inc() {
      return add(1);
    }

  }

  // A statistic holding a total.
  public static class TotalStat extends NumberStat {
    private final NumberStat totalee;

    public TotalStat(NumberStat stat) {
      super("Total-" + stat.getName(), 0);
      totalee = stat;
    }

    @Override
    public long clr() {
      // We don't clear a total.
      return value.get();
    }

    @Override
    public long add(long n) {
      // No adding either.
      return value.get();
    }

    @Override
    public String toString() {
      // toString adds from the totalee and clears it.
      super.add(totalee.clr());
      return super.toString();
    }

  }
  // My stats.
  private final List<Stat> stats = new ArrayList<>();

  public Stats(Stat... stats) {
    this.stats.addAll(Arrays.asList(stats));
  }

  public void add(Stat add) {
    stats.add(add);
  }

  static class Gatherer extends TimerTask {
    private final Stats stats;
    private final Logger log;

    public Gatherer(long interval, Logger log, Stats stats) {
      this.log = log;
      this.stats = stats;
      // Make the timer a daemon.
      Timer timer = new Timer(true);
      // Wait one interval before starting.
      timer.scheduleAtFixedRate(this, interval, interval);
    }

    @Override
    public void run() {
      log.info(stats.toString());
    }

  }

  @Override
  public String toString() {
    return Separator.separate(",", stats);
  }

  public static void main(String args[]) {
    try {
      NumberStat one = new NumberStat("One");
      NumberStat two = new NumberStat("Two");
      Stats stats = new Stats(one, two);
      log.info("Stats: " + stats);
      one.inc();
      two.add(2);
      log.info("Stats: " + stats);
      Stats tStats = new Stats(one, two, new TotalStat(one), new TotalStat(two));
      Gatherer g = new Gatherer(1000, log, tStats );
      // Hang around for a while.
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() < start + 30000) {
        Thread.sleep(1000);
        one.inc();
        two.add(2);
      }
      log.info("Stats: " + stats);
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}
