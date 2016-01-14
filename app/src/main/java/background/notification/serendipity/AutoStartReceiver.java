package background.notification.serendipity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Home on 7/23/15.
 */
public class AutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        BusinessQueryManager receiver = new BusinessQueryManager();

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            receiver.setAlarm(context);
        }
    }
}
