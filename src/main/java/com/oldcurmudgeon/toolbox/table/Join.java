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
package com.oldcurmudgeon.toolbox.table;

import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.util.Set;

/**
 *
 * @author OldCurmudgeon
 */
class Join<Column extends Enum<Column> & Table.Columns> {

   Set<Column> key;
   Table with;

   public Join(Table with, Set<Column> key) {
      this.with = with;
      this.key = key;
   }

   public String join(String alias) {
      String s = "LEFT JOIN " + with.tableName + " " + with.alias + " ON ";
      Separator and = new Separator(" AND ");
      for (Column c : key) {
         s += and.sep() + alias + "." + c.name() + " = " + with.alias + "." + c.name();
      }
      return s;
   }
}
