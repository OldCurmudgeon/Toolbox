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
package com.oldcurmudgeon.toolbox.meta;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import java.io.FileInputStream;
import java.util.List;

/**
 * @author OldCurmudgeon
 */
public class Parse {
  public static void main(String[] args) throws Exception {
    // Create an input stream for the file to be parsed
    FileInputStream in = new FileInputStream("C:\\Documents and Settings\\user\\My Documents\\Junk.java");

    CompilationUnit cu;
    try {
      // parse the file
      cu = JavaParser.parse(in);
    } finally {
      in.close();
    }

    // change the methods names and parameters
    changeMethods(cu);

    // prints the changed compilation unit
    System.out.println(cu.toString());
  }

  private static void changeMethods(CompilationUnit cu) {
    List<TypeDeclaration> types = cu.getTypes();
    for (TypeDeclaration type : types) {
      System.out.println("Type: "+type);
      List<BodyDeclaration> members = type.getMembers();
      for (BodyDeclaration member : members) {
        System.out.println("Member: "+member);
        if (member instanceof MethodDeclaration) {
          MethodDeclaration method = (MethodDeclaration) member;
          changeMethod(method);
        }
      }
    }
  }

  private static void changeMethod(MethodDeclaration n) {
    // change the name of the method to upper case
    n.setName(n.getName().toUpperCase());

    // create the new parameter
    Parameter newArg = ASTHelper.createParameter(ASTHelper.INT_TYPE, "value");

    // add the parameter to the method
    ASTHelper.addParameter(n, newArg);
  }
}
