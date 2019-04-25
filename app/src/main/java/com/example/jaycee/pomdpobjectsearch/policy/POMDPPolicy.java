package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.Objects;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

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

        ObjectInputStream ois = null;
        InputStream is = null;
        try
        {
            String fileName = "POMDPPolicies/" + target.getFileName();
            is = context.getAssets().open(fileName);
            ois = new ObjectInputStream(is);

            ArrayList<VEntry> entry;
            boolean running = true;
            while (running)
            {
                entry = (ArrayList<VEntry>)ois.readObject();
/*                ArrayList<Double> value = new ArrayList<>(numStates);
                long action;
                ArrayList<Long> obs = new ArrayList<>(numStates);*/

                policy.add(entry);

                running = false;
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not open policy file: " + e);
        }
        catch (ClassNotFoundException e)
        {
            Log.e(TAG, "Class not found: " + e);
        }
        finally
        {
            if (ois != null)
            {
                try
                {
                    ois.close();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Error closing the file: " + e);
                }
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

