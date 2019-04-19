package com.example.jaycee.pomdpobjectsearch.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public final class PermissionHelper
{
    public static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    public static boolean hasCameraPermission(Activity activity)
    {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestCameraPermission(Activity activity)
    {
        if(ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION))
        {
            Toast.makeText(activity, "Camera permission required for this app", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(activity, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
    }

    public static void launchPermissionSettings(Activity activity)
    {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }
}
