package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.example.jaycee.pomdpobjectsearch.App.NUM_OBJECTS;
import static com.example.jaycee.pomdpobjectsearch.Objects.Observation.O_NOTHING;
import static com.example.jaycee.pomdpobjectsearch.policy.State.MAX_STEPS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_OBS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STATE_VISITED;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STEPS;

public class Model
{
    private static final String TAG = Model.class.getSimpleName();

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
            JSONObject jsonObject = new JSONObject(json);
            for(String obj1 : objects)
            {
                for(String action : actions)
                {
                    for(String obj2 : objects)
                    {
                        transitions.put(new Key(obj1, action, obj2), jsonObject.getJSONObject(obj1).getJSONObject(action).getDouble(obj2));
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
            Log.w(TAG, "Already initialised model");
            return model;
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

    double getTransitionProbability(int state1, int action, int state2)
    {
        int[] s1 = getEncodedState(state1);
        int[] s2 = getEncodedState(state2);

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

        Key key = new Key(objects[s1[S_OBS]], actions[action], objects[s2[S_OBS]]);
        if(transitions.containsKey(key))
        {
            return transitions.get(key)/2.0;
        }
        return 0.0;
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

        public String toString()
        {
            return String.format("transition keys: %s %s %s", k1, k2, k3);
        }

        @Override
        public boolean equals(Object obj)
        {
            if(this == obj) return true;
            if(obj == null || getClass() != obj.getClass()) return false;
            Key key = (Key)obj;

            if(k1 != null ? !k1.equals(key.k1) : key.k1 != null) return false;
            if(k2 != null ? !k2.equals(key.k2) : key.k2 != null) return false;
            if(k3 != null ? !k3.equals(key.k3) : key.k3 != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = 11 * k1.hashCode();
            result = 13*result + (k2.hashCode());
            result = 13*result + (k3.hashCode());

            return result;
        }
    }
}
