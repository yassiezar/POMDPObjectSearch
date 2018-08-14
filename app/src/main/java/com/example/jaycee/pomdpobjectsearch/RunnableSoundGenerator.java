package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();

    private static final int O_NOTHING = 0;
    private static final int O_DESK = 11;
    private static final int O_LAPTOP = 5;


    private Activity callingActivity;

    private Pose phonePose;
    private Waypoint waypoint = new Waypoint();
    private Pose offsetPose;
    private Anchor waypointAnchor;
    private Session session;

    private boolean targetSet = false;
    private boolean targetFound = false;

    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long target = -1;

    private Policy policy;

    private ClassMetrics metrics = new ClassMetrics();
    private State state = new State();

    private Vibrator vibrator;
    private Toast toast;

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
        this.vibrator= (Vibrator)callingActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void run()
    {
        float gain = 1.f;
        if(observation == target)
        {
            Log.i(TAG, "Target found");
            targetFound = true;
            targetSet = false;
            observation = O_NOTHING;
            vibrator.vibrate(350);
            gain = 0.f;
        }

        // Get Euler angles from vector wrt axis system
        // pitch = tilt, yaw = pan
        ClassHelpers.mVector cameraVector = getRotation(phonePose, false);
        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];
        long newCameraObservation = this.observation;

        // Get current state and generate new waypoint if agent is in new state or sees new object
        //long[] currentStateArr = state.getEncodedState();
        //long[] waypointArr = encodeState(waypointState);
        //Log.i(TAG, String.format("current pan %d tilt %d obs %d ", currentStateArr[0], currentStateArr[1], currentStateArr[2]));
        Log.d(TAG, String.format("current pan %f tilt %f ", cameraPan, cameraTilt));
        if(waypoint.waypointReached(cameraPan, cameraTilt) || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))
        {
            long action = policy.getAction(state);
            Log.i(TAG, String.format("Object found or found waypoint, action: %d", action));
            waypoint.updateWaypoint(phonePose, state, action);
            waypointAnchor = session.createAnchor(waypoint.getPose());
            prevCameraObservation = newCameraObservation;
            state.addObservation(newCameraObservation);
        }
        ClassHelpers.mVector waypointVector = getRotation(waypoint.getPose(), false);
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];

        // float tiltRequired = (float)Math.atan2(cameraVector.y - waypointVector.y, cameraVector.z - waypointVector.z);
        // Log.i(TAG, String.format("Tilt required %f", tiltRequired));

        JNIBridge.playSound(waypoint.getPose().getTranslation(), cameraVector.asFloat(), gain, getPitch(waypointTilt - cameraTilt));
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        metrics.writeWifi();
        this.session = session;

        this.run();
    }

    private ClassHelpers.mVector getRotation(Pose pose, boolean isWaypointPose)
    {
        // Get rotation angles and convert to pan/tilt angles
        // Start by rotating vector by quaternion (camera vector = -z)
        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(pose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector vector = new ClassHelpers.mVector(0.f, 0.f, -1.f);
        vector.rotateByQuaternion(phoneRotationQuaternion);
        vector.normalise();

        // Add initial offset pose
        ClassHelpers.mQuaternion offsetRotationQuaternion = new ClassHelpers.mQuaternion(offsetPose.getRotationQuaternion());
        offsetRotationQuaternion.normalise();
        ClassHelpers.mVector offsetVector = new ClassHelpers.mVector(0.f, 0.f, -1.f);
        offsetVector.rotateByQuaternion(offsetRotationQuaternion);
        offsetVector.normalise();

        if(!isWaypointPose)
        {
            vector.x -= offsetVector.x;
            vector.y -= offsetVector.y;
        }

        return vector;
    }

    public void setTarget(long target)
    {
        policy = new Policy((int)target);

        this.target = target;
        this.targetSet = true;
        this.targetFound = false;

        metrics.updateTarget(target);
    }

    private float getPitch(double tilt)
    {
        float pitch;
        // From config file; HI setting
        int pitchHighLim = 12;
        int pitchLowLim = 6;

        // Compensate for the Tango's default position being 90deg upright
        if(tilt >= Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, 64));
        }

        else if(tilt <= -Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, pitchHighLim));
        }

        else
        {
            double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(pitchHighLim - Math.PI / 2 * grad);

            pitch = (float)(Math.pow(2, grad * -tilt + intercept));
        }
        Log.d(TAG, String.format("pitch: %f", pitch));

        return pitch;
    }

    public void setObservation(final long observation)
    {
        this.observation = observation;
        metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            callingActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String val;
                    if(observation == 13)
                    {
                        val = "Door handle";
                    }
                    else if(observation == 7)
                    {
                        val = "Mouse";
                    }
                    else if(observation == 12)
                    {
                        val = "Door";
                    }
                    else if(observation == 18)
                    {
                        val = "Laptop";
                    }
                    else if(observation == 6)
                    {
                        val = "Keyboard";
                    }
                    else if(observation == 5)
                    {
                        val = "Monitor";
                    }
                    else if(observation == 23)
                    {
                        val = "Window";
                    }
                    else if(observation == 11)
                    {
                        val = "Desk";
                    }
                    else if(observation == 22)
                    {
                        val = "Table";
                    }
                    else
                    {
                        val = "Unknown";
                    }

                    if(toast != null)
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(callingActivity, val, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    public void setOffsetPose(Pose pose) { this.offsetPose = pose; }
    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public long getTarget() { return this.target; }
    public Anchor getWaypointAnchor() { return this.waypointAnchor; }

    class Waypoint
    {
        private static final long ANGLE_INTERVAL = 15;

        private Pose pose;

        Waypoint()
        {
            pose = new Pose(new float[] {0.f, 0.f, -1.f}, new float[] {0.f, 0.f, 0.f, 1.f});
        }

        Pose getPose() { return pose; }

        void updateWaypoint(Pose phonePose, State state, long action)
        {
            float[] wayPointTranslation = new float[3];
            long[] stateVector = state.getEncodedState();

            // Assume the current waypoint is where the camera is pointing.
            // Reasonable since this function only called when pointing to new target
            ClassHelpers.mVector waypointVector = getRotation(phonePose, true);
            waypointVector.x /= waypointVector.z;
            waypointVector.y /= waypointVector.z;
            waypointVector.z /= waypointVector.z;

            if(action == Policy.A_LEFT)
            {
                wayPointTranslation[0] = waypointVector.x - 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                wayPointTranslation[1] = waypointVector.y;
                stateVector[0] += 1;
            }
            if(action == Policy.A_RIGHT)
            {
                wayPointTranslation[0] = waypointVector.x + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                wayPointTranslation[1] = waypointVector.y;
                stateVector[0] -= 1;
            }

            if(action == Policy.A_UP)
            {
                wayPointTranslation[0] = waypointVector.x;
                wayPointTranslation[1] = waypointVector.y + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                stateVector[1] -= 1;
            }
            if(action == Policy.A_DOWN)
            {
                wayPointTranslation[0] = waypointVector.x;
                wayPointTranslation[1] = waypointVector.y - 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                stateVector[1] += 1;
            }

            // waypointState = decodeState(state[0], state[1], state[2]);

            // wayPointTranslation[0] = phonePose.getTranslation()[0] - 1.f;
            // wayPointTranslation[1] = phonePose.getTranslation()[1] - 1.f;
            wayPointTranslation[2] = phonePose.getTranslation()[2] - 1.f;

            pose = new Pose(wayPointTranslation, phonePose.getRotationQuaternion());
        }

        private boolean waypointReached(float pan, float tilt)
        {
            float x = pose.getTranslation()[0];
            float y = pose.getTranslation()[1];

            return Math.abs(Math.sin(tilt) - y) < 0.1 && Math.abs(Math.cos(pan) - x) < 0.1;
        }
    }

    class State
    {
        private static final int NUM_OBJECTS = 9;
        private static final int MAX_STEPS = 10;
        private static final int HISTORY_LEN = 256;

        private static final int S_OBS = 0;
        private static final int S_STEPS = 1;
        private static final int S_HISTORY = 2;

        private long state;
        private long[] stateVector;
        private long[] primeObservation;

        private long observation = 0;
        private long steps = 0;
        private long history = 1;

        State()
        {
            state = getDecodedState();
            stateVector = getEncodedState();
            primeObservation = generatePrimeProductLookupTable();
        }

        private long getDecodedState()
        {
            long state = 0;
            long multiplier = 1;

            state += (multiplier * observation);
            multiplier *= NUM_OBJECTS;
            state += (multiplier * steps);
            multiplier *= MAX_STEPS;
            state += (multiplier * history);

            return state;
        }

        private long[] getEncodedState()
        {
            long[] stateVector = new long[3];
            stateVector[S_OBS] = state % NUM_OBJECTS;
            state /= NUM_OBJECTS;
            stateVector[S_STEPS] = state % MAX_STEPS;
            state /= MAX_STEPS;
            stateVector[S_HISTORY] = state % HISTORY_LEN;

            return stateVector;
        }

        private void addObservation(long observation)
        {
            this.observation = observation;
            this.steps ++;
            this.history *= primeObservation[(int)observation];
        }

        long[] generatePrimeProductLookupTable()
        {
            // 1 is for O_NOTHING
            long primeNumbers[] = {2, 3, 5, 7, 11, 13, 17, 19};
            long[] lookupTable = new long[HISTORY_LEN];

            int n = 8;
            int k = 0;
            int tableIndex = 0;

            for(int i = k; i < n+1; i++)
            {
                if(i == 0)
                {
                    lookupTable[tableIndex++] = 1;
                    continue;
                }
                if(i == 1)
                {
                    for(int t = 0; t < 8; t ++)
                    {
                        lookupTable[tableIndex++] = primeNumbers[t];
                    }
                    continue;
                }

                for(int t = 1; t <= choose(n, i); t++)
                {
                    int[] combination = generateCombination(n, i, t);
                    int product = 1;
                    for(int x = 0; x < i; x++)
                    {
                        product *= primeNumbers[combination[x]-1];
                    }
                    lookupTable[tableIndex++] = product;
                }
            }

            return lookupTable;
        }

        int[] generateCombination(int n, int p, int t)
        {
            int[] combination = new int[p];
            int i, r, k = 0;
            for(i = 0; i < p-1; i++)
            {
                combination[i] = ((i != 0) ? combination[i-1] : 0);
                do {
                    combination[i] ++;
                    r = choose(n - combination[i], p - (i + 1));
                    k += r;
                } while(k < t);
                k -= r;
            }
            combination[p-1] = combination[p-2] + t - k;

            return combination;
        }

        int choose(int n, int k)
        {
            // Base Cases
            if (k==0 || k==n)
                return 1;

            // Recur
            return  choose(n-1, k-1) + choose(n-1, k);
        }
    }

    class Policy
    {
        private static final int O_MUG = 6;
        private static final int O_LAPTOP = 4;
        private static final int O_WINDOW = 8;

        private static final int A_UP = 0;
        private static final int A_DOWN = 1;
        private static final int A_LEFT = 2;
        private static final int A_RIGHT = 3;

        private String fileName = "MDPPolicies/sarsa_";

        private Map<Long, ArrayList<Long>> policy = new HashMap<>();

        public Policy(int target)
        {
            switch(target)
            {
                case O_MUG:
                    this.fileName += "mug.txt";
                    break;
                case O_LAPTOP:
                    this.fileName += "laptop.txt";
                    break;
                case O_WINDOW:
                    this.fileName += "window.txt";
                    break;
            }

            BufferedReader reader = null;
            try
            {
                // Extract policy state-action pairs from text file using regex
                Pattern pattern = Pattern.compile("(\\d+)\\s(\\d)\\s(1.0|0.25)");
                reader = new BufferedReader(new InputStreamReader(callingActivity.getResources().getAssets().open(fileName)));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    // Save all non-zero prob actions into a hashmap to sample from later
                    Matcher matcher = pattern.matcher(line);
                    if(matcher.find())
                    {
                        if(Double.valueOf(matcher.group(3)) > 0.0)
                        {
                            long state = Long.valueOf(matcher.group(1));
                            long action = Long.valueOf(matcher.group(2));

                            policy.putIfAbsent(state, new ArrayList<Long>());
                            policy.get(state).add(action);
                        }
                    }
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
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "Error closing the file: " + e);
                    }
                }
            }
        }

        long getAction(State state)
        {
            // Draw random action from action set from policy
            Random rand = new Random();
            long s = state.getDecodedState();

            int nActions = policy.get(s).size();
            return policy.get(s).get(rand.nextInt(nActions));
        }
    }
}
