package com.example.jaycee.pomdpobjectsearch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.jaycee.pomdpobjectsearch.helpers.PermissionHelper;

public class ActivityEntry extends AppCompatActivity
{
    private static final int ACTIVITY_GUIDED = 1;
    private static final int ACTIVITY_UNGUIDED = 2;

    private int activityToLaunch = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        findViewById(R.id.button_guided).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activityToLaunch = ACTIVITY_GUIDED;
                if(!PermissionHelper.hasCameraPermission(ActivityEntry.this))
                {
                    PermissionHelper.requestCameraPermission(ActivityEntry.this);
                    return;
                }
                startActivity(new Intent(ActivityEntry.this, ActivityGuided.class));
            }
        });
        findViewById(R.id.button_unguided).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activityToLaunch = ACTIVITY_UNGUIDED;
                if(!PermissionHelper.hasCameraPermission(ActivityEntry.this))
                {
                    PermissionHelper.requestCameraPermission(ActivityEntry.this);
                    return;
                }
                startActivity(new Intent(ActivityEntry.this, ActivityUnguided.class));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results)
    {
        if(requestCode == PermissionHelper.CAMERA_PERMISSION_CODE)
        {
            if(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            {
                launchActivity();
            }
            else
            {
                PermissionHelper.requestCameraPermission(this);
            }
        }
    }

    public void launchActivity()
    {
        switch(activityToLaunch)
        {
            case ACTIVITY_GUIDED: startActivity(new Intent(ActivityEntry.this, ActivityGuided.class));
            case ACTIVITY_UNGUIDED: startActivity(new Intent(ActivityEntry.this, ActivityUnguided.class));
        }
    }
}
