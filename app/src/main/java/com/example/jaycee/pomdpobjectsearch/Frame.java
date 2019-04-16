package com.example.jaycee.pomdpobjectsearch;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Frame
{
    private static Frame frame = new Frame();

    private static final Lock lock = new ReentrantLock();

    private com.google.ar.core.Frame arFrame;

    public Frame() {}

    public static Frame getFrame() { return frame; }
    public Lock getLock() { return lock; }
    public void setFrame(com.google.ar.core.Frame frame) { arFrame = frame; }
    public com.google.ar.core.Frame getArFrame() { return arFrame; }
}
