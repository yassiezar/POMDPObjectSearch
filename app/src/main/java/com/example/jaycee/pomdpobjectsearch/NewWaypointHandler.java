package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.views.Arrow;

public interface NewWaypointHandler
{
    void onNewWaypoint();
    void onTargetFound();
    void onArrowDirectionChange(Arrow.Direction direction);
}
