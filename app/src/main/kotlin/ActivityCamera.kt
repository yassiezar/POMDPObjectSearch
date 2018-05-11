package com.example.jaycee.pomdpobjectsearch

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class ActivityCamera : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        JNIBridge().init()
    }
}
