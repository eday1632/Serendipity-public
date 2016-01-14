package yelp.apis.serendipity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;

import www.appawareinc.org.serendipity.MainActivity;
import www.appawareinc.org.serendipity.R;
import www.appawareinc.org.serendipity.SearchResults;

/**
 * Created by Home on 7/20/15.
 */

public class YelpSearchAsync extends AsyncTask<String, Integer, String> {

    final String FOUR_HALF = "4.5";
    final String FIVE = "5";
    int minimumReviews = 15;
    String latitude;
    String longitude;
    String businessType;
    String distance;
    Context context;
    boolean okToSearch;
    boolean fromMainActivity = false;
    boolean fromNotificationService = false;


    public YelpSearchAsync (Context context, String latitude, String longitude){
        this.context = context;
        this.latitude = latitude;
        this.longitude = longitude;
        fromNotificationService = true;
    }

    public YelpSearchAsync (Context context, String latitude, String longitude, String businessType, String distance){
        this.context = context;
        this.latitude = latitude;
        this.longitude = longitude;
        this.businessType = businessType;
        this.distance = distance;
        fromMainActivity = true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        okToSearch = isOnline();
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected String doInBackground(String... params) {
        if(okToSearch) {
            return mySerendipity();
        } else {
            return null;
        }
    }

    private String mySerendipity() {
        String rawYelpData = getRawYelpResults();

        try {
            //turn raw data into JSON
            JSONObject results = new JSONObject(rawYelpData);

            //get the array of businesses
            JSONArray businessesArray = results.getJSONArray("businesses");

            //put businesses in an ArrayList for easier handling
            ArrayList<JSONObject> returnedBusinesses = JSONArrayToArrayList(businessesArray);

            //get the best, unseen business!
            JSONObject newBusiness = getBestNewBusiness(returnedBusinesses);

            if(newBusiness == null) {
                return null;
            } else {
                return newBusiness.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRawYelpResults(){
        YelpAPI go = new YelpAPI();

        if(fromNotificationService) {
            SharedPreferences preferences = context.getSharedPreferences("businessType", Context.MODE_PRIVATE);
            businessType = preferences.getString("businessType", "");

            preferences = context.getSharedPreferences("distance", Context.MODE_PRIVATE);
            distance = preferences.getString("distance", "100");

            Log.i("YelpSearchAsync", "Business type and distance: " + businessType + ", " + distance);
            return go.searchForBusinessesByLocation(businessType, latitude, longitude, distance);
        } else {
            Log.i("YelpSearchAsync", "Business type and distance: " + businessType + ", " + distance);
            return go.searchForBusinessesByLocation(businessType, latitude, longitude, distance);
        }
    }

    private ArrayList<JSONObject> JSONArrayToArrayList(JSONArray array){
        ArrayList<JSONObject> result = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            try {
                result.add(array.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result; //return null or result, not unused, but instantiated ArrayList
    }

    private JSONObject getBestNewBusiness(ArrayList<JSONObject> array){
        ArrayList<JSONObject> results = new ArrayList<>();
        HashSet<String> seenBusinesses = getSeenBusinesses();
        try {
            for(JSONObject object : array){
                String rating = object.getString("rating");
                int distanceToBusiness = object.getInt("distance");
                int reviews = object.getInt("review_count");
                String id = object.getString("id");

                if((rating.contentEquals(FOUR_HALF) || rating.contentEquals(FIVE)) //sorts out low ratings
                        && distanceToBusiness < Integer.parseInt(distance) //sorts out ads
                        && reviews >= minimumReviews //sorts out too few reviews
                        && !seenBusinesses.contains(id)) { //sorts out seen businesses
                    results.add(object);
                    Log.i("YelpSearchAsync", object.toString());
                }
            }
            Log.i("YelpSearchAsync", String.valueOf(results.size()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (results.size() > 0) {
            try {
                String business = results.get(0).getString("id");
                seenBusinesses.add(business);
                saveSeenBusinessIDs(seenBusinesses);
                return results.get(0);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private HashSet<String> getSeenBusinesses() {
        HashSet<String> seenBusinesses = new HashSet<>();
        try {
            String inputLine = "";
            FileInputStream fIn = context.openFileInput("seen_businesses_serendipity.txt");
            InputStreamReader isr = new InputStreamReader(fIn);
            BufferedReader inBuff = new BufferedReader(isr);

            while ((inputLine = inBuff.readLine()) != null) {
                seenBusinesses.add(inputLine);
            }
            inBuff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return seenBusinesses;
    }

    private void saveSeenBusinessIDs(HashSet<String> IDs){
        try {
            FileOutputStream fOut = context.openFileOutput("seen_businesses_serendipity.txt",
                    Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            try {
                for (String item : IDs) {
                    osw.write(item + "\n");
                }
                osw.flush();
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        MainActivity.cancelToast();
        if(s != null) {
            if (fromMainActivity) {
                Intent intent = new Intent("www.appawareinc.org.serendipity.SearchResults");
                intent.putExtra("info", s);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                if (okToSearch) sendNotification(s);
            }
        } else {
            if(fromMainActivity) Toast.makeText(
                    context, R.string.empty_yelp_results, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotification(String msg) {

        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, SearchResults.class);
        intent.putExtra("info", msg);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.star)
                        .setContentTitle("Your Serendipity!")
                        .setAutoCancel(true)
                        .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                        .setLights(Color.YELLOW, 3000, 3000)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(parseJSONData(msg)))
                        .setContentText(parseJSONData(msg));

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public static String parseJSONData (String data) {
        String result;
        String distance = "unknown";
        String reviews = "none";
        String name = "unknown";
        String rating = "none";
        String crossStreets = "unspecified";
        String address = "unlisted";
        String phone = "unlisted";
        String snippetText = "none";

        try {
            JSONObject object = new JSONObject(data);
            JSONObject location = object.getJSONObject("location");
            JSONArray addressDetails = location.getJSONArray("display_address");

            if(object.has("distance")) {
                distance = object.getString("distance");
            }
            if(object.has("review_count")) {
                reviews = object.getString("review_count");
            }
            if(object.has("name")) {
                name = object.getString("name");
            }
            if(object.has("rating")) {
                rating = object.getString("rating");
            }
            if(object.has("phone")) {
                phone = object.getString("phone");
            }
            if(location.has("cross_streets")) {
                crossStreets = location.getString("cross_streets");
            }
            if(location.has("display_address")) {
                address = addressDetails.getString(0) + ", " + addressDetails.getString(1);
            }
            if(object.has("snippet_text")) {
                snippetText = object.getString("snippet_text");
            }

            result = "Name: " + name + "\n" + "Distance in meters: " + distance + "\n"
                    + "Rating: " + rating + "\n" + "Reviews: " + reviews
                    + "\n" + "Cross streets: " + crossStreets
                    + "\n" + "Address: " + address + "\n" + "Phone: " + phone
                    + "\n" + "A customer said: " + snippetText;

        } catch (JSONException e){
            e.printStackTrace();
            result = "Error parsing JSON data";
        }
        return result;
    }
}
