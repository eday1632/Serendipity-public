package www.appawareinc.org.serendipity;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    double latitude;
    double longitude;
    String name;
    boolean placeMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        onNewIntent(getIntent());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        String rawJSON = extras.getString("businessInfo");
        if(rawJSON != null) {
            extractBusinessInfo(rawJSON);
        } else {
            focusCameraOnUser(extras);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        if(placeMarker) googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(name));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17));
    }

    private void extractBusinessInfo(String rawData){
        Log.i("MapsActivity", "Raw JSON exists!");
        try {
            JSONObject object = new JSONObject(rawData);
            name = object.getString("name");

            JSONObject location = object.getJSONObject("location");
            JSONObject coordinates = location.getJSONObject("coordinate");
            latitude = Double.parseDouble(coordinates.getString("latitude"));
            longitude = Double.parseDouble(coordinates.getString("longitude"));
            placeMarker = true;

            Log.i("MapsActivity", latitude + " and " + longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void focusCameraOnUser(Bundle extras){
        latitude = Double.parseDouble(extras.getString("userLat"));
        longitude = Double.parseDouble(extras.getString("userLng"));
        placeMarker = false;

        Log.i("MapsActivity", "JSON is null, set lat/long to be user location");
    }
}
