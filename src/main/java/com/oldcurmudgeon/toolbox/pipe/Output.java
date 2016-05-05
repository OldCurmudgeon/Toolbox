/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.test.pipe;

/**
 *
 * @author pcaswell
 */
public interface Output<T> {

    public Pipe<T> put(T datum);
}
