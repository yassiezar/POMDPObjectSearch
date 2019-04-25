package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;

import com.example.jaycee.pomdpobjectsearch.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import static com.example.jaycee.pomdpobjectsearch.Objects.Observation.O_NOTHING;
import static com.example.jaycee.pomdpobjectsearch.policy.State.MAX_STEPS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_OBS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STATE_VISITED;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STEPS;

public class Model
{
    private static final int NUM_OBJECTS = 15;

    private Objects.Observation target;

    private static Model model = null;

    private HashMap<Key, Double> transitions;

    private String[] objects;
    private String[] actions;

    private Model(Context context)
    {
        transitions = new HashMap<>();

        objects = new String[] {"Nothing", "Computer monitor", "Computer keyboard", "Computer mouse",
                                    "Desk", "Laptop", "Mug", "Window", "Lamp", "Backpack",
                                    "Chair", "Couch", "Plant", "Telephone", "Whiteboard", "Door"};
        actions = new String[] {"up", "down", "left", "right"};

        String json = null;
        try
        {
            InputStream is = context.getAssets().open("transitions/enlarged_objects_probabilities.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        try
        {
            JSONObject obj1Json = new JSONObject(json);
            Iterator<String> obj1Iter = obj1Json.keys();

            while(obj1Iter.hasNext())
            {
                String obj1 = obj1Iter.next();
                JSONObject actionJson = (JSONObject)obj1Json.get(obj1);
                Iterator<String> actionIter = actionJson.keys();
                while(actionIter.hasNext())
                {
                    String action = actionIter.next();
                    JSONObject obj2Json = (JSONObject)actionJson.get(action);
                    Iterator<String> obj2Iter = obj2Json.keys();
                    while(obj2Iter.hasNext())
                    {
                        String obj2 = obj2Iter.next();
                        double val = obj2Json.getDouble(obj2);
                        Key key = new Key(obj1, action, obj2);
                        transitions.put(key, val);
                    }
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized static Model create(final Context context)
    {
        if(model != null)
        {
            throw new AssertionError("Already initialised model");
        }

        model = new Model(context);
        return model;
    }

    public static Model getInstance()
    {
        if(model == null)
        {
            throw new AssertionError("Model not initialised");
        }

        return model;
    }

    public void setTarget(Objects.Observation target)
    {
        this.target = target;
    }

    public int[] getEncodedState(int state)
    {
        int[] stateVector = new int[3];

        stateVector[S_OBS] = state % NUM_OBJECTS;
        state /= NUM_OBJECTS;
        stateVector[S_STEPS] = state % MAX_STEPS;
        state /= MAX_STEPS;
        stateVector[S_STATE_VISITED] = state % 2;

        return stateVector;
    }

    double getObservationProbability(int state, int action, int obs)
    {
        double p = 0.5;
        int[] encodedState = getEncodedState(state);
        int[] encodedObs = getEncodedState(obs);

        if(state == obs && encodedState[S_OBS] != O_NOTHING.getCode())
        {
            return p;
        }
        else if(encodedState[S_STEPS] == encodedObs[S_STEPS] &&
                    encodedState[S_STATE_VISITED] == encodedObs[S_STATE_VISITED])
        {
            if(encodedState[S_OBS] == O_NOTHING.getCode()) return 1.0/(objects.length - 1);
            else if(encodedObs[S_OBS] == O_NOTHING.getCode()) return 1.0 - p;
        }

        return 0.0;
    }

    double getTransitionProbability(int state, int action, int state1)
    {
        int[] s1 = getEncodedState(state);
        int[] s2 = getEncodedState(state1);

        // Terminal state
        if(s1[S_OBS] == target.getCode())
        {
            if(s1 == s2) return 1.0;
            return 0.0;
        }

        // Move one up on distance
        if(s2[S_STEPS] != s1[S_STEPS] + 1)
        {
            if(s1[S_STEPS] + 1 == MAX_STEPS &&
                    s2[S_STEPS] == s1[S_STEPS] &&
                    s1[S_STATE_VISITED] == s2[S_STATE_VISITED] &&
                    s1[S_OBS] == s2[S_OBS]) return 1.0;
            return 0.0;
        }

        return transitions.get(new Key(objects[s1[S_OBS]], actions[action], objects[s2[S_OBS]]))/2.0;
    }

    class Key
    {
        String k1, k2, k3;
        public Key(String k1, String k2, String k3)
        {
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
        }
    }
}
