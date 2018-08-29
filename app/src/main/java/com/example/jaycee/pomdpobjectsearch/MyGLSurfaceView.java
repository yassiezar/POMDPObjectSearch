package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class MyGLSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback
{
    public MyGLSurfaceView(Context context)
    {
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
}
