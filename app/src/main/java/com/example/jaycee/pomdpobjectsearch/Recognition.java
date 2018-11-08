package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;

public class Recognition
{
    private Integer id;
    private String title;
    private Float confidence;
    private RectF location;

/*    public Recognition(final int id, final String title, final Float confidence, final RectF location)
    {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }*/

    public Recognition(int id, String title, Float confidence, int x, int y, int w, int h)
    {
        this.id = id;
        this.title = title;
        this.confidence = confidence;

        this.location = new RectF(x, y, x+w, y+h);
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public Float getConfidence() { return confidence; }
    public RectF getLocation() { return location; }

    public void setId(int id) { this.id = id;}
    public void setTitle(String title) { this.title = title; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }
    public void setLocation(RectF location) { this.location = location; }

    @Override
    public String toString()
    {
        String resultString = "";
        if (id != null)
        {
            resultString += "[" + id + "] ";
        }

        if (title != null)
        {
            resultString += title + " ";
        }

        if (confidence != null)
        {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f);
        }

        if (location != null)
        {
            resultString += location + " ";
        }

        return resultString.trim();
    }
}
