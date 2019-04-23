package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.google.ar.core.Pose;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaypointProvider
{
    private static final String TAG = WaypointProvider.class.getSimpleName();

    private Pose waypointPose;

    private Lock lock = new ReentrantLock();

    public Objects.Observation observation = Objects.Observation.O_NOTHING;

    public WaypointProvider()
    {
    }

    public Lock getLock() { return this.lock; }

    Pose getWaypointPose()
    {
        return waypointPose;
    }

    void updateWaypoint(VectorTools.PanAndTilt newAngles, float z)
    {
        float[] wayPointTranslation = new float[3];

        wayPointTranslation[0] = (float) Math.sin(Math.toRadians(newAngles.pan));
        wayPointTranslation[1] = (float) Math.sin(Math.toRadians(newAngles.tilt));
        wayPointTranslation[2] = z - 1.f;

        // Log.i(TAG, String.format("new pan: %d new tilt: %d", pan, tilt));
        //Log.i(TAG, String.format("translation x %f translation y: %f", wayPointTranslation[0], wayPointTranslation[1]));

        waypointPose = new Pose(wayPointTranslation, new float[]{0.f, 0.f, 0.f, 1.f});
    }

    public boolean waypointReached(float cameraPan, float cameraTilt)
    {
        if(waypointPose == null)
        {
            return true;
        }

        float x = waypointPose.getTranslation()[0];
        float y = waypointPose.getTranslation()[1];

        // Compensate for Z-axis going in negative direction, rotating pan around y-axis
        return Math.abs(Math.sin(cameraTilt) - y) < 0.1 && Math.abs(Math.cos(-cameraPan + Math.PI / 2) - x) < 0.1;
    }
}
