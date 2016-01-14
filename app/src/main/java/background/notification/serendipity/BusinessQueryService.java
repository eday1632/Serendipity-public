package background.notification.serendipity;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import yelp.apis.serendipity.YelpSearchAsync;

/**
 * Created by Home on 7/23/15.
 */
public class BusinessQueryService extends IntentService implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener{

    protected GoogleApiClient mGoogleApiClient;
    protected String mLatitudeText;
    protected String mLongitudeText;
    protected LocationRequest mLocationRequest;
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

    public BusinessQueryService() {
        super("BusinessQueryService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int level;
        int scale;
        float batteryPct;

        try {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float)scale;
            Log.i("BusinessQueryService", String.valueOf(batteryPct));
        } catch (NullPointerException e) {
            e.printStackTrace();
            batteryPct = 1.0f;
        }

        if(batteryPct > .20) {
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        } else {
            BusinessQueryManager.cancelAlarm(getApplicationContext());
            stopLocationUpdates();
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
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("BusinessQueryService", "Connection suspended for cause: " + cause);
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("BusinessQueryService", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    private void getLatLng(Location location){
        mLatitudeText = String.valueOf(location.getLatitude());
        mLongitudeText = String.valueOf(location.getLongitude());
        Log.i("BusinessQueryService", "Latitude, Longitude: " + mLatitudeText + ", " + mLongitudeText);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            getLatLng(location);
            new YelpSearchAsync(getApplicationContext(), mLatitudeText, mLongitudeText).execute();
            stopLocationUpdates();
            Log.i("BusinessQueryService", "Latitude, Longitude: " + mLatitudeText + ", " + mLongitudeText);
        }
    }
}
