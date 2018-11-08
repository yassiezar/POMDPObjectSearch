package com.example.jaycee.pomdpobjectsearch;

import java.util.List;

public class YoloDetector implements Classifier
{
    private YoloDetector() {}

    public static Classifier create()
    {
        YoloDetector detector =  new YoloDetector();

        String cfgFilePath;
        String weightFilepat;
        float confidence_threshold = 0;

        cfgFilePath = getPath(".cfg", this);
        weightFilepat = getPath(".weights", this);

        JNIBridge.create(cfgFilePath, weightFilepat, confidence_threshold);

        return detector;
    }

    @Override
    public List<Recognition> classifyImage(byte[] imageData)
    {
        return null;
    }

    public static String getPath(String fileType, Context context)
    {
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
