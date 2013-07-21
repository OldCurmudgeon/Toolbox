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

package com.oldcurmudgeon.toolbox;

import java.util.Arrays;

/**
 * @author OldCurmudgeon
 */
public class Objects {

  // Also in Rebox.
  public static <T> T[] newArray(int length, T... empty) {
    return Arrays.copyOfRange(empty, 0, length);
  }
  
  public static void main(String[] args) {
    String[] strings = Objects.<String>newArray(5);
    System.out.println(Arrays.asList(strings));
  }
  
}
