package com.example.jaycee.pomdpobjectsearch.rendering;

import android.opengl.GLSurfaceView;

import com.example.jaycee.pomdpobjectsearch.CameraSurface;
import com.example.jaycee.pomdpobjectsearch.JNIBridge;

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

        JNIBridge.createGLRenderer();
    }

    public void requestRender()
    {
        if(surfaceView != null && surfaceView.getRenderMode() == GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        {
            surfaceView.requestRender();
        }
    }

    public void destroyRenderer() { JNIBridge.destroyGLRenderer(); }

    public void drawFrame(byte[] frame, int width, int height, int rotation)
    {
        JNIBridge.drawFrame(frame, width, height, rotation);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1)
    {

    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        JNIBridge.renderFrame();
    }
}
