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

    public static Classifier create(final AssetManager am)
    {
        YoloDetector detector =  new YoloDetector();

        String cfgFilePath;
        String weightFilepat;
        float confidenceThreshold = 0;

        cfgFilePath = getPath(am, ".cfg");
        weightFilepat = getPath(am, ".weights" );

        JNIBridge.createObjectDetector(cfgFilePath, weightFilepat, confidenceThreshold);

        return detector;
    }

    @Override
    public List<Recognition> classifyImage(byte[] imageData)
    {
        return null;
    }

    private static String getPath(AssetManager am, String fileType)
    {
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
        return fileName;
    }
}
