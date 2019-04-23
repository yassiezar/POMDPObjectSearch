package com.example.jaycee.pomdpobjectsearch.imageprocessing;

import com.google.ar.core.Frame;

import java.util.List;

public interface FrameHandler
{
    void onNewFrame(Frame frame);
    void onScanComplete(List<ObjectClassifier.Recognition> results);
    void onNewTimestamp(long timestamp);
}
