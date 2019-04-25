package com.example.jaycee.pomdpobjectsearch.policy;

import com.example.jaycee.pomdpobjectsearch.Objects;

import static com.example.jaycee.pomdpobjectsearch.App.ANGLE_INTERVAL;
import static com.example.jaycee.pomdpobjectsearch.App.GRID_SIZE;

public class State
{
    private static final int NUM_OBJECTS = 15;
    public static final int MAX_STEPS = 12;

    public static final int S_OBS = 0;
    public static final int S_STEPS = 1;
    public static final int S_STATE_VISITED = 2;

    private int state;

    private Objects.Observation observation = Objects.Observation.O_NOTHING;

    private long steps = 0;
    private long stateVisted = 0;

    private int[] panHistory = new int[GRID_SIZE];
    private int[] tiltHistory = new int[GRID_SIZE];

    public State()
    {
        for (int i = 0; i < GRID_SIZE; i++)
        {
            panHistory[i] = 0;
            tiltHistory[i] = 0;
        }
    }

    public int getDecodedState()
    {
        int state = 0;
        int multiplier = 1;

        state += (multiplier * observation.getCode());
        multiplier *= NUM_OBJECTS;
        state += (multiplier * steps);
        multiplier *= MAX_STEPS;
        state += (multiplier * stateVisted);

        return state;
    }

    public int[] getEncodedState()
    {
        int[] stateVector = new int[3];
        int state = this.state;

        stateVector[S_OBS] = state % NUM_OBJECTS;
        state /= NUM_OBJECTS;
        stateVector[S_STEPS] = state % MAX_STEPS;
        state /= MAX_STEPS;
        stateVector[S_STATE_VISITED] = state % 2;

        return stateVector;
    }

    public void addObservation(Objects.Observation observation, float fpan, float ftilt)
    {
        // Origin is top right, not bottom left
        int pan = (int) ((Math.floor(Math.toDegrees(fpan) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);
        int tilt = (int) ((Math.floor(Math.toDegrees(ftilt) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);

        if (pan < 0) pan = GRID_SIZE - 1;
        if (pan > GRID_SIZE - 1) pan = 0;
        if (tilt < 0) tilt = GRID_SIZE - 1;
        if (tilt > GRID_SIZE - 1) tilt = 0;

        this.observation = observation;
        if (this.steps != MAX_STEPS - 1) this.steps++;

        if (panHistory[pan] == 1 && tiltHistory[tilt] == 1) this.stateVisted = 1;
        else this.stateVisted = 0;

        panHistory[pan] = 1;
        tiltHistory[tilt] = 1;

        this.state = getDecodedState();
    }
}
