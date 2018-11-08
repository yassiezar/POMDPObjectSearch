package com.example.jaycee.pomdpobjectsearch.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.jaycee.pomdpobjectsearch.ObjectDetector;
import com.example.jaycee.pomdpobjectsearch.Recognition;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * The BoundingBoxView class provide view usefull to drawn the bounding box of the object found by
 * the Object detector.
 *
 * @author  Andrea Gaetano Tramontano
 * @version 1.0
 * @since   2018-10-29
 */
public class BoundingBoxView extends View implements ResultsView
{
    private static final String TAG = BoundingBoxView.class.getSimpleName();

    //the coordinates of the bounding box + the label index of the bounding box + the confidence
    // threshold of the found object
    private List<Recognition> results;
    private final Paint fgPaint, textPaint, trPaint;
    private RectF bbox;

    String labelsFilePath;

    /**
     * Constructor: The constructor initialize the global variables needed to draw the bounding box.
     *
     * @param context The actual context activity.
     * @param set The attributes of the view.
     */
    public BoundingBoxView(final Context context, final AttributeSet set)
    {
        super(context, set);

        labelsFilePath = ObjectDetector.getPath(".names", context);

        //setting for the bounding boxes around the objects
        fgPaint = new Paint();
        fgPaint.setColor(0xff00ff01);
        fgPaint.setStyle(Paint.Style.STROKE);
        fgPaint.setStrokeWidth(4);

        //paint around the text with the objects name
        trPaint = new Paint();
        trPaint.setColor(0xff00ff01);
        trPaint.setStyle(Paint.Style.FILL);

        //setting for the label with the name of the objects
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextSize(50);

        bbox = new RectF();
    }

    /**
     * The onDraw method draw the bounding boxes of the found object.
     *
     * @param canvas The Canvas object needed to draw bounding boxes.
     */
    @Override
    public void onDraw(final Canvas canvas)
    {
        // Get view size.
/*        float viewHeightTmp = (float) this.getHeight();
        float viewWidthTmp = (float) this.getWidth();
        float viewHeight = Math.max(viewHeightTmp, viewWidthTmp);
        float viewWidth = Math.min(viewHeightTmp, viewWidthTmp);

        Log.v(TAG, String.format("BoundingBox width %f height %f", viewWidth, viewHeight));*/

        //if some object were found
        if (results != null && results.size() > 0)
        {
            //for every object found
            for(Recognition result : results)
            {
                // Create new bounding box and draw it.
                bbox.set(result.getLocation());
                canvas.drawRect(bbox, fgPaint);

                ArrayList<String> labelNames = readLabelsName(labelsFilePath);
                String label = labelNames.get(result.getId());

                // Create the label name on the bounding box.
                float textWidth = textPaint.measureText(label)/2;
                float textSize = textPaint.getTextSize();
                float textCentreX = bbox.centerX() - 2;
                float textCentreY = bbox.centerY() - textSize;
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawRect(textCentreX, textCentreY, textCentreX + 2 * textWidth, textCentreY + textSize, trPaint);
                canvas.drawText(label, textCentreX + textWidth, textCentreY + textSize, textPaint);
            }
        }
    }

    /**
     * The readLabelsName method read the name of the object label from the file and save them in a
     * variable.
     *
     * @param labelsFilePath The file path where to read the labels name.
     *
     * @return ArrayList<String> The ArrayList where are saved the labels name.
     */
    private static ArrayList<String> readLabelsName(String labelsFilePath)
    {
        File file = new File(labelsFilePath);
        Scanner sc = null;
        try
        {
            sc = new Scanner(file);
        }
        catch(FileNotFoundException e)
        {
            Log.w(TAG, "Label file not found " + e);
        }

        ArrayList<String> labelNames = new ArrayList<String>();

        for(int i=0; sc.hasNextLine(); i++)
            labelNames.add(sc.nextLine());

        return labelNames;

    }

    /**
     * The setResults method set the coordinates of the bounding box we want to draw.
     *
     * @param results The coordinates of the bounding box.
     */
    @Override
    public void setResults(List<Recognition> results)
    {
        this.results = results;
        postInvalidate();
    }
}
