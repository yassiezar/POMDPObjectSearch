package com.example.jaycee.pomdpobjectsearch.mdptools;

import android.content.Context;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

public class GuidanceManager
{
    private static final String TAG = GuidanceManager.class.getSimpleName();

    private Waypoint waypoint;
    private State state;
    private Policy policy;

    private Pose devicePose;

    public GuidanceManager(Session session, Pose pose, Context context, int target)
    {
        devicePose = pose;
        waypoint = new Waypoint(session, pose);
        state = new State();
        policy = new Policy(context, target);
    }

    public void end()
    {
        state = null;
        waypoint.clear();
        waypoint = null;
    }

    public void updateDevicePose(Pose pose)
    {
        devicePose = pose;
    }

    public boolean waypointReached()
    {
        float[] phoneRotationAngles = getCameraVector();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];

        float x = waypoint.getWaypointPose().getTranslation()[0];
        float y = waypoint.getWaypointPose().getTranslation()[1];

        /*        Log.d(TAG, String.format("x: %f y %f", Math.sin(pan) - x, Math.sin(tilt) - y));*/
        // Compensate for Z-axis going in negative direction, rotating pan around y-axis
        return Math.abs(Math.sin(cameraTilt) - y) < 0.1 && Math.abs(Math.cos(-cameraPan+Math.PI/2) - x) < 0.1;
    }

    public void provideGuidance(Session session, long observation)
    {
        long action = policy.getAction(state);
        waypoint.updateWaypoint(action, session, devicePose);

        float[] phoneRotationAngles = getCameraVector();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];
        state.addObservation(observation, cameraPan, cameraTilt);
    }

    public Pose getWaypointPose() { return waypoint.getWaypointPose(); }

    public float[] getCameraVector()
    {
        ClassHelpers.mVector cameraVector = ClassHelpers.getCameraVector(this.devicePose);
        return cameraVector.getEuler();
    }
}
