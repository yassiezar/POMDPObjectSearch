package com.example.jaycee.pomdpobjectsearch

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_camera.*

class ActivityCamera : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Example of a call to a native method
        sample_text.text = stringFromJNI()
    }

    external fun stringFromJNI(): String

    companion object
    {
        init
        {
            System.loadLibrary("native-lib")
        }
    }
}
