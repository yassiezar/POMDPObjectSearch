package com.example.jaycee.pomdpobjectsearch.mdptools;

import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.ANGLE_INTERVAL;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.GRID_SIZE_TILT;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.GRID_SIZE_PAN;

class State
{
    private static final String TAG = State.class.getSimpleName();

    private static final int NUM_OBJECTS = 9;
    private static final int MAX_STEPS = 12;

    private static final int S_OBS = 0;
    private static final int S_STEPS = 1;
    private static final int S_STATE_VISITED = 2;

    private long state;

    private long observation = 0;
    private long steps = 0;
    private long stateVisted = 0;

    private int[] panHistory = new int[GRID_SIZE_PAN];
    private int[] tiltHistory = new int[GRID_SIZE_TILT];

    State()
    {
        for(int i = 0; i < GRID_SIZE_PAN; i ++)
        {
            panHistory[i] = 0;
        }
        for(int i = 0; i < GRID_SIZE_TILT; i ++)
        {
            tiltHistory[i] = 0;
        }
    }

    long getDecodedState()
    {
        long state = 0;
        long multiplier = 1;

        state += (multiplier * observation);
        multiplier *= NUM_OBJECTS;
        state += (multiplier * steps);
        multiplier *= MAX_STEPS;
        state += (multiplier * stateVisted);

        return state;
    }

    long[] getEncodedState()
    {
        long[] stateVector = new long[3];
        long state = this.state;

        stateVector[S_OBS] = state % NUM_OBJECTS;
        state /= NUM_OBJECTS;
        stateVector[S_STEPS] = state % MAX_STEPS;
        state /= MAX_STEPS;
        stateVector[S_STATE_VISITED] = state % 2;

        return stateVector;
    }

    void addObservation(long observation, float fpan, float ftilt)
    {
        // Origin is top right, not bottom left
        int pan = (int)((Math.floor(Math.toDegrees(fpan)/ANGLE_INTERVAL)) + GRID_SIZE_PAN/2 - 1);
        int tilt = (int)((Math.floor(Math.toDegrees(ftilt)/ANGLE_INTERVAL)) + GRID_SIZE_TILT/2 - 1);

        if(pan < 0) pan = GRID_SIZE_PAN - 1;
        if(pan > GRID_SIZE_PAN - 1) pan = 0;
        if(tilt < 0) tilt = GRID_SIZE_TILT - 1;
        if(tilt > GRID_SIZE_TILT - 1) tilt = 0;

        this.observation = observation;
        if(this.steps != MAX_STEPS-1) this.steps ++;

        if(panHistory[pan] == 1 && tiltHistory[tilt] == 1) this.stateVisted = 1;
        else this.stateVisted = 0;

        panHistory[pan] = 1;
        tiltHistory[tilt] = 1;

        this.state = getDecodedState();
    }
}
