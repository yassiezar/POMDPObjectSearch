package com.example.jaycee.pomdpobjectsearch.imageprocessing;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

public class ImageConverter implements Runnable
{
    private static final String TAG = ImageConverter.class.getSimpleName();

    private Image image;

    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes;

    public ImageConverter(int width, int height)
    {
        rgbBytes = new int[width*height];
    }

    @Override
    public void run()
    {
        final Image currentImage = image;
        int width = currentImage.getWidth();
        int height = currentImage.getHeight();
        final Image.Plane[] planes = currentImage.getPlanes();

        fillBytes(planes, yuvBytes);
        int yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();

        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                width,
                height,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes);
        currentImage.close();
    }

    private void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes)
    {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            buffer.position(0);
            if (yuvBytes[i] == null) {
                Log.d(TAG, String.format("Initializing buffer %d at size %d", i, buffer.capacity()));
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public int[] getRgbBytes(Image img)
    {
        Log.d(TAG, "Getting RGB bytes");
        this.image = img;
        run();

        return rgbBytes;
    }
}
