package com.example.jaycee.pomdpobjectsearch;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class IntentServiceMDP extends IntentService
{
    private static final String TAG = IntentServiceMDP.class.getSimpleName();

    public IntentServiceMDP()
    {
        super("IntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle bundle = intent.getExtras();
        if(bundle != null)
        {
            Messenger messenger = (Messenger)bundle.get("HANDLER_MESSENGER");
            Message msg = Message.obtain();

            Bundle reply = new Bundle();
            reply.putBoolean("BOOLEAN_MDP_LEARNING", true);
            reply.putBoolean("BOOLEAN_MDP_LEARNED", false);
            msg.setData(reply);

            try
            {
                messenger.send(msg);
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "Could not send message to handler. ");
            }
        }

        Log.d(TAG, "Starting MDP solver");
        JNIBridge.initSearch(intent.getIntExtra("INT_TARGET", 0), 500);
        Toast.makeText(this, "Done!", Toast.LENGTH_LONG).show();

        if(bundle != null)
        {
            Messenger messenger = (Messenger)bundle.get("HANDLER_MESSENGER");
            Message msg = Message.obtain();

            Bundle reply = new Bundle();
            reply.putBoolean("BOOLEAN_MDP_LEARNING", false);
            reply.putBoolean("BOOLEAN_MDP_LEARNED", true);
            msg.setData(reply);

            try
            {
                messenger.send(msg);
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "Could not send message to handler. ");
            }
        }
    }
}
