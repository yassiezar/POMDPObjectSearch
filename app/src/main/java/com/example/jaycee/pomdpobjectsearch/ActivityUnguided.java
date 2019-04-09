package com.example.jaycee.pomdpobjectsearch;

public class ActivityUnguided extends ActivityBase
{
    /* TODO:
    Detect object in middle of screen
    Add text to speech to read objects in middle on screen tap
     */

    @Override
    public void onResume()
    {
        super.onResume();

        surfaceView.enableScreenTap();
    }

    @Override
    public void setTarget(int target)
    {

    }

    @Override
    public void onNewTimestamp(long timestamp)
    {

    }
}
