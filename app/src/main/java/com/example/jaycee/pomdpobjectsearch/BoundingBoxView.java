package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * The BoundingBoxView class provide view usefull to drawn the bounding box of the object found by
 * the Object detector.
 *
 * @author  Andrea Gaetano Tramontano
 * @version 1.0
 * @since   2018-10-29
 */
public class BoundingBoxView extends View {

    //the coordinates of the bounding box + the label index of the bounding box + the confidence
    // threshold of the found object
    private float[] coordinates;
    private final Paint fgPaint, textPaint, trPaint;

    String labelsName_file;

    /**
     * Constructor: The constructor initialize the global variables needed to draw the bounding box.
     *
     * @param context The actual context activity.
     * @param set The attributes of the view.
     */
    public BoundingBoxView(final Context context, final AttributeSet set) {
        super(context, set);

        labelsName_file = ObjectDetector.getPath(".names", context);

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

    }


    /**
     * The setResults method set the coordinates of the bounding box we want to draw.
     *
     * @param results The coordinates of the bounding box.
     */
    public void setResults(float[] results) {
        this.coordinates = results;
        postInvalidate();
    }


    /**
     * The onDraw method draw the bounding boxes of the found object.
     *
     * @param canvas The Canvas object needed to draw bounding boxes.
     */
    @Override
    public void onDraw(final Canvas canvas) {

        // Get view size.
        float view_height_tmp = (float) this.getHeight();
        float view_width_tmp = (float) this.getWidth();
        float view_height = Math.max(view_height_tmp, view_width_tmp);
        float view_width = Math.min(view_height_tmp, view_width_tmp);

        String prediction_string = "width: " + Float.toString(view_width) +
                " height: " + Float.toString(view_height);
        Log.v("BoundingBox", prediction_string);

        //if some object were found
        if (coordinates != null) {

            //for every object found
            for(int i=0; i<coordinates.length/6; i++) {

                float x = coordinates[(i*6)];
                float y = coordinates[(i*6)+1];
                float width = coordinates[(i*6)+2];
                float height = coordinates[(i*6)+3];

                //compute the point of the bounding box
                float p1_x = x - width / 2;
                float p1_y = y - height / 2;
                float p2_x = x + width / 2;
                float p2_y = y + height / 2;

                // Create new bounding box and draw it.
                canvas.drawRect(new RectF(p1_x, p1_y, p2_x, p2_y), fgPaint);

                ArrayList<String> labels_name = readLabelsName(labelsName_file);
                String label = labels_name.get((int)coordinates[(i*6)+4]);

                // Create the label name on the bounding box.
                float text_width = textPaint.measureText(label)/2;
                float text_size = textPaint.getTextSize();
                float text_center_x = p1_x - 2;
                float text_center_y = p1_y - text_size;
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawRect(text_center_x, text_center_y, text_center_x + 2 * text_width, text_center_y + text_size, trPaint);
                canvas.drawText(label, text_center_x + text_width, text_center_y + text_size, textPaint);

            }
        }
    }


    /**
     * The readLabelsName method read the name of the object label from the file and save them in a
     * variable.
     *
     * @param labelsName_file The file path where to read the labels name.
     *
     * @return ArrayList<String> The ArrayList where are saved the labels name.
     */
    private static ArrayList<String> readLabelsName(String labelsName_file){

        File file = new File(labelsName_file);
        Scanner sc = null;
        try{
            sc = new Scanner(file);
        }
        catch(FileNotFoundException e){

        }

        ArrayList<String> labels_name = new ArrayList<String>();

        for(int i=0; sc.hasNextLine(); i++)
            labels_name.add(sc.nextLine());

        return labels_name;

    }
}
