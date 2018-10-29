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

    private static final String TAG = "OBJECT_DETECTOR";
    private static final int O_NOTHING = 0;

    //index of the found object. code=0 if no objects were found.
    private int code = O_NOTHING;

    private Handler handler = new Handler();
    private boolean stop = false;

    private Bitmap bitmap;
    private SurfaceRenderer renderer;

    //variable that count the number of frame, useful to decide how many FPS we want to compute.
    private int counter;
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

        boundingBoxView = bbv;
        this.renderer = renderer;
        //the bitmap where we will read the actual frame
        this.bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);

        String cfg_file = getPath(".cfg", context);
        String weigth_file = getPath(".weights", context);
        float confidence_threshold = 0;

        //this method call the native code for the DNN creation
        JNIBridge.create(cfg_file, weigth_file, confidence_threshold);

        counter = 0;
    }


    /**
     * The run method start the thread: take a new frame, call the native code for analyze the frame,
     * look for found object and save the results in a variable.
     */
    @Override
    public void run()
    {

        code = O_NOTHING;
        //read the actual frame
        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());

        Mat input_frame = new Mat();
        Utils.bitmapToMat(bitmap, input_frame);

        double time = -1;

        int camera_FPS = 30;
        //we decide to compute 2 FPS
        int yolo_FPS = 2;

        if(counter%(camera_FPS/yolo_FPS) == 0) {

            //call for the native classification method
            float[] results = JNIBridge.classify(input_frame.getNativeObjAddr());

            int result_length = results.length;
            //we look if there are found object (result_length > 5) and if the array is correctly
            // set (result_length % 6 == 0). Remember that for every object we have 6 parameters.
            if (result_length > 5 && result_length % 6 == 0) {

                //give the results to the bounding box view
                boundingBoxView.setResults(results);
                //update bounding box view
                boundingBoxView.invalidate();

                String t = "Time: " + time + " s - Number of found objects: " + result_length / 6;
                Log.v(TAG, t);

                int num_foundObject = result_length / 6;

                //connect every object found with the correct code
                //TODO: delete this part, because when we will have the our trained model, we don't
                //TODO: need to make this operation
                for (int i = 0; i < num_foundObject; i++) {

                    int idx = (int) results[(i * 6) + 4] + 1;

                    t = "Index: " + idx;
                    Log.v(TAG, t);

                    code = (int) results[(i * 6) + 4];

                    switch (idx) {
                        case 1: //person
                            code = 1;
                            break;
                        case 25: //backpack
                            code = 2;
                            break;
                        case 57: //chair
                            code = 3;
                            break;
                        case 63: //tvmonitor
                            code = 4;
                            break;
                        case 64: //laptop
                            code = 5;
                            break;
                        case 65: //mouse
                            code = 6;
                            break;
                        case 67: //keyboard
                            code = 7;
                            break;
                    }

                }

            }
            else {
                boundingBoxView.setResults(null);
                boundingBoxView.invalidate();
            }

        }
        counter++;

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
    public int getCode() {
        return this.code;
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


