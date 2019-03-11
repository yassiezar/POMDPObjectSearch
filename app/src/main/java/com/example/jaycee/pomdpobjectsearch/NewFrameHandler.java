package com.example.jaycee.pomdpobjectsearch;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;

public interface NewFrameHandler
{
    void onNewFrame(Frame frame);
    void setSession(Session session);
    void onNewTimestamp(long timestamp);
}
