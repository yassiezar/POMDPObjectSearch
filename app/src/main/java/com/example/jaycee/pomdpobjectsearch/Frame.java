package com.example.jaycee.pomdpobjectsearch;

import android.media.Image;
import android.util.Log;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Frame
{
    private static final String TAG = Frame.class.getSimpleName();

    private static Frame frame = new Frame();

    private static final Lock lock = new ReentrantLock();

    private com.google.ar.core.Frame arFrame;
    private Image image;

    public Frame() {}

    public static Frame getFrame() { return frame; }
    public Lock getLock() { return lock; }
    public com.google.ar.core.Frame getArFrame() { return arFrame; }
    public Image getImage() { return this.image; }
    public void updateFrame(com.google.ar.core.Frame frame)
    {
        arFrame = frame;
        try
        {
            if(image != null)
            {
                image.close();
            }
            image = frame.acquireCameraImage();
        }
        catch(NotYetAvailableException e)
        {
            Log.e(TAG, "Camera not available: " + e);
        }
    }
}
