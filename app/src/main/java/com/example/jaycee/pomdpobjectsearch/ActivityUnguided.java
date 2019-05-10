package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.imageprocessing.ObjectClassifier;
import com.example.jaycee.pomdpobjectsearch.CameraSurface.ScreenReadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityUnguided extends ActivityBase implements ScreenReadRequest
{
    private static final String TAG = ActivityUnguided.class.getSimpleName();

    private static final float MIN_CONF = 0.4f;

    private TextToSpeech tts;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        surfaceView.enableScreenTap();
        surfaceView.setScreenReadRequest(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        tts = new TextToSpeech(ActivityUnguided.this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status == TextToSpeech.SUCCESS)
                {
                    int result = tts.setLanguage(Locale.UK);
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.e("error", "This Language is not supported");
                    }
                }
                else
                {
                    Log.e("error", "Initialisation Failed!");
                }
            }
        });
    }

    @Override
    public void onPause()
    {
        if(tts != null)
        {
            tts.shutdown();
        }

        super.onPause();
    }

    @Override
    public void onScreenTap()
    {
        Log.d(TAG, "Tapped screen");
        scanFrameForObjects();
    }

    @Override
    public void onNewFrame(com.google.ar.core.Frame frame)
    {
        super.onNewFrame(frame);

        getMetrics().updateTimestamp(frame.getTimestamp());
        getMetrics().updatePhonePose(frame.getAndroidSensorPose());

        getMetrics().writeWifi();
    }

    @Override
    public void setTarget(Objects.Observation target)
    {
        super.setTarget(target);
        getMetrics().updateTarget(target);
    }


    @Override
    public void onScanComplete(List<ObjectClassifier.Recognition> results)
    {
        RectF centreOfScreen = new RectF(0, 0, 300, 300);
        ArrayList<String> previousUtterances = new ArrayList<>();
        ArrayList<Objects.Observation> validObservations = new ArrayList<>();

        for(ObjectClassifier.Recognition result : results)
        {
            Log.i(TAG, result.toString());
            if(result.getConfidence() > MIN_CONF && !previousUtterances.contains(result.getTitle()) && centreOfScreen.contains(result.getLocation()))
            {
                Log.i(TAG, result.getObservation().getFileName() + getTarget().getFileName());
                tts.speak(result.getTitle(), TextToSpeech.QUEUE_ADD, null, "");
                previousUtterances.add(result.getTitle());
                validObservations.add(result.getObservation());
                if(result.getObservation() == getTarget())
                {
                    getVibrator().vibrate(350);
                    // setTarget(Objects.Observation.O_NOTHING);
                }
                Log.d(TAG, result.toString());
            }
        }
        if(validObservations.isEmpty())
        {
            getMetrics().updateObservation(Objects.Observation.O_NOTHING);
        }
        else
        {
            getMetrics().updateObservation(validObservations);
        }
    }
}
