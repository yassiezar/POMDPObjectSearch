package com.example.jaycee.pomdpobjectsearch;

import android.app.Application;

public class App extends Application
{
    public static final int NUM_STATES = 384;
    public static final int NUM_ACTIONS = 4;
    public static final int HORIZON_DISTANCE = 11;
    public static final int GRID_SIZE = 6;
    public static final int ANGLE_INTERVAL = 20;
    public static final int NUM_OBJECTS = 15;

    public static final Action A_UP = new Action(0);
    public static final Action A_DOWN = new Action(1);
    public static final Action A_LEFT = new Action(2);
    public static final Action A_RIGHT = new Action(3);

    static class Action
    {
        public int code;
        public Action(int code) { this.code = code; }
    }
}
