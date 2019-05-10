package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.imageprocessing.ObjectClassifier;
import com.example.jaycee.pomdpobjectsearch.policy.Model;
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

    private static final float MIN_CONF = 0.4f;

    private SoundGenerator soundGenerator;

    private WaypointProvider waypointProvider;
    private ActionGenerator actionGenerator;
    private Model model;

    private Objects.Observation observation;

    private boolean targetSet = false;

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }

        if(soundGenerator == null)
        {
            soundGenerator = SoundGenerator.create(this);
        }
        else
        {
            soundGenerator = SoundGenerator.getInstance();
        }
        waypointProvider = new WaypointProvider();
        if(actionGenerator == null)
        {
            actionGenerator = ActionGenerator.create(this);
        }
        else
        {
            actionGenerator = ActionGenerator.getInstance();
        }
        if(model == null)
        {
            model = Model.create(this);
        }
        else
        {
            model = Model.getInstance();
        }
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


    public Pose getWaypointPose()
    {
        return waypointProvider.getWaypointPose();
    }

    @Override
    public void onNewFrame(final Frame frame)
    {
        super.onNewFrame(frame);

        getMetrics().updateTimestamp(frame.getTimestamp());
        getMetrics().updatePhonePose(frame.getAndroidSensorPose());

        getMetrics().writeWifi();

        if(soundGenerator == null)
        {
            Log.w(TAG, "Sound Generator is dead. Likely that the thread has been killed");
            return;
        }

        if(!targetSet)
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
        getMetrics().updateTarget(target);
        Log.d(TAG, "Setting target");
        try
        {
            soundGenerator.getLock().lock();

            if(target != O_NOTHING)
            {
                model.setTarget(target);
                if(actionGenerator.setTarget(target, model))
                {
                    targetSet = true;
                    soundGenerator.setPhonePose(getFrame().getArFrame().getAndroidSensorPose());
                    soundGenerator.start();
                }
                else
                {
                    targetSet = false;
                    displayToast("Invalid target");
                }
            }
        }
        finally
        {
            soundGenerator.getLock().unlock();
        }
    }

    @Override
    public void onScanComplete(List<ObjectClassifier.Recognition> results)
    {
        RectF centreOfScreen = new RectF(0, 0, 300, 300);
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
            Log.d(TAG, observation.getFileName());
        }
        else
        {
            observation = O_NOTHING;
        }
        getMetrics().updateObservation(observation);
        displayToast(observation.getFriendlyName());

        if(observation == getTarget())
        {
            Log.i(TAG, "Target found");

            targetSet = false;
//            setTarget(O_NOTHING);
            soundGenerator.stop();
            getVibrator().vibrate(350);
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
