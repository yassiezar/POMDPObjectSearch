package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.Objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class POMDPPolicy implements Policy
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
            long action;
            ArrayList<Long> obs = new ArrayList<>(Collections.nCopies(numStates, 0L));

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
                action = Long.valueOf(entries.get(entryIndex++));
                for(int i = 0; i < numStates; i ++)
                {
                    obs.set(i, Long.valueOf(entries.get(entryIndex++)));
                }
                horizon.add(new VEntry(values, action, obs));
                values = new ArrayList<>(Collections.nCopies(numStates, 0.0));
                obs = new ArrayList<>(Collections.nCopies(numStates, 0L));
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

    @Override
    public long getAction(State state)
    {
        // TODO: FIX
        return 0;
    }

    public class VEntry
    {
        public ArrayList<Double> values;
        public long action;
        public ArrayList<Long>observations;

        public VEntry() {}
        public VEntry(ArrayList<Double> v, long a, ArrayList<Long> o)
        {
            values = v;
            action = a;
            observations = o;
        }
        public VEntry(long v, long a, long o)
        {
            values = new ArrayList<>((int)v);
            action = a;
            observations = new ArrayList<>((int)o);
        }
    }
}

