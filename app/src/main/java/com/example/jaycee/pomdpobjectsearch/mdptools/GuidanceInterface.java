package com.example.jaycee.pomdpobjectsearch.mdptools;

public interface GuidanceInterface
{
    void onNewPoseAvailable();
/*    void onUpdateWaypoint(long action);*/
    void onGuidanceStart(int target);
    void onGuidanceEnd();
    boolean onWaypointReached();
    void onGuidanceRequested(long observation);
    void onPlaySound();
}
