package com.example.jaycee.pomdpobjectsearch

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class ActivityCamera : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        findViewById<Button>(R.id.button_object_toilet).setOnClickListener {
                JNIBridge().initSearch(1, 500)
            }
    }
}
