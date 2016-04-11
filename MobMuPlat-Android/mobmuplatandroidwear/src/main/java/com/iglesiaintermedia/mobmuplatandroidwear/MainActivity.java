package com.iglesiaintermedia.mobmuplatandroidwear;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.MessageApi.MessageListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class MainActivity extends WearableActivity implements ControlDelegate, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
	
	private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MESSAGE = "Hello Wear!";

    private GoogleApiClient mGoogleApiClient;

    GridViewPager pager;
    SampleGridPagerAdapter adapter;
    Handler refreshHandler = new Handler(Looper.getMainLooper());
    MessageReceiver messageReceiver;
    String currentLoadedMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Resources res = getResources();
        pager = (GridViewPager) findViewById(R.id.pager);
        pager.setOffscreenPageCount(8);
        //pager.setBackgroundColor(getPatchBackgroundColor());
        adapter = new SampleGridPagerAdapter(this, getFragmentManager());

        /*String patchString = getTestJsonString();
        setBGColorFromJSONString(patchString);
        adapter.loadGUI(patchString);*/

        String loadingGUIString =
                "{\"backgroundColor\":[0.4,0.4,0.4,1],\"wearGui\":[ {\"title\":\"MobMuPlat on a watch!\",\"pageGui\":{\"address\":\"foo\",\"color\":[1,1,1,1],\"class\":\"MMPLabel\",\"text\":\"Open a watch-enabled file within MobMuPlat\",\"textSize\":32} } ] }";
        setBGColorFromJSONString(loadingGUIString);
        adapter.loadGUI(loadingGUIString);

        pager.setAdapter(adapter);

        pager.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Adjust page margins:
                //   A little extra horizontal spacing between pages looks a bit
                //   less crowded on a round display.
                final boolean round = insets.isRound();
                int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                int colMargin = res.getDimensionPixelOffset(round ?
                        R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                pager.setPageMargins(rowMargin, colMargin);
                return insets;
            }
        });

      mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Register the local broadcast receiver, defined in step 3.
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) { // called from listener startActivity when already running.
        //Log.i("MMP", "On newintent===");
        super.onNewIntent(intent);
        //setIntent(intent); //setter for getIntent(), necc?
        final String address = intent.getStringExtra("path");
        final String message = intent.getStringExtra("message");
        if (address != null && address.equals("/loadGUI") && message != null) {
            loadGUI(message);
        }
    }

    @Override
    protected void onStart() {
        //Log.i("MMP", "On start===");
        super.onStart();
        mGoogleApiClient.connect();

        Intent intent = getIntent();
        final String address = intent.getStringExtra("path");
        final String message = intent.getStringExtra("message");
        if (address != null && address.equals("/loadGUI") && message != null) {
            loadGUIOnMainThread(message);
        }
    }

    private void loadGUIOnMainThread(final String message) {
        refreshHandler.post(new Runnable() { //combine with below
            public void run() {
                loadGUI(message);
            }
        });
    }

    private void loadGUI(final String message) {
        if (message == null || !isValidJSON(message)) return;
        if (message.equals(currentLoadedMessage)) { //redundant load
            return;
        }
        currentLoadedMessage = message;
        setBGColorFromJSONString(message);
        adapter.loadGUI(message);
        pager.setAdapter(adapter); //reset adapter to trigger layout.
    }

        public class MessageReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.i("MMP", "On receive===");
                final String address = intent.getStringExtra("path");
                final String message = intent.getStringExtra("message");
                //Log.v("MMP", "Main activity received path message: " + address + " " + message);

                if (address.equals("/startActivity")) {
                    //handled by datalsyerlistenerservice, if this activity is catching it we don't need it
                    return;
                } else if (address.equals("/loadGUI")) { //Shouldn't hit this anymore...
                    loadGUI(message);
                } else { //gui message
                    refreshHandler.post(new Runnable() {
                        public void run() {
                            String[] messageStringArray = message.split(" ");
                            List<Object> objList = new ArrayList<Object>();
                            for (String token : messageStringArray) {
                                try {
                                    Float f = Float.valueOf(token);
                                    objList.add(f);
                                } catch (NumberFormatException e) {
                                    // not a number, add as string.
                                    objList.add(token);
                                }
                            }
                            adapter.passMessage(address, objList);
                        }
                    });
                }
            }
        }
	
	private String getTestJsonString(){
		AssetManager assetManager = getAssets();

		try {
			InputStream is = assetManager.open("testJSON.txt");
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			
			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
			
			is.close();		
			return writer.toString();
		} catch (IOException e) {
			//Log.i("WEAR", "Unable to open file: "+e.getMessage());
			return null;
		} 
	}
	
	//@Override
	/*public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/
	
	 /**
     * Returns a GoogleApiClient that can access the Wear API.
     * @param context
     * @return A GoogleApiClient that can make calls to the Wear API
     */
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    /*private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                client.disconnect();
            }
        }).start();
    }*/

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    /*private void sendToast() {
        //if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                    for (Node node : nodes.getNodes()) {
                        SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();

                        //client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    //Wearable.MessageApi.sendMessage(client, nodeId, MESSAGE, null);
                    //client.disconnect();
                }
            }).start();
        }
    }*/
    
    /*class WorkerThread extends Thread {
        public Handler mHandler;

        public void run() {	 
            Looper.prepare();
             mHandler = new Handler();
            Looper.loop();
        }
    }*/
	//@Override
	public void sendGUIMessageArray(List<Object> msgArray) {
		if (msgArray.size() == 0) return;
		final String path = (String)msgArray.remove(0); //remove address
		String message = TextUtils.join(" ", msgArray.toArray()); //rest is turned into list, delimited by space.
		//final byte[] data = message.getBytes(Charset.forName("UTF-8"));
        sendWearMessage(path, message);
		/*wt.mHandler.post(new Runnable() {
			  @Override
			  public void run() {  
				  //String message = (String) msg.obj;
				if (client.isConnected()==false)client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
              	Wearable.MessageApi.sendMessage(client, nodeId, path, data);
              	//Log.i("WEAR", "client = "+client.isConnected());
			  }
		});*/
	}

    public void sendWearMessage(String inPath, String message) {
        new SendToDataLayerThread(inPath, message).start();
    }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message!=null?message.getBytes():null).await();
                if (result.getStatus().isSuccess()) {
                    //Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    //Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }

	
	private void setBGColorFromJSONString(String dataString) {
		try {
			JsonParser parser = new JsonParser();
			JsonObject topDict = parser.parse(dataString).getAsJsonObject();//top dict=wear - exception on bad JSON

			//bg color	
			if(topDict.getAsJsonArray("backgroundColor")!=null){
				JsonArray colorArray = topDict.getAsJsonArray("backgroundColor");
				//if(colorArray.size()==4)
					int bgColor = colorFromRGBAArray(colorArray);
					pager.setBackgroundColor(bgColor);
			}
		} catch(JsonParseException e) {
		//showAlert("Unable to parse interface file.");
		}
	}
	static int colorFromRGBAArray(JsonArray rgbaArray){
		return Color.argb((int)(rgbaArray.get(3).getAsFloat()*255), (int)(rgbaArray.get(0).getAsFloat()*255), (int)(rgbaArray.get(1).getAsFloat()*255), (int)(rgbaArray.get(2).getAsFloat()*255)  );
	}
	static boolean isValidJSON(String string){
		try {
			JsonParser parser = new JsonParser();
			JsonObject topDict = parser.parse(string).getAsJsonObject();
			return true;
		} catch(JsonParseException e) {
			//showAlert("Unable to parse interface file.");
				return false;
		}
	}

    //wear connection
    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
        //String message = "Hello MobMuPlat\n Via the data layer";
        //Requires a new thread to avoid blocking the UI
        //new SendToDataLayerThread("/message_path", message).start();
        //sendWearMessage("/startActivity", null);
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }
	
}
