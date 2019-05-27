package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.mdptools.GuidanceInterface;
import com.example.jaycee.pomdpobjectsearch.mdptools.Policy;
import com.example.jaycee.pomdpobjectsearch.mdptools.State;
import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.ar.core.Pose;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class SoundGenerator implements Runnable
{
    private static final String TAG = SoundGenerator.class.getSimpleName();

    private static final int O_NOTHING = 0;

    private Context context;
    private SurfaceRenderer renderer;

/*    private Pose phonePose;
    private Pose offsetPose;*/

    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long target = -1;

/*    private Policy policy;*/

    private Metrics metrics = new Metrics();
/*    private State state;*/

    private Vibrator vibrator;
    private Toast toast;
    private Handler handler = new Handler();

    private boolean stop = false;
    private boolean targetSet = false;
    private boolean targetFound = false;

    private BarcodeListener barcodeListener;
    private GuidanceInterface guidanceInterface;

    SoundGenerator(Context context, SurfaceRenderer renderer)
    {
        this.context = context;
        this.renderer = renderer;
        this.barcodeListener = (BarcodeListener)context;
        this.guidanceInterface = (GuidanceInterface)context;

        this.vibrator= (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void stop()
    {
        this.stop = true;
        handler = null;
        guidanceInterface.onGuidanceEnd();
    }

    @Override
    public void run()
    {
/*        phonePose = renderer.getDevicePose();*/

        if(!isTargetSet() || isTargetFound())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        if(!renderer.isRendererReady())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        setObservation(barcodeListener.onBarcodeScan());

        float gain = 1.f;
        if(observation == target)
        {
            Log.i(TAG, "Target found");
            targetFound = true;
            targetSet = false;
            observation = O_NOTHING;
            vibrator.vibrate(350);
            gain = 0.f;

            ((ActivityCamera)context).getCentreView().resetArrows();
            renderer.setDrawWaypoint(false);
            guidanceInterface.onGuidanceEnd();
        }

        guidanceInterface.onNewPoseAvailable();
        long newCameraObservation = this.observation;

        if(guidanceInterface.onWaypointReached() || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))
        {
/*            long action = policy.getAction(state);
            Log.d(TAG, String.format("Object found or found waypoint, action: %d", action));*/
/*            long[] testState = state.getEncodedState();
            Log.i(TAG, String.format("State %d obs %d steps %d prev %d", state.getDecodedState(), testState[0], testState[1], testState[2]));*/
/*            guidanceInterface.onUpdateWaypoint(action);*/
/*            waypoint.updateWaypoint(cameraPan, cameraTilt, phonePose.getTranslation()[2], action);*/
            prevCameraObservation = newCameraObservation;
            guidanceInterface.onGuidanceRequested(newCameraObservation);
/*            state.addObservation(newCameraObservation, cameraPan, cameraTilt);*/
            Log.i(TAG, "Setting new waypoint");
        }
/*        ClassHelpers.mVector waypointVector = new ClassHelpers.mVector(waypoint.pose.getTranslation());
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];*/

        // Set direction arrow
/*        ClassHelpers.mVector vectorToWaypoint = waypointVector.translate(cameraVector);
        Log.d(TAG, String.format("x %f y %f z %f", vectorToWaypoint.x, vectorToWaypoint.y, vectorToWaypoint.z));
        Log.d(TAG, String.format("x %f y %f z %f", cameraVector.x, cameraVector.y, cameraVector.z));
        Log.d(TAG, String.format("x %f y %f z %f", waypointVector.x, waypointVector.y, waypointVector.z));*/
/*        ((ActivityCamera)context).getCentreView().resetArrows();
        if(vectorToWaypoint.x > 0.1)
        {
            ((ActivityCamera)context).getCentreView().setArrowAlpha(Arrow.Direction.RIGHT, 255);
        }
        else if (vectorToWaypoint.x < -0.1)
        {
            ((ActivityCamera)context).getCentreView().setArrowAlpha(Arrow.Direction.LEFT, 255);
        }
        if(vectorToWaypoint.y > 0.1)
        {
            ((ActivityCamera)context).getCentreView().setArrowAlpha(Arrow.Direction.UP, 255);
        }
        if(vectorToWaypoint.y < -0.1)
        {
            ((ActivityCamera)context).getCentreView().setArrowAlpha(Arrow.Direction.DOWN, 255);
        }*/

        guidanceInterface.onPlaySound();
        if(!stop) handler.postDelayed(this, 40);
    }


    public void setTarget(long target)
    {
/*        policy = new Policy(context, (int)target);
        state = new State();*/

        this.target = target;
        this.targetSet = true;
        this.targetFound = false;

        prevCameraObservation = O_NOTHING;//observation;

        metrics.updateTarget(target);

        renderer.setDrawWaypoint(true);
    }

    public void setObservation(long observation)
    {
        final String val;
        if(observation == 1)
        {
            val = "Monitor";
        }
        else if(observation == 2)
        {
            val = "Keyboard";
        }
        else if(observation == 3)
        {
            val = "Mouse";
        }
        else if(observation == 4)
        {
            val = "Desk";
        }
        else if(observation == 5)
        {
            val = "Laptop";
        }
        else if(observation == 6)
        {
            val = "Mug";
        }
        else if(observation == 7)
        {
            val = "Office supplies";
        }
        else if(observation == 8)
        {
            val = "Window";
        }
        else
        {
            val = "Unknown";
        }
        this.observation = observation;
        this.metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            ((ActivityCamera)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(toast != null)
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(context, val, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

/*    public void markOffsetPose() { this.offsetPose = phonePose; }*/
    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public long getTarget() { return this.target; }
}
