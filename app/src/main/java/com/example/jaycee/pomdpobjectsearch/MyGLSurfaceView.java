package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.List;

public class MyGLSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = MyGLSurfaceView.class.getSimpleName();

    private Camera camera;

    public MyGLSurfaceView(Context context)
    {
        super(context);
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            camera = Camera.open();
            camera.setPreviewDisplay(holder);
        }
        catch(Exception e)
        {
            camera.release();
            camera = null;
            Log.e(TAG, "failed to open Camera: "+ e);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (camera != null)
        {
            // Call stopPreview() to stop updating the preview surface.
            camera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if(camera != null)
        {
            Camera.Parameters parameters = camera.getParameters();

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            requestLayout();

            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);

            camera.startPreview();
        }
    }
}
