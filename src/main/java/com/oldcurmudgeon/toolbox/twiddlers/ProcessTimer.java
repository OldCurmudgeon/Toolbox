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
package com.oldcurmudgeon.toolbox.twiddlers;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>Title: Import Prices</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author OldCurmudgeon
 * @version 1.0
 */
public class ProcessTimer {
  // A zero-based time zone for time differences.

  private static final TimeZone zz = TimeZone.getTimeZone("GMT+00");
  // Format for the time difference display.
  private static final DateFormat secondsFormat = new SimpleDateFormat("ss.SSS", Locale.UK);
  private static final DateFormat minutesFormat = new SimpleDateFormat("mm:ss.SSS", Locale.UK);
  private static final DateFormat hoursFormat = new SimpleDateFormat("hh:mm:ss.SSS", Locale.UK);
  // Creation of this object magically takes a note of the current time.
  private long start = System.currentTimeMillis();
  // Zero means not stopped.
  private long stopped = 0;

  /**
   * now
   *
   * @return Date
   */
  public long elapsed() {
    // What's the time now.
    if ( stopped == 0 ) {
      stop ();
    }
    return stopped;
  }
  private static DecimalFormat perSecondFormat = new DecimalFormat("#.###");

  public String perSecond(long n) {
    return perSecondFormat.format(((float) n) / ((float) elapsed()) * 1000.0);
  }
  
  // Stop now.
  public void stop() {
    stopped = System.currentTimeMillis() - start;
  }

  /**
   * format
   *
   * @param elapsed Date
   * @return String
   */
  public String format(long elapsed) {
    Calendar c = new GregorianCalendar(zz);

    c.setTimeInMillis(elapsed);
    String s;
    // Not necessary in 5.0.
    Date d = new Date(elapsed);
    if (c.get(Calendar.HOUR) > 0) {
      s = hoursFormat.format(d);
    } else if (c.get(Calendar.MINUTE) > 0) {
      s = minutesFormat.format(d);
    } else {
      s = secondsFormat.format(d);
    }
    return s;
  }

  /**
   * toString
   *
   * @return String
   */
  @Override
  public String toString() {
    return format(elapsed());
  }
}
