package com.iglesiaintermedia.mobmuplatandroidwear;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listens to DataItems and Messages from the local node.
 */
public class DataLayerListenerService extends WearableListenerService {

    //private static final String TAG = "DataLayerListenerService";
    // Match what the app sends on connection.
    //private static final String START_ACTIVITY_PATH = "/startActivity";
    private static final String LOAD_GUI_PATH = "/loadGUI";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        /*if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) { // start activity
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);// Don't launch second instance on top..
            startActivity(startIntent);
        } else*/ if (messageEvent.getPath().equals(LOAD_GUI_PATH)) { //start activity and load gui
            Intent startAndLoadIntent = new Intent(this, MainActivity.class);
            startAndLoadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startAndLoadIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Don't launch second instance on top. Send onNewIntent instead.
            final String path = messageEvent.getPath(); // "/loadGUI"
            final String message = new String(messageEvent.getData());
            startAndLoadIntent.setAction(Intent.ACTION_SEND);
            startAndLoadIntent.putExtra("path", path);
            startAndLoadIntent.putExtra("message", message);
            startActivity(startAndLoadIntent);
        } else {
            final String path = messageEvent.getPath();
            final String message = new String(messageEvent.getData());

            // Broadcast message to wearable activity for display
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("path", path);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
    }
}
