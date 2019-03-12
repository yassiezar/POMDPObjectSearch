package com.example.jaycee.pomdpobjectsearch;

import com.google.ar.core.Frame;

public interface NewFrameHandler
{
    void onNewFrame(Frame frame);
    void onNewTimestamp(long timestamp);
}
