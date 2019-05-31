package com.example.jaycee.pomdpobjectsearch.mdptools;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.SEARCH_TIME_LIMIT;

public class GuidanceManager
{
    private static final String TAG = GuidanceManager.class.getSimpleName();

    private Waypoint waypoint;
    private State state;
    private Policy policy;

    private Pose devicePose;

    private long searchTimerZero;

    public GuidanceManager(Session session, Pose pose, Context context, int target)
    {
        devicePose = pose;
        waypoint = new Waypoint(session, pose);
        state = new State();
        policy = new Policy(context, target);

        searchTimerZero = System.currentTimeMillis();
    }

    public void end()
    {
        state = null;
        waypoint.clear();
        waypoint = null;
    }

    // Called on every sound emission loop
    public boolean updateDevicePose(Pose pose)
    {
        devicePose = pose;

        // Check if time limit for search has been exceeded, return true if it has
        return System.currentTimeMillis() - searchTimerZero > SEARCH_TIME_LIMIT;
    }

    public boolean waypointReached()
    {
        float[] phoneRotationAngles = getCameraVector();
        float cameraPan = -phoneRotationAngles[1];
        float cameraTilt = -phoneRotationAngles[2];

        float x = waypoint.getWaypointPose().getTranslation()[0];
        float y = waypoint.getWaypointPose().getTranslation()[1];

        // Compensate for Z-axis going in negative direction, rotating pan around y-axis

/*        Log.d(TAG, String.format("x: %f y %f", Math.cos(-cameraPan+Math.PI/2) + x, Math.sin(cameraTilt) - y));*/
        return Math.abs(Math.sin(cameraTilt) - y) < 0.1 && Math.abs(Math.cos(-cameraPan+Math.PI/2) + x) < 0.1;
    }

    public void provideGuidance(Session session, long observation)
    {
        long action = policy.getAction(state);
        float[] phoneRotationAngles = getCameraVector();
        float cameraPan = -phoneRotationAngles[1];
        float cameraTilt = -phoneRotationAngles[2];

        waypoint.updateWaypoint(action, session, cameraPan, cameraTilt, devicePose.tz());

        state.addObservation(observation, cameraPan, cameraTilt);
    }

    public Pose getWaypointPose() { return waypoint.getWaypointPose(); }

    public float[] getCameraVector()
    {
        ClassHelpers.mVector cameraVector = ClassHelpers.getCameraVector(this.devicePose);
        return cameraVector.getEuler();
    }

    public String objectCodeToString(long observation)
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
        return val;
    }
}
