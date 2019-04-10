package com.example.jaycee.pomdpobjectsearch;

import android.graphics.RectF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class ActivityUnguided extends ActivityBase implements ScreenReadRequest
{
    private static final String TAG = ActivityUnguided.class.getSimpleName();

    private static final float MIN_CONF = 0.2f;

    private TextToSpeech tts;

    /* TODO:
    Detect object in middle of screen
    Add text to speech to read objects in middle on screen tap
     */

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
            tts.shutdown();
        }

        super.onPause();
    }

    @Override
    public void onScreenTap()
    {
        Log.d(TAG, "Tapped screen");
        tts.speak("Screen tapped", TextToSpeech.QUEUE_FLUSH, null, "");
        scanFrameForObjects();
    }

    @Override
    public void onScanComplete(List<ObjectClassifier.Recognition> results)
    {
        RectF centreOfScreen = new RectF(213, 160, 416, 320);
        for(ObjectClassifier.Recognition result : results)
        {
            if(result.getConfidence() > MIN_CONF && centreOfScreen.contains(result.getLocation()))
            {
                tts.speak(result.getTitle(), TextToSpeech.QUEUE_FLUSH, null, "");
                Log.d(TAG, result.toString());
            }
        }
    }
}
