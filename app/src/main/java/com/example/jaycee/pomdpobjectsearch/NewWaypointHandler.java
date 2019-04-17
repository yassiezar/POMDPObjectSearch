package com.example.jaycee.pomdpobjectsearch;

public interface NewWaypointHandler
{
    void onNewWaypoint();
    void onTargetFound();
    void onArrowDirectionChange(Arrow.Direction direction);
}
