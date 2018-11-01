package com.example.jaycee.pomdpobjectsearch;

public class JNIBridge
{
    static
    {
        System.loadLibrary("JNI");
    }

    public static native boolean initSearch(long target, long horizon);
    public static native long getAction(long state);

    public static native boolean initSound();
    public static native boolean killSound();
    public static native void playSound(float[] src, float[] list, float gain, float pitch);
    public static native boolean stopSound();

    // Object detectors
    public static native void create(String cfg_file, String weights_file, float conf_thr);
    public static native float[] classify(long input_frame);

    // GLRenderer
     public native static void createGLRenderer();
     public native static void destroyGLRenderer();
     public native static void initGLRenderer(int width, int height);
     public native static void renderFrame();
     public native static void drawFrame(byte[] data, int width, int height, int rotation);
     public native static void applyFilter(int filter);
     public native static int getMaxFilter();
}
