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
package com.oldcurmudgeon.toolbox.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author OldCurmudgeon
 */
public class TinyLogger {
  private final String logName;
  private boolean debug = false;
  private static PrintWriter log;
  private static final String LogFolder = "C:\\Logs\\";

  public TinyLogger(String name) {
    logName = name + ".log";
  }

  private boolean open() {
    if (debug && log == null) {
      // Try to get it there.
      new File(LogFolder).mkdirs();
      try {
        log = new PrintWriter(LogFolder + logName);
      } catch (FileNotFoundException ex) {
        // Ignore.
      }
    }
    return log != null;
  }

  public synchronized TinyLogger log(boolean toStdoutToo, String s) {
    if (open()) {
      log(s);
    }
    if (toStdoutToo) {
      System.out.println(s);
    }
    return this;
  }

  public synchronized TinyLogger log(String s) {
    if (open()) {
      log.println(s);
      log.flush();
    }
    return this;
  }

  public synchronized TinyLogger log(Throwable e) {
    log(true, "Exception: " + e.getMessage() + "\r\n" + stackTrace(e));
    return this;
  }

  public static String stackTrace(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  public TinyLogger reset() {
    if ( log != null ) {
      log.close();
      log = null;
    }
    new File(LogFolder + logName).delete();
    return this;
  }

  public TinyLogger setDebug(boolean debug) {
    this.debug = debug;
    return this;
  }
}
