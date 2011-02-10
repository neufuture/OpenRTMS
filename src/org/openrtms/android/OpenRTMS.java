package org.openrtms.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.openrtms.android.R;

import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OpenRTMS extends Activity implements LocationListener{
    /** Called when the activity is first created. */
	
	static final String tag = "Main"; // for Log
	
	// change this to your Bluetooth device address 
	private static final String DEVICE_ADDRESS =  "";
	private ArduinoReceiver arduinoReceiver = new ArduinoReceiver();
	
	String pachubeKey = "";
	String pachubeFeed = "";
	String postURL = "http://www.openrtms.org/pachube/post.php";
	//String postURL = "http://www.neufuture.com/test/android/post.php";

	double lat = 0;
	double lon = 0;
	int delayTime = 5000;
	String val;
	
	StringBuilder sb;

	Timer myTimer;
	int count = 0;
	TextView txtInfo;
	
	LocationManager lm;
	int locationCurrent;
	boolean dataPosted;
	
   boolean CheckboxPreference;
   String ListPreference;
   String editTextPreference;
   String ringtonePreference;
   String secondEditTextPreference;
   String customPref;
   Button prefBtn;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        txtInfo = (TextView) this.findViewById(R.id.textInfo);
	//	prefBtn = (Button)   this.findViewById(R.id.prefButton);

        
        myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
		}, 0, delayTime);
		
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 10f, this);

		
    }
    
    public void prefClick(){
    	Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
        startActivity(settingsActivity);
    }
    
    public void onStart(Bundle savedInstanceState) {
    	getPrefs();
    }
    
    private void getPrefs() {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
        CheckboxPreference = prefs.getBoolean("checkboxPref", true);
        ListPreference = prefs.getString("listPref", "nr1");
        editTextPreference = prefs.getString("editTextPref",
                        "Nothing has been entered");
        ringtonePreference = prefs.getString("ringtonePref",
                        "DEFAULT_RINGTONE_URI");
        secondEditTextPreference = prefs.getString("SecondEditTextPref",
                        "Nothing has been entered");
        // Get the custom preference
        SharedPreferences mySharedPreferences = getSharedPreferences(
                        "myCustomSharedPrefs", Activity.MODE_PRIVATE);
        customPref = mySharedPreferences.getString("myCusomPref", "");
    }
    
    /***********  TIMER *************/
    private void TimerMethod(){
		//This method is called directly by the timer
		//and runs in the same thread as the timer.

		//We call the method that will work with the UI
		//through the runOnUiThread method.
		this.runOnUiThread(Timer_Tick);
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			count++;
			getLocation();
			postData();
			updateText();
		}
	};
	
	/***********  DATA POST *************/
	public void postData() {  
	    // Create a new HttpClient and Post Header  
	    HttpClient httpclient = new DefaultHttpClient();  
	    HttpPost httppost = new HttpPost(postURL);  
	 	    
	    try {  
	        // Add your data  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);  
			nameValuePairs.add(new BasicNameValuePair("key",  pachubeKey ));  
	        nameValuePairs.add(new BasicNameValuePair("feed", pachubeFeed ));
	        nameValuePairs.add(new BasicNameValuePair("values", lat + "," + lon + "," + val));  

	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	  
	        // Execute HTTP Post Request  
	        HttpResponse response = httpclient.execute(httppost); 
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block  
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block  
	    } 
	    
	}
	
	/***********  GPS  *************/
	
	public void getLocation(){
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			lat = location.getLatitude();
			lon = location.getLongitude();
		} else {
		// put alt here
		}
	}
	
	public void onLocationChanged(Location location) {
		locationCurrent = count;
	}
	
	public void updateText() {
		Log.v(tag, "Text updated");
		sb = new StringBuilder(512);
		
		/* display some of the data in the TextView */
		sb.append("Latitude: ");
		sb.append(lat);
		sb.append('\n');
		
		sb.append("Londitude: ");
		sb.append(lon);
		sb.append('\n');

		sb.append("value: ");
		sb.append(val);
		sb.append('\n');
		
		sb.append("Number of values sent: ");
		sb.append(Boolean.toString(dataPosted));
		sb.append('\n');
		
		sb.append("Number of values attempted: ");
		sb.append(Integer.toString(count));
		sb.append('\n');

		sb.append("Last current GPS: ");
		sb.append(Integer.toString(locationCurrent));
		sb.append('\n');
		

		txtInfo.setText(sb.toString());
	}
	
	public void onProviderDisabled(String provider) {
		/* this is called if/when the GPS is disabled in settings */
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	public void onProviderEnabled(String provider) {
		Log.v(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service", Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	/***********  Amarino  *************/

	@Override
	protected void onStart() {
		super.onStart();
		// in order to receive broadcasted intents we need to register our receiver
		registerReceiver(arduinoReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
		
		// this is how you tell Amarino to connect to a specific BT device from within your own code
		Amarino.connect(this, DEVICE_ADDRESS);
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		// if you connect in onStart() you must not forget to disconnect when your app is closed
		Amarino.disconnect(this, DEVICE_ADDRESS);
		
		// do never forget to unregister a registered receiver
		unregisterReceiver(arduinoReceiver);
	}
	
	// ArduinoReceiver is responsible for catching broadcasted Amarino events.
	// It extracts data from the intent and updates the graph accordingly.

	public class ArduinoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = null;
			
			// the device address from which the data was sent, we don't need it here but to demonstrate how you retrieve it
			final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			
			// the type of data which is added to the intent
			final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);
			
			// we only expect String data though, but it is better to check if really string was sent
			// later Amarino will support differnt data types, so far data comes always as string and
			// you have to parse the data to the type you have sent from Arduino, like it is shown below
			if (dataType == AmarinoIntent.STRING_EXTRA){
				data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);
				
				if (data != null){
					val = data;

					try {
						// since we know that our string value is an int number we can parse it to an integer
		
					} 
					catch (NumberFormatException e) { /* oh data was not an integer */ }
				}
			}
		}
	}	
}





