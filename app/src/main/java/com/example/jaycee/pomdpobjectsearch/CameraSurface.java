package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.ar.core.Session;

public class CameraSurface extends GLSurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = CameraSurface.class.getSimpleName();

    public interface ScreenReadRequest
    {
        void onScreenTap();
    }

    private Context context;

    private ScreenReadRequest screenReadRequest;

    private Session session;

    private SurfaceRenderer renderer;

    private boolean screenTapEnabled = false;

    public CameraSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;

        renderer = new SurfaceRenderer(context, this);

        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void setScreenReadRequest(Context context)
    {
        this.screenReadRequest = (ScreenReadRequest)context;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        super.surfaceCreated(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
        super.surfaceChanged(surfaceHolder, format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        super.surfaceDestroyed(surfaceHolder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        final int action = event.getAction();

        if(!screenTapEnabled)
        {
            return false;
        }

        switch(action)
        {
            case (MotionEvent.ACTION_DOWN):
            {
                performClick();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        // Read out object in view
        screenReadRequest.onScreenTap();
        return true;
    }

    public void enableScreenTap()
    {
        this.screenTapEnabled = true;
    }

    public SurfaceRenderer getRenderer()
    {
        return renderer;
    }

    public void setSession(Session session) { this.session = session; }
    public Session getSession() { return this.session; }
}
