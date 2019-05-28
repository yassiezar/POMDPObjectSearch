package com.example.jaycee.pomdpobjectsearch.mdptools;

import com.google.ar.core.Pose;

public interface GuidanceInterface
{
    void onNewPoseAvailable();
    void onGuidanceStart(int target);
    void onGuidanceEnd();
    void onGuidanceRequested(long observation);
    boolean onWaypointReached();
    Pose onDrawWaypoint();
    Pose onWaypointPoseRequested();
    Pose onDevicePoseRequested();
    float[] onCameraVectorRequested();
}