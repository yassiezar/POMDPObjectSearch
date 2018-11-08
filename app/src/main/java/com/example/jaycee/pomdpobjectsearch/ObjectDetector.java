package com.example.jaycee.pomdpobjectsearch;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.example.jaycee.pomdpobjectsearch.views.BoundingBoxView;


/**
 * The ObjectDetector class provide the functions to create and use a Deep Neural Network based on
 * YOLOv3 algorithm (an object detector). It use native c++ code to reach the purpose (see
 * ObjectDetector.cpp).
 *
 * @author  Andrea Gaetano Tramontano
 * @version 1.0
 * @since   2018-10-29
 */
public class ObjectDetector implements Runnable
{
    private static final String TAG = ObjectDetector.class.getSimpleName();
    private static final int O_NOTHING = 0;

    //index of the found object. code=0 if no objects were found.
    private int objectCode = O_NOTHING;

    private Handler handler = new Handler();

    private boolean stop = false;

    private Bitmap bitmap;
    private SurfaceRenderer renderer;

    //variable that count the number of frame, useful to decide how many FPS we want to compute.
    private int frameCounter = 0;
    private BoundingBoxView boundingBoxView;

    /**
     * Constructor: The constructor initialize the global variables and call the native method for the DNN creation.
     *
     * @param context The actual context activity.
     * @param frameWidth This is the width of the analyzed frame.
     * @param frameWidth This is the height of the analyzed frame.
     * @param renderer This is the address of the SurfaceRenderer object, used to take the actual
     *                 frame.
     * @param bbv The bounding box view where will be drawn the bounding box of the object.
     *
     */
    public ObjectDetector(Context context, int frameWidth, int frameHeight, SurfaceRenderer renderer,
                          BoundingBoxView bbv)
    {
        this.boundingBoxView = bbv;
        this.renderer = renderer;
        //the bitmap where we will read the actual frame
        this.bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);

        String cfgFilePath = getPath(".cfg", context);
        String weightFilepat = getPath(".weights", context);
        float confidence_threshold = 0;

        //this method call the native code for the DNN creation
        JNIBridge.createObjectDetector(cfgFilePath, weightFilepat, confidence_threshold);
    }


    /**
     * The run method start the thread: take a new frame, call the native code for analyze the frame,
     * look for found object and save the results in a variable.
     */
    @Override
    public void run()
    {
        int cameraFPS = 30;
        //we decide to compute 2 FPS
        int yoloFPS = 2;

        if(frameCounter%(cameraFPS/yoloFPS) != 0)
        {
            boundingBoxView.setResults(null);
            boundingBoxView.invalidate();

            handler.postDelayed(this, 40);

            return;
        }
        objectCode = O_NOTHING;
        //read the actual frame
        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());

        Mat inputFrame = new Mat();
        Utils.bitmapToMat(bitmap, inputFrame);

        double time = -1;

        //call for the native classification method
        float[] objectResults = JNIBridge.classify(inputFrame.getNativeObjAddr());

        int resultLength = objectResults.length;
        //we look if there are found object (result_length > 5) and if the array is correctly
        // set (result_length % 6 == 0). Remember that for every object we have 6 parameters.
        if (resultLength > 5 && resultLength % 6 == 0)
        {
            //give the results to the bounding box view
            // boundingBoxView.setResults(objectResults);
            //update bounding box view
            boundingBoxView.invalidate();

            Log.v(TAG, String.format("Time: %f s - Number of found objects: %d ", time, resultLength/6));

            int numFoundObjects = resultLength / 6;

            //connect every object found with the correct code
            //TODO: delete this part, because when we will have the our trained model, we don't
            //TODO: need to make this operation
            for (int i = 0; i < numFoundObjects; i++)
            {
                int idx = (int) objectResults[(i * 6) + 4] + 1;

                Log.v(TAG, String.format("Index: %d", idx));

                objectCode = (int)objectResults[(i * 6) + 4];

                switch (idx)
                {
                    case 1: //person
                        objectCode = 1;
                        break;
                    case 25: //backpack
                        objectCode = 2;
                        break;
                    case 57: //chair
                        objectCode = 3;
                        break;
                    case 63: //tvmonitor
                        objectCode = 4;
                        break;
                    case 64: //laptop
                        objectCode = 5;
                        break;
                    case 65: //mouse
                        objectCode = 6;
                        break;
                    case 67: //keyboard
                        objectCode = 7;
                        break;
                    default: objectCode = O_NOTHING;
                }
            }
        }
    frameCounter++;

    if(!stop)
        handler.postDelayed(this, 40);
    }

    /**
     * The stop method stop the thread.
     */
    public void stop()
    {
        this.stop = true;
        handler = null;
    }


    /**
     * The getCode method return the actual code of the found object.
     *
     * @return int The actual code.
     */
    public int getCode()
    {
        return this.objectCode;
    }

    //TODO: I would like to delete this method and use direct path of the assets directory
    @SuppressLint("LongLogTag")
    /**
     * This method take file from asset directory, read them and save them in a different directory:
     * File. So the file could be accessible from the native code.
     */
    public static String getPath(String fileType, Context context) {
        AssetManager assetManager = context.getAssets();
        String[] pathNames = {};
        String fileName = "";
        try {
            pathNames = assetManager.list("yolo");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for ( String filePath : pathNames ) {
            if ( filePath.endsWith(fileType)) {
                fileName = filePath;
                break;
            }
        }
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open("yolo/" + fileName));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();

            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), fileName);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            //Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }
}
