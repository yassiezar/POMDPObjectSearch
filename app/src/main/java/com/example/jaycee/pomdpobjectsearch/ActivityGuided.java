package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.imageprocessing.ObjectClassifier;
import com.example.jaycee.pomdpobjectsearch.views.Arrow;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.jaycee.pomdpobjectsearch.Objects.Observation.O_NOTHING;
import static com.example.jaycee.pomdpobjectsearch.helpers.VectorTools.CameraVector.getCameraVectorPanAndTilt;

public class ActivityGuided extends ActivityBase
{
    private static final String TAG = ActivityGuided.class.getSimpleName();

    private static final float MIN_CONF = 0.2f;

    private SoundGenerator soundGenerator;

    private Vibrator vibrator;

    private WaypointProvider waypointProvider;
    private ActionGenerator actionGenerator;

    private Objects.Observation observation;

    @Override
    protected void onResume()
    {
        super.onResume();

        this.vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);

        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }

        soundGenerator = SoundGenerator.create(this);
        waypointProvider = new WaypointProvider();
        actionGenerator = ActionGenerator.create(this);
    }

    @Override
    protected void onPause()
    {
        if(vibrator != null)
        {
            vibrator.cancel();
        }

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


    public Pose getWaypointPose()
    {
        return waypointProvider.getWaypointPose();
    }

    @Override
    public void onNewFrame(final Frame frame)
    {
        if(soundGenerator == null)
        {
            Log.w(TAG, "Sound Generator is dead. Likely that the thread has been killed");
            return;
        }

        super.onNewFrame(frame);

        if(getTarget() == O_NOTHING)
        {
            Log.d(TAG, "Target not set");
            return;
        }

        scanFrameForObjects();
        if(observation != null)
        {
            try
            {
                soundGenerator.getLock().lock();

                Pose phonePose = frame.getAndroidSensorPose();
                soundGenerator.setPhonePose(phonePose);
                VectorTools.PanAndTilt panAndTilt = getCameraVectorPanAndTilt(phonePose);
                float cameraPan = (float)panAndTilt.pan;
                float cameraTilt = (float)panAndTilt.tilt;

                if(waypointProvider.waypointReached(cameraPan, cameraTilt) ||
                        (waypointProvider.observation != observation && waypointProvider.observation != O_NOTHING) ||
                        waypointProvider.getWaypointPose() == null)
                {
                    Log.d(TAG, "setting new waypoint");
                    VectorTools.PanAndTilt newWaypointAngles = actionGenerator.getAngleAdjustment(observation, cameraPan, cameraTilt);
                    waypointProvider.updateWaypoint(newWaypointAngles, phonePose.getTranslation()[2]);
                    soundGenerator.setWaypointPose(waypointProvider.getWaypointPose());
                    surfaceView.getRenderer().setDrawWaypoint(true);
                }
            }
            finally
            {
                soundGenerator.getLock().unlock();
            }
        }
    }

    @Override
    public void setTarget(Objects.Observation target)
    {
        super.setTarget(target);
        Log.d(TAG, "Setting target");
        actionGenerator.setTarget(target);
        soundGenerator.setPhonePose(getFrame().getArFrame().getAndroidSensorPose());
        soundGenerator.start();
    }

    @Override
    public void onScanComplete(List<ObjectClassifier.Recognition> results)
    {
        RectF centreOfScreen = new RectF(100, 100, 200, 200);
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
            observation = validObservations.get(0).getObservation();
        }
        else
        {
            observation = O_NOTHING;
        }

        if(observation == getTarget())
        {
            Log.i(TAG, "Target found");

            setTarget(O_NOTHING);
            soundGenerator.stop();
            vibrator.vibrate(350);
            surfaceView.getRenderer().setDrawWaypoint(false);
            updateArrows(null);
        }
    }

    public void updateArrows(final Arrow.Direction direction)
    {
        Log.d(TAG, "Updating arrows");
        this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(direction == null)
                {
                    getCentreView().resetArrows();
                    return;
                }
                getCentreView().setArrowAlpha(direction, 255);
            }
        });
    }
}
