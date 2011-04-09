package org.openrtms.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import Pachube.PachubeException;
import Pachube.httpClient.HttpClient;
import Pachube.httpClient.HttpMethod;
import Pachube.httpClient.HttpRequest;
import Pachube.httpClient.HttpResponse;

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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class OpenRTMS extends MapActivity implements LocationListener{	
	static final String tag = "Main"; // for Log
	
	// Bluetooth device address 
//	private static final String DEVICE_ADDRESS = "";

	private ArduinoReceiver arduinoReceiver = new ArduinoReceiver();
	
	// Pachube Values
	HttpClient client = new HttpClient("www.pachube.com");
	String pachubeKey;
	int pachubeFeed;
	String pachubeLogin = "";
	String pachubePass;

	String val;
	
	// Interval delay
	int interval = 10;
	Timer myTimer = new Timer();
	boolean isRecording = false;
	boolean runBackground = false;
	
	StringBuilder sb;
	int count = 0;
	TextView txtInfo;
	Button record;
	Button changeView;
	
	// GPS Values
	LocationManager lm;
	private MapController mapController;
	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;
	int locationCurrent;
	Location location;
	double lat;
	double lon;
	boolean gpsRecord;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		mapView = (MapView) findViewById(R.id.mapview);

        txtInfo = (TextView) this.findViewById(R.id.textInfo);
		record = (Button)   this.findViewById(R.id.record);
	//	changeView = (Button)   this.findViewById(R.id.changeView);

		
		// Activate GPS
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		lat = location.getLatitude();
		lon = location.getLongitude();
		
		mapView.setBuiltInZoomControls(true);
		mapView.setStreetView(true);
		mapController = mapView.getController();
		mapController.setZoom(18); // Zoom 1 is world view
		
		myLocationOverlay = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocationOverlay);
        // myLocationOverlay.enableCompass();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mapController.animateTo(myLocationOverlay.getMyLocation());
            }
        });
        
        
    }
    
	@Override
	public void onResume() {
		super.onResume();
		
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
		
		// Get values from preferneces on resume
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		pachubeFeed = Integer.parseInt(prefs.getString("pachubeFeed", "0"));
		pachubePass = prefs.getString("pachubePass", "<unset>");
		pachubeLogin = prefs.getString("pachubeLogin", "<unset>");
		gpsRecord = prefs.getBoolean("gpsRecord", false);
		runBackground = prefs.getBoolean("runBackground", false);
		interval = Integer.parseInt(prefs.getString("interval", "5000"));
		
		if(pachubeLogin != "") getKey();
		showMsg(pachubeKey);
	
	}
	
	@Override
	protected void onPause() {
		super.onResume();
		lm.removeUpdates(this);

	}
    
    public void startRecord(View view) {
    	if(isRecording){
    		record.setText("Start Recording");
    		isRecording = false;
    		myTimer.cancel();
    	}
    	else {
    		record.setText("Stop Recording");
    		isRecording = true;
    		myTimer.cancel();
    		myTimer = new Timer();
    		myTimer.schedule(new TimerTask() {
    			@Override
    			public void run() {
    				TimerMethod();
    			}
    			
    		}, 0, interval);
    	}
    }
    
    /***********  PREFERENCES *************/

    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.setup:
			// showMsg("Setup");
			Intent myIntent = new Intent(OpenRTMS.this, PrefView.class);
            startActivityForResult(myIntent, 0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showMsg(String msg) {
		Toast toast = Toast.makeText(OpenRTMS.this, msg, Toast.LENGTH_LONG);
		//toast.setGravity(Gravity.CENTER, toast.getXOffset() / 2, toast.getYOffset() / 2);
		toast.show();
	}

    /***********  TIMER *************/
    private void TimerMethod(){
		this.runOnUiThread(Timer_Tick);
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			if(isRecording){
				count++;
				updateText();
				//if(count >= interval && val != ""){
					//getLocation();
					try {
						postData();
					} catch (PachubeException e) {
						e.printStackTrace();
					}
					//count=0;
				//}
			}
		}
	};
	
	/***********  DATA POST *************/
		
	public boolean postData() throws PachubeException {
		HttpRequest hr = new HttpRequest("http://www.pachube.com/api/feeds/" + pachubeFeed + ".csv");
		hr.setMethod(HttpMethod.PUT);
		hr.addHeaderItem("X-PachubeApiKey", pachubeKey);
		hr.setBody(lat + "," + lon + "," + val);
		HttpResponse g = client.send(hr);

		if (g.getHeaderItem("Status").equals("HTTP/1.1 200 OK")) {
			return true;
		} else {
			throw new PachubeException(g.getHeaderItem("Status"));
		}

		/*
		// Initiate Pachube Feed
		try {
			p = new Pachube(pachubeKey);
			f = p.getFeed(pachubeFeed);
		} catch (PachubeException e) {
		   // System.println(e.errorMessage);
	    }
		// For testing
		
		val[0] = Math.random();
		val[1] = Math.random();
		val[2] = Math.random();
		  
		
	    // POST using web php script  
	    HttpClient httpclient = new DefaultHttpClient();  
	    HttpPost httppost = new HttpPost(postURL);  
	    try {  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);  
			nameValuePairs.add(new BasicNameValuePair("key",  pachubeKey ));  
	        nameValuePairs.add(new BasicNameValuePair("feed", pachubeFeed ));
	        nameValuePairs.add(new BasicNameValuePair("values", lat + "," + lon + "," + val));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	        HttpResponse response = httpclient.execute(httppost); 	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block  
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block  
	    } 
	    
	    // JAVA Pachube API
		for(int i=0; i< d.size(); i++) {
			Data data = (Data) d.get(i);
			data.setId(i);
			data.setValue(val[i]);
			f.updateDatastream(data);
		}

		// PUT REQUEST
	    try{
			URL url = new URL("http://api.pachube.com/v2/feeds/" + pachubeFeed + ".csv?key=" + pachubeKey);
	    	HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		    httpCon.setDoOutput(true);
		    httpCon.setRequestMethod("PUT");
		    OutputStreamWriter out = new OutputStreamWriter(
	        httpCon.getOutputStream());
		    out.write("0,123\n\r1,32\n\r2,23\n\r");
		    out.close();
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block  
	    }
	    */ 
	    
	}
	
	/***********  GPS  *************/
	
	public void getLocation(){
		
		/*
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			lat = location.getLatitude();
			lon = location.getLongitude();
		} else {
		// put alt here
		}
		*/
	}
	
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	public void onLocationChanged(Location location) {
		lat = location.getLatitude();
		lon = location.getLongitude();
		//locationCurrent = count;
	}
	
	public void updateText() {
		Log.v(tag, "Text updated");
		sb = new StringBuilder(512);
		
		
		/* display some of the data in the TextView */
		
		/*
		sb.append("Latitude: ");
		sb.append(lat);
		sb.append('\n');
		
		sb.append("Londitude: ");
		sb.append(lon);
		sb.append('\n');
		
		sb.append("Values: ");
		sb.append(val);
		*/
		
		String[] temp = val.split(",");
		
		if(temp.length>1){
			sb.append("Humidity: ");
			sb.append(temp[0]);
			sb.append('\n');
			
			sb.append("Temperature: ");
			sb.append(temp[1]);
			sb.append('\n');
		}
		
		
		sb.append("Count: ");
		sb.append(Integer.toString(count));
		/*
		sb.append('\n');
		
		
		sb.append("Number of values sent: ");
		sb.append(Boolean.toString(dataPosted));
		sb.append('\n');

		sb.append("Last current GPS: ");
		sb.append(Integer.toString(locationCurrent));
		sb.append('\n');
		*/
		
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
		/* This is called when the GPS status alters 
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
		*/
	}
	
	/***********  Amarino  *************/

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(arduinoReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
		//Amarino.connect(this, DEVICE_ADDRESS);
	}

	@Override
	protected void onStop() {
		super.onStop();
		//Amarino.disconnect(this, DEVICE_ADDRESS);
		unregisterReceiver(arduinoReceiver);
	}

	public class ArduinoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = null;
			final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);
			
			if (dataType == AmarinoIntent.STRING_EXTRA){
				data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);
				if (data != null){
					 val = data;
					try {
		
					} catch (NumberFormatException e) { 
						// Add exception
					}
				}
			}
		}
	}	
	
	
	/***********  Pachube Query  *************/	
 
	public void getKey() {
	    	
	    	UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(pachubeLogin, pachubePass);
	        String url = "http://api.pachube.com/v2/users/"+pachubeLogin+".json";
	        DefaultHttpClient otherClient = new DefaultHttpClient();
	        otherClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
	        HttpGet newRequest = new HttpGet(url);
	        try{
		        org.apache.http.HttpResponse newResponse = otherClient.execute(newRequest);	            
	            String jString = HttpHelper.request(newResponse);
	            JSONObject jObject = new JSONObject(jString);
	            JSONObject menuObject = jObject.getJSONObject("user");
	    		pachubeKey = menuObject.getString("api_key");
	        }catch(Exception ex){
	           // txtResult.setText("Failed!");
	        }
	    
    }
	
	/***********  Pachube Query  *************/	

	void changeView(View view){
		
	}
	

}





