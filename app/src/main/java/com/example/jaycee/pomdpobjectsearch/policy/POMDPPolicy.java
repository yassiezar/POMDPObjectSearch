package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.Objects;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;

import static com.example.jaycee.pomdpobjectsearch.App.NUM_STATES;
import static com.example.jaycee.pomdpobjectsearch.policy.State.MAX_STEPS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_OBS;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STEPS;

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

    public boolean setTarget(Objects.Observation target)
    {
        int numStates = NUM_STATES;

        ArrayList<ArrayList<VEntry>> policy = new ArrayList<>();
        ArrayList<VEntry> horizon;

        Scanner reader = null;
        try
        {
            String fileName = "POMDPPolicies/" + target.getFileName();
            reader = new Scanner(new InputStreamReader(context.getAssets().open(fileName)));
            reader.useDelimiter("\\n");

            horizon = new ArrayList<>();

            DoubleVector values = new DoubleVector((Collections.nCopies(numStates, 0.0)));
            int action;
            ArrayList<Integer> obs = new ArrayList<>(Collections.nCopies(numStates, 0));

            while (reader.hasNext() && policy.size() < MAX_STEPS)
            {
                ArrayList<String> entries = new ArrayList<>(Arrays.asList(reader.nextLine().split("\\s+")));
                entries.remove("");
                if(entries.contains("@"))
                {
                    policy.add(new ArrayList<>(horizon));
                    horizon.clear();
                    continue;
                }
                int entryIndex = 0;
                for(int i = 0; i < numStates; i ++)
                {
                    values.vector.set(i, Double.valueOf(entries.get(entryIndex++)));
                }
                action = Integer.valueOf(entries.get(entryIndex++));
                for(int i = 0; i < numStates; i ++)
                {
                    obs.set(i, Integer.valueOf(entries.get(entryIndex++)));
                }
                horizon.add(new VEntry(values, action, obs));
                values = new DoubleVector(Collections.nCopies(numStates, 0.0));
                obs = new ArrayList<>(Collections.nCopies(numStates, 0));
            }
            this.policy = new ArrayList<>(policy);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not open policy file: " + e);
            return false;
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }

        return true;
    }

    public ActionId getAction(int id, State state)
    {
        int[] decodesState = state.getEncodedState();
        int h = decodesState[S_STEPS];
        ArrayList<VEntry> vlist = policy.get(h+1);

        int newId = vlist.get(id).observations.get(decodesState[S_OBS]);
        int action = policy.get(h).get(newId).action;

        return new ActionId(newId, action);
    }

    public ActionId getAction(DoubleVector b, int h)
    {
        ArrayList<VEntry> vlist = policy.get(h);

        VEntry bestMatch = findBestAtPoint(b, vlist);

        return new ActionId(policy.indexOf(bestMatch), bestMatch.action);
    }

    private VEntry findBestAtPoint(DoubleVector point, ArrayList<VEntry> vlist)
    {
        VEntry bestMatch = vlist.get(0);
        double bestValue = point.dot(bestMatch.values);

        for(VEntry ele : vlist)
        {
            double currValue = point.dot(bestMatch.values);
            if(currValue > bestValue ||
                    (currValue == bestValue && ele.values.vecCompare(bestMatch.values) > 0))
            {
                bestMatch = ele;
                bestValue = currValue;
            }
        }

        return bestMatch;
    }

    static class DoubleVector
    {
        ArrayList<Double> vector;

        DoubleVector(int cap)
        {
            vector = new ArrayList<>(cap);
        }

        DoubleVector(Collection<Double> collection)
        {
            vector = new ArrayList<>(collection);
        }

        DoubleVector() { this.vector = new ArrayList<>(); }

        int vecCompare(DoubleVector vec)
        {
            for(int i = 0; i < vector.size(); i++)
            {
                if(vector.get(i) > vec.vector.get(i)) return 1;
                if(vector.get(i) < vec.vector.get(i)) return -1;
            }
            return 0;
        }

        double dot(DoubleVector vec)
        {
            double result = 0.0;
            for(int i = 0; i < this.vector.size(); i++)
            {
                result += (this.vector.get(i) * vec.vector.get(i));
            }
            return result;
        }
    }

    public class VEntry
    {
        public DoubleVector values;
        public int action;
        public ArrayList<Integer>observations;

        public VEntry() {}
        public VEntry(DoubleVector v, int a, ArrayList<Integer> o)
        {
            values = v;
            action = a;
            observations = o;
        }
        public VEntry(int v, int a, int o)
        {
            values = new DoubleVector(v);
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

