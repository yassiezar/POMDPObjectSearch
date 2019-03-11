package com.example.jaycee.pomdpobjectsearch;

public class JNIBridge
{
    static
    {
        System.loadLibrary("JNI");
    }

    public static native boolean initSound();
    public static native boolean killSound();
    public static native void playSound_FFFF(float[] src, float[] list, float gain, float pitch);
    public static native void playSound_FF(float gain, float pitch);
}
