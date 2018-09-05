package com.example.jaycee.pomdpobjectsearch.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.WindowManager;

import com.example.jaycee.pomdpobjectsearch.CameraSurface;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = SurfaceRenderer.class.getSimpleName();

    private Context context;
    private CameraSurface surfaceView;

    private BackgroundRenderer backgroundRenderer;
    private ObjectRenderer objectRenderer;

    private boolean viewportChanged = false;

    private int width, height;

    private int scannerWidth, scannerHeight;
    private int scannerX, scannerY;

    public SurfaceRenderer(Context context, CameraSurface surfaceView)
    {
        this.context = context;
        this.surfaceView = surfaceView;

        this.scannerWidth = 525;
        this.scannerHeight = 525;
        this.scannerX = 450;
        this.scannerY = 1017;

        init();
    }

    public void init()
    {
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        backgroundRenderer = new BackgroundRenderer(scannerX, scannerY, scannerWidth, scannerHeight);
        objectRenderer = new ObjectRenderer();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Log.i(TAG, "Surface created");
        try
        {
            backgroundRenderer.createOnGlThread(context);
            // objectRenderer.createOnGlThread(context, "models/arrow/Arrow.obj", "models/arrow/Arrow_S.tga");
            // objectRenderer.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            // objectRenderer.setMaterialProperties(0.f, 2.f, 0.5f, 6.f);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Failed to read asset file. ", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        Log.i(TAG, "Surface changed");
        viewportChanged = true;
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Session session = surfaceView.getSession();
        if(session == null)
        {
            Log.w(TAG, "No session available for draw.");
            return;
        }
        if(viewportChanged)
        {
            try
            {
                viewportChanged = false;
                int displayRotation = context.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
                session.setDisplayGeometry(displayRotation, width, height);
            }
            catch(NullPointerException e)
            {
                Log.e(TAG, "Default display exception: " + e);
            }
        }

        session.setCameraTextureName(backgroundRenderer.getTextureId());
        try
        {
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            backgroundRenderer.draw(frame);
        }
        catch(CameraNotAvailableException e)
        {
            Log.e(TAG, "Camera not available: " + e);
        }
        catch(Throwable t)
        {
            Log.e(TAG, "Exception on the GL Thread: " + t);
        }
    }

    public IntBuffer getCurrentFrameBuffer()
    {
        try
        {
            return backgroundRenderer.getCurrentFrameBuffer().duplicate();
        }
        catch(NullPointerException e)
        {
            Log.e(TAG, "Frame buffer not yet initialised: " + e);
            // return IntBuffer.allocate(480*480);
            return IntBuffer.allocate(scannerWidth*scannerHeight);
        }
    }

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
}
