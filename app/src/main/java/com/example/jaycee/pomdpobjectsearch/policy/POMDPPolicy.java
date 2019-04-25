package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.ActivityEntry;
import com.example.jaycee.pomdpobjectsearch.Objects;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class POMDPPolicy
{
    private static final String TAG = POMDPPolicy.class.getSimpleName();

    private ArrayList<ArrayList<VEntry>> policy;
    private Context context;

    public POMDPPolicy(Context context)
    {
        this.context = context;
    }

    public ArrayList<ArrayList<VEntry>> getPolicy() { return this.policy; }

    public void setTarget(Objects.Observation target, int numStates)
    {
        this.policy = new ArrayList<>(numStates);

        Scanner reader = null;
        try
        {
            String fileName = "POMDPPolicies/" + target.getFileName();
            fileName = "POMDPPolicies/tiger.txt";
            reader = new Scanner(new InputStreamReader(context.getAssets().open(fileName)));
            reader.useDelimiter("\\n");

            ArrayList<VEntry> horizon = new ArrayList<>();

            ArrayList<Double> values = new ArrayList<>(Collections.nCopies(numStates, 0.0));
            int action;
            ArrayList<Integer> obs = new ArrayList<>(Collections.nCopies(numStates, 0));

            while (reader.hasNext())
            {
                ArrayList<String> entries = new ArrayList<>(Arrays.asList(reader.nextLine().split("\\s+")));
                entries.remove("");
                if(entries.contains("@"))
                {
                    policy.add(horizon);
                    horizon.clear();
                    continue;
                }
                int entryIndex = 0;
                for(int i = 0; i < numStates; i ++)
                {
                    values.set(i, Double.valueOf(entries.get(entryIndex++)));
                }
                action = Integer.valueOf(entries.get(entryIndex++));
                for(int i = 0; i < numStates; i ++)
                {
                    obs.set(i, Integer.valueOf(entries.get(entryIndex++)));
                }
                horizon.add(new VEntry(values, action, obs));
                values = new ArrayList<>(Collections.nCopies(numStates, 0.0));
                obs = new ArrayList<>(Collections.nCopies(numStates, 0));
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not open policy file: " + e);
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
    }

    public ActionId getAction(int id, int obs, int h)
    {
        // TODO: FIX
        ArrayList<VEntry> vlist = policy.get(h+1);

        int newId = vlist.get(id).observations.get(obs);
        int action = policy.get(h).get(newId).action;

        return new ActionId(newId, action);
    }

    public class VEntry
    {
        public ArrayList<Double> values;
        public int action;
        public ArrayList<Integer>observations;

        public VEntry() {}
        public VEntry(ArrayList<Double> v, int a, ArrayList<Integer> o)
        {
            values = v;
            action = a;
            observations = o;
        }
        public VEntry(int v, int a, int o)
        {
            values = new ArrayList<>(v);
            action = a;
            observations = new ArrayList<>(o);
        }
    }

    public class ActionId
    {
        public int id, action;
        public ActionId(int id, int action)
        {
            this.id = id;
            this.action = action;
        }
    }
}

