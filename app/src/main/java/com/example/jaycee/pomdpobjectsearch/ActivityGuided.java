package com.example.jaycee.pomdpobjectsearch;

import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotTrackingException;

public class ActivityGuided extends ActivityBase implements NewWaypointHandler
{
    private static final String TAG = ActivityGuided.class.getSimpleName();

    private SoundGenerator soundGenerator;

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }

        soundGenerator = new SoundGenerator(this);
        soundGenerator.setSession(session);
        soundGenerator.run();
    }

    @Override
    protected void onPause()
    {
        if(soundGenerator != null)
        {
            soundGenerator.stop();
            soundGenerator = null;
        }

        if(!JNIBridge.killSound())
        {
            Log.e(TAG, "OpenAL kill error");
        }

        super.onPause();
    }


    public Anchor getWaypointAnchor()
    {
        /* TODO: Handle nullpointer crash here */
        return soundGenerator.getWaypointAnchor();
    }

    @Override
    public void onNewTimestamp(long timestamp)
    {
        soundGenerator.setTimestamp(timestamp);
    }

    @Override
    public void onTargetFound()
    {
        surfaceView.getRenderer().setDrawWaypoint(false);
    }

    @Override
    public void onNewWaypoint()
    {
        surfaceView.getRenderer().setDrawWaypoint(true);
    }

    @Override
    public void onNewFrame(final Frame frame)
    {
        if(soundGenerator == null)
        {
            Log.w(TAG, "Sound Generator is dead. Likely that the thread has been killed");
            return;
        }
        soundGenerator.setFrame(frame);

        if(!soundGenerator.isTargetSet())
        {
            Log.w(TAG, "Sound Generator target not set");
            return;
        }

        super.onNewFrame(frame);
    }

    public void setTarget(int target)
    {
        try
        {
            soundGenerator.setTarget(target);
        }
        catch(NotTrackingException e)
        {
            Log.e(TAG, "Not tracking: " + e);
            Toast.makeText(ActivityGuided.this, "Camera not tracking", Toast.LENGTH_LONG).show();
        }
    }
}
