package com.oldcurmudgeon.toolbox.pipe;

/**
 * Interface to be passed to pipe writers.
 *
 * @author OldCurmudgeon
 */
public interface Output<T> {

    public Pipe<T> put(T datum);
}
