package com.example.jaycee.pomdpobjectsearch.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FrameScanner
{
    private static final String TAG = FrameScanner.class.getSimpleName();

    private static final String TF_MODEL_FILE = "mobilenet/office_detect.tflite";
    private static final String TF_LABELS_FILE = "file:///android_asset/mobilenet/office_labels_list.txt";
    private static final boolean TF_IS_QUANTISED = false;
    private static final boolean MAINTAIN_ASPECT_RATIO = false;
    private static final int TF_INPUT_SIZE = 300;

    private ObjectClassifier detector;

    private FrameHandler frameHandler;

    private Bitmap fullsizeBitmap;
    private Bitmap croppedBitmap;

    private Matrix frameToCropTransform;

    private Lock lock = new ReentrantLock();

    private int width, height;

    public FrameScanner(int width, int height, Context context)
    {
        this.width = width;
        this.height = height;

        frameHandler = (FrameHandler)context;

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
        try
        {
            lock.lock();
            fullsizeBitmap.setPixels(img, 0, width, 0, 0, width, height);

            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(fullsizeBitmap, frameToCropTransform, null);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void scanFrame()
    {
        try
        {
            lock.lock();
            Log.d(TAG, "Detecting objects");
            List<ObjectClassifier.Recognition> results = new ArrayList<>(detector.recogniseImage(croppedBitmap));
            frameHandler.onScanComplete(results);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void close()
    {
        detector.close();
    }
}
