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
        private ActivityBase.Observation observation;

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

        public ActivityBase.Observation getObservation()
        {
            switch(getCode())
            {
                case 1: return ActivityBase.Observation.T_COMPUTER_MONITOR;
                case 2: return ActivityBase.Observation.T_COMPUTER_KEYBOARD;
                case 3: return ActivityBase.Observation.T_COMPUTER_MOUSE;
                case 4: return ActivityBase.Observation.T_DESK;
                case 5: return ActivityBase.Observation.T_LAPTOP;
                case 6: return ActivityBase.Observation.T_MUG;
                case 8: return ActivityBase.Observation.T_WINDOW;
                case 9: return ActivityBase.Observation.T_BACKPACK;
                case 10: return ActivityBase.Observation.T_CHAIR;
                case 11: return ActivityBase.Observation.T_COUCH;
                case 12: return ActivityBase.Observation.T_PLANT;
                case 13: return ActivityBase.Observation.T_TELEPHONE;
                case 14: return ActivityBase.Observation.T_WHITEBOARD;
                case 15: return ActivityBase.Observation.T_DOOR;
                default: return ActivityBase.Observation.O_NOTHING;
            }
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
