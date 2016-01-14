package www.appawareinc.org.serendipity;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import background.notification.serendipity.BusinessQueryManager;
import yelp.apis.serendipity.YelpSearchAsync;

public class SearchResults extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static TextView businessInfo;
    protected String mLatitudeText;
    protected String mLongitudeText;
    static String queryResult;
    protected GoogleApiClient mGoogleApiClient;
    private static LatLngBounds localGPSBounds;
    AutocompletePredictionBuffer autocompletePredictions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.title_activity_search_results);

        businessInfo = (TextView) findViewById(R.id.business_info);

        Button map = (Button) findViewById(R.id.map_button);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("www.appawareinc.org.serendipity.MapsActivity");
                intent.putExtra("businessInfo", queryResult);
                intent.putExtra("userLat", mLatitudeText);
                intent.putExtra("userLng", mLongitudeText);
                startActivity(intent);
            }
        });
        onNewIntent(getIntent());
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();

        if(extras != null) {
            showSearchResult(extras.getString("info"));
            mLatitudeText = extras.getString("latitude");
            mLongitudeText = extras.getString("longitude");
        } else {
            Log.e("SearchResults", "Error retrieving intent extras");
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_results, menu);
        return true;
    }

    public static void showSearchResult(String result){
        if(businessInfo != null) {
            if (result != null) {
                queryResult = result;
                businessInfo.setText(YelpSearchAsync.parseJSONData(result));
            } else {
                businessInfo.setText("Nothing to see here... yet!");
            }
        }
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        getLatLng(currentLocation);
        LatLng low = new LatLng(Double.parseDouble(mLatitudeText) - 0.05,
                Double.parseDouble(mLongitudeText) - 0.05);
        LatLng high = new LatLng(Double.parseDouble(mLatitudeText) + 0.05,
                Double.parseDouble(mLongitudeText) + 0.05);
        localGPSBounds = new LatLngBounds(low, high);
        new PlacesSearchAsync().execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("SearchResults", "Connection suspended for cause: " + i);
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("SearchResults", "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

    }

    private void getLatLng(Location location){
        if (location != null) {
            mLatitudeText = String.valueOf(location.getLatitude());
            mLongitudeText = String.valueOf(location.getLongitude());
            Log.i("SearchResults", "Latitude, Longitude: " + mLatitudeText + ", " + mLongitudeText);
        }
    }

    class PlacesSearchAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i("SearchResults", "Result: " + queryResult);
                JSONObject object = new JSONObject(queryResult);
                String name = object.getString("name");
                Log.i("SearchResults", "Name: " + name);
                PendingResult<AutocompletePredictionBuffer> results =
                        Places.GeoDataApi
                                .getAutocompletePredictions(mGoogleApiClient, name,
                                        localGPSBounds, null);

                // This method should have been called off the main UI thread. Block and wait for at most 60s
                // for a result from the API.
                autocompletePredictions = results
                    .await(60, TimeUnit.SECONDS);

                // Confirm that the query completed successfully, otherwise return null
                final com.google.android.gms.common.api.Status status = autocompletePredictions.getStatus();
                if (!status.isSuccess()) {
                    Log.e("SearchResults", "Error getting autocomplete prediction API call: " + status.toString());
                    autocompletePredictions.release();
                    return null;
                }

                Log.i("SearchResults", "Query completed. Received " + autocompletePredictions.getCount()
                        + " predictions.");

                // Copy the results into our own data structure, because we can't hold onto the buffer.
                // AutocompletePrediction objects encapsulate the API response (place ID and description).

                Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
                ArrayList resultList = new ArrayList<>(autocompletePredictions.getCount());
                while (iterator.hasNext()) {
                    AutocompletePrediction prediction = iterator.next();
                    // Get the details of this prediction and copy it into a new PlaceAutocomplete object.
                    resultList.add(new PlaceAutocomplete(prediction.getPlaceId()));
                }

                // Release the buffer now that all data has been copied.
                autocompletePredictions.release();

                Log.i("SearchResults", "Whatever we got back: " + resultList.get(0).toString());

                Places.GeoDataApi.getPlaceById(mGoogleApiClient, resultList.get(0).toString())
                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (places.getStatus().isSuccess()) {
                                    final Place myPlace = places.get(0);
                                    Log.i("SearchResults", "Place: " + myPlace.getName()
                                            + "; Price level: " + myPlace.getPriceLevel()
                                            + "; Rating: " + myPlace.getRating());
                                }
                                places.release();
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Holder for Places Geo Data Autocomplete API results.
     */
    class PlaceAutocomplete {

        public CharSequence placeId;

        PlaceAutocomplete(CharSequence placeId) {
            this.placeId = placeId;
        }

        @Override
        public String toString() {
            return placeId.toString();
        }
    }
}
