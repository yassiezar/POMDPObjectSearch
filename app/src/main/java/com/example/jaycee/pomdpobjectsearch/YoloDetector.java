package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class YoloDetector implements Classifier
{
    private YoloDetector() {}

    public static Classifier create(final Context context)
    {
        YoloDetector detector =  new YoloDetector();

        String cfgFilePath;
        String weightFilepat;
        float confidenceThreshold = 0;

        cfgFilePath = getPath(context, ".cfg");
        weightFilepat = getPath(context, ".weights" );

        JNIBridge.createObjectDetector(cfgFilePath, weightFilepat, confidenceThreshold);

        return detector;
    }

    @Override
    public List<Recognition> classifyImage(byte[] imageData)
    {
        return null;
    }

    private static String getPath(Context context, String fileType)
    {
        AssetManager am = context.getAssets();
        String[] pathNames = {};
        String fileName = "";
        try
        {
            pathNames = am.list("yolo");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        for ( String filePath : pathNames )
        {
            if ( filePath.endsWith(fileType))
            {
                fileName = filePath;
                break;
            }
        }
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(am.open("yolo/" + fileName));
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
