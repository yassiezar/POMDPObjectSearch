package com.example.jaycee.pomdpobjectsearch.rendering;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.CameraSurface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = CameraRenderer.class.getSimpleName();

    private long glContext;
    private CameraSurface surfaceView;

    public CameraRenderer(GLSurfaceView surfaceView)
    {
        this.surfaceView = (CameraSurface)surfaceView;

        Log.i(TAG, "Renderer init");
        nativeCreateRenderer();
    }

    public void destroyRenderer() { nativeDestroyRenderer(); }

    public void drawFrame(byte[] frame, int width, int height, int rotation)
    {
        nativeDrawFrame(frame, width, height, rotation);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) { }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        nativeInitRenderer(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        nativeRenderFrame();
    }

    // GLRenderer
    private native void nativeCreateRenderer();
    private native void nativeDestroyRenderer();
    private native void nativeInitRenderer(int width, int height);
    private native void nativeRenderFrame();
    private native void nativeDrawFrame(byte[] data, int width, int height, int rotation);

    static
    {
        System.loadLibrary("render");
    }
}
