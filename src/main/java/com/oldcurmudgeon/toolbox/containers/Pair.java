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

/**
 * @author OldCurmudgeon
 */
public class Pair<P,Q> {
  private final P p;
  private final Q q;
  
  public Pair( P p, Q q ) {
    this.p = p;
    this.q = q;
  }
  
  public P getP () {
    return p;
  }

  public Q getQ () {
    return q;
  }

  @Override
  public String toString () {
    return "{"+p+","+q+"}";
  }
}
