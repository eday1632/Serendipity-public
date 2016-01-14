package www.appawareinc.org.serendipity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import background.notification.serendipity.BusinessQueryManager;
import yelp.apis.serendipity.YelpSearchAsync;

public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    String tempType;
    String tempDistance;
    static Toast toast;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    protected LocationRequest mLocationRequest;
    protected String mLatitudeText;
    protected String mLongitudeText;
    boolean makeQuery = false;
    BusinessQueryManager receiver = new BusinessQueryManager();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 4000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.title_activity_main);

        final EditText businessType = (EditText) findViewById(R.id.businessType);
        final EditText distance = (EditText) findViewById(R.id.distance);
        Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (makeQuery) {
                    cancelToast();
                    tempType = businessType.getText().toString();
//                    if (tempType.contentEquals("")) tempType = "food";
                    tempDistance = distance.getText().toString();
                    if (tempDistance.contentEquals("")) tempDistance = "100";
                    saveParametersInBackground();

                    new YelpSearchAsync(getApplicationContext(),
                            mLatitudeText, mLongitudeText, tempType, tempDistance).execute();
                    toast = Toast.makeText(getApplicationContext(),
                            R.string.searching, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.location_error, Toast.LENGTH_SHORT).show();
                }
                receiver.setAlarm(getApplicationContext());
            }
        });
        buildGoogleApiClient();
    }

    private void saveParametersInBackground(){
        Intent serviceIntent = new Intent(this, MultiIntentService.class);
        serviceIntent.putExtra("controller", "searchParameters");
        serviceIntent.putExtra("businessType", tempType);
        serviceIntent.putExtra("distance", tempDistance);
        startService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    public static void cancelToast(){
        if(toast != null){
            toast.cancel();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.clear_businesses) {
            return deleteSeenBusinessesFile();
        } else if (id == R.id.cancel_alarm) {
            return BusinessQueryManager.cancelAlarm(getApplicationContext());
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean deleteSeenBusinessesFile() {
        try {
            FileOutputStream fOut = openFileOutput("seen_businesses_serendipity.txt",
                    Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            try {
                osw.write("");
                osw.flush();
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        getLatLng(mCurrentLocation);
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("SearchResults", "Connection suspended for cause: " + cause);
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("SearchResults", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        getLatLng(location);
    }

    private void getLatLng(Location location){
        if (location != null) {
            mLatitudeText = String.valueOf(location.getLatitude());
            mLongitudeText = String.valueOf(location.getLongitude());
            makeQuery = true;
            Log.i("MainActivity", "Latitude, Longitude: " + mLatitudeText + ", " + mLongitudeText);
        } else {
            makeQuery = false;
        }
    }
}