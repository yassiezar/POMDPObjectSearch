package com.example.jaycee.pomdpobjectsearch;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public interface ObjectClassifier
{
    List<Recognition> recogniseImage(Bitmap bitmap);
    void close();

    public class Recognition
    {
        private final String id;
        private final String title;
        private final Float confidence;
        private RectF location;
        private int code;

        public Recognition(final String id, final String title, final Float confidence, final RectF location)
        {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public int getCode()
        {
            if(title.equals("monitor")) return 1;
            if(title.equals("keyboard")) return 2;
            if(title.equals("mouse")) return 3;
            if(title.equals("desk")) return 4;
            if(title.equals("mug")) return 6;
            if(title.equals("supplies")) return 7;
            if(title.equals("window")) return 8;

            return 0;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {

            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}
