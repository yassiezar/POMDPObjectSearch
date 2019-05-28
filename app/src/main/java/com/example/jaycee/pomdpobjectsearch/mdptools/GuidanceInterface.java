package com.example.jaycee.pomdpobjectsearch.mdptools;

import com.google.ar.core.Pose;

public interface GuidanceInterface
{
    void onNewPoseAvailable();
    void onGuidanceStart(int target);
    void onGuidanceEnd();
    void onGuidanceRequested(long observation);
    void onPlaySound();
    boolean onWaypointReached();
    Pose onDrawWaypoint();
}
