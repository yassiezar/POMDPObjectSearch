package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotTrackingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ActivityGuided extends ActivityBase implements NewWaypointHandler
{
    private static final String TAG = ActivityGuided.class.getSimpleName();

    private static final float MIN_CONF = 0.2f;

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

        scanFrameForObjects();
    }

    @Override
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

    @Override
    public void onScanComplete(List<ObjectClassifier.Recognition> results)
    {
        RectF centreOfScreen = new RectF(213, 160, 416, 320);
        List<ObjectClassifier.Recognition> validObservations = new ArrayList<>();
        for(ObjectClassifier.Recognition result : results)
        {
            if(result.getConfidence() > MIN_CONF && centreOfScreen.contains(result.getLocation()))
            {
                validObservations.add(result);
                Log.d(TAG, result.toString());
            }
        }

        if(!validObservations.isEmpty())
        {
            Collections.sort(validObservations, new Comparator<ObjectClassifier.Recognition>()
            {
                @Override
                public int compare(ObjectClassifier.Recognition r1, ObjectClassifier.Recognition r2)
                {
                    return r1.getConfidence() > r2.getConfidence() ? 1 : r1.getConfidence() < r2.getConfidence() ? -1 : 0;
                }
            });
            soundGenerator.setObservation(validObservations.get(0).getCode());
        }
        else
        {
            soundGenerator.setObservation(0);
        }
    }
}
