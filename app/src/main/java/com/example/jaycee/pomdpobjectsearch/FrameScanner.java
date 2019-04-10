package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.ImageUtils;

import java.io.IOException;
import java.util.List;

public class FrameScanner
{
    private static final String TAG = FrameScanner.class.getSimpleName();

    private static final String TF_MODEL_FILE = "mobilenet/detect.tflite";
    private static final String TF_LABELS_FILE = "file:///android_asset/mobilenet/coco_labels_list.txt";
    private static final boolean TF_IS_QUANTISED = true;
    private static final boolean MAINTAIN_ASPECT_RATIO = false;
    private static final int TF_INPUT_SIZE = 300;
    private static final float MIN_CONF = 0.2f;

    private ObjectClassifier detector;

    private Bitmap fullsizeBitmap;
    private Bitmap croppedBitmap;

    private Matrix frameToCropTransform;

    private List<ObjectClassifier.Recognition> results;

    private int width, height;

    public FrameScanner(int width, int height, Context context)
    {
        this.width = width;
        this.height = height;

        try
        {
            detector = ObjectDetector.create(context.getAssets(), TF_MODEL_FILE, TF_LABELS_FILE, TF_INPUT_SIZE, TF_IS_QUANTISED);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Object detector init error: Cannot read file " + e);
        }

        // TODO: Add compensation for other screen rotations
        int sensorOrientation = 90;      // Assume 0deg rotation for now

        fullsizeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(TF_INPUT_SIZE, TF_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        width, height,
                        TF_INPUT_SIZE, TF_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT_RATIO);

        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    public void updateBitmap(int[] img)
    {
        fullsizeBitmap.setPixels(img, 0, width, 0, 0, width, height);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(fullsizeBitmap, frameToCropTransform, null);
    }

    public void scanFrame()
    {
        Log.d(TAG, "Detecting objects");
        results = detector.recogniseImage(croppedBitmap);

        for(ObjectClassifier.Recognition rec : results)
        {
            if(rec.getConfidence() > MIN_CONF)
            {
                Log.d(TAG, rec.toString());
            }
        }
    }

    public void close()
    {
        detector.close();
    }

    public List<ObjectClassifier.Recognition> getDetectedObjects()
    {
        return this.results;
    }
}
