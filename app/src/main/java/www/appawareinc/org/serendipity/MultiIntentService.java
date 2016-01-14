package www.appawareinc.org.serendipity;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Home on 7/25/15.
 */
public class MultiIntentService extends IntentService {

    public MultiIntentService() {
        super("MultiIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String controller = extras.getString("controller");

        if (controller.contentEquals("searchParameters")) { //multiple per session
            SharedPreferences typePref = getSharedPreferences("businessType", Context.MODE_PRIVATE);
            SharedPreferences distancePref = getSharedPreferences("distance", Context.MODE_PRIVATE);

            SharedPreferences.Editor typeEditor = typePref.edit();
            SharedPreferences.Editor distanceEditor = distancePref.edit();

            typeEditor.putString("businessType", extras.getString("businessType"));
            distanceEditor.putString("distance", extras.getString("distance"));

            typeEditor.commit();
            distanceEditor.commit();
        }
    }
}