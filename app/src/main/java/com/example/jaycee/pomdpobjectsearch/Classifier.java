package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;

import java.util.List;

public interface Classifier
{
    List<Recognition> classifyImage(byte[] imageData);
}
