package com.example.jaycee.pomdpobjectsearch;

public class JNIBridge
{
    companion object
    {
        init
        {
            System.loadLibrary("JNI")
        }
    }

    external fun init() : Boolean
}
