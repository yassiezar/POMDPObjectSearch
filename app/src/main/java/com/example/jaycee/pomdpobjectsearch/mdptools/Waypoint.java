package com.example.jaycee.pomdpobjectsearch.mdptools;

import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.ANGLE_INTERVAL;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.A_DOWN;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.A_LEFT;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.A_RIGHT;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.A_UP;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.GRID_SIZE_PAN;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.GRID_SIZE_TILT;

class Waypoint
{
    private static final String TAG = Waypoint.class.getSimpleName();

    private Pose waypointPose;
    private Anchor anchor;

    Waypoint(Session session, Pose pose)
    {
        float[] phoneTranslation = pose.getTranslation();
        this.waypointPose = new Pose(new float[] {phoneTranslation[0], phoneTranslation[1], phoneTranslation[2] - 1.f}, pose.getRotationQuaternion());
        this.anchor = session.createAnchor(pose);
    }

    Pose getWaypointPose() { return waypointPose; }

    void clear()
    {
        anchor.detach();
    }

    void updateWaypoint(long action, Session session, Pose devicePose)
    {
        ClassHelpers.mVector cameraVector = ClassHelpers.getCameraVector(devicePose);
        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];

        float[] wayPointTranslation = new float[3];

        // Assume the current waypoint is where the camera is pointing.
        // Reasonable since this function only called when pointing to new target
        // Discretise pan/tilt into grid
        int pan = (int)((Math.floor(Math.toDegrees(cameraPan)/ANGLE_INTERVAL)) + GRID_SIZE_PAN/2 - 1);
        int tilt = (int)((Math.floor(Math.toDegrees(cameraTilt)/ANGLE_INTERVAL)) + GRID_SIZE_TILT/2 - 1);

        if(action == A_LEFT)
        {
            pan -= 1;
        }
        else if(action == A_RIGHT)
        {
            pan += 1;
        }
        else if(action == A_UP)
        {
            tilt += 1;
        }
        else if(action == A_DOWN)
        {
            tilt -= 1;
        }

        // Wrap the world
        if(pan < 0) pan = GRID_SIZE_PAN - 1;
        if(pan > GRID_SIZE_PAN - 1) pan = 0;
        if(tilt < 0) tilt = GRID_SIZE_TILT - 1;
        if(tilt > GRID_SIZE_TILT - 1) tilt = 0;

        float z =  devicePose.getTranslation()[2] - 1.f;
        wayPointTranslation[0] = (float)Math.sin(Math.toRadians(ANGLE_INTERVAL*(pan - GRID_SIZE_PAN/2 + 1)));
        wayPointTranslation[1] = (float)Math.sin(Math.toRadians(ANGLE_INTERVAL*(tilt - GRID_SIZE_TILT/2 + 1)));
        wayPointTranslation[2] = z;

        Log.i(TAG, String.format("new pan: %d new tilt: %d", pan, tilt));

        waypointPose = new Pose(wayPointTranslation, new float[]{0.f, 0.f, 0.f, 1.f});

        // Update anchor
        anchor.detach();
        anchor = session.createAnchor(waypointPose);
    }
}
