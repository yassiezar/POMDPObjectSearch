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

    external fun initSearch(target: Long, horizon: Long) : Boolean
    external fun getAction(state: Long) : Long
}
