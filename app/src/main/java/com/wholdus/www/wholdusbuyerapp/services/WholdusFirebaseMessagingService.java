package com.wholdus.www.wholdusbuyerapp.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.SplashActivity;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.NotificationContract.NotificationTable;
import com.wholdus.www.wholdusbuyerapp.databaseHelpers.NotificationDBHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static android.app.Notification.DEFAULT_ALL;

/**
 * Created by aditya on 28/12/16.
 */

public class WholdusFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //super.onMessageReceived(remoteMessage);

        try {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            Map<String, String> data = remoteMessage.getData();

            if ((data == null || data.isEmpty()) && notification == null) {
                return;
            }

            if (data.containsKey("has_notification") && data.get("has_notification").equals("true")) {
                String notificationTitle = null;
                String notificationBody = null;
                if (data.containsKey("notification_title")) {
                    notificationTitle = data.get("notification_title");
                }
                if (data.containsKey("notification_body")) {
                    notificationBody = data.get("notification_body");
                }
                NotificationDBHelper notificationDBHelper = new NotificationDBHelper(getApplicationContext());
                try {
                    JSONObject notificationJSON = new JSONObject();
                    notificationJSON.put(NotificationTable.COLUMN_NOTIFICATION_JSON, new JSONObject(data));
                    notificationJSON.put(NotificationTable.COLUMN_NOTIFICATION_TYPE, "general");
                    int notificationID = notificationDBHelper.saveNotificationData(notificationJSON);
                    data.put("notificationID", String.valueOf(notificationID));
                } catch (JSONException e){

                }
                buildNotification(notificationTitle, notificationBody, data);
            } else if (notification != null) {
                buildNotification(notification.getTitle(), notification.getBody(), data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.report(e);
        }
    }

    public void buildNotification(String title, String body, Map<String, String> data) {
        Intent intent = new Intent(this, SplashActivity.class);
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }

        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        if (title == null) {
            title = "Wholdus";
        }
        if (body == null) {
            title = "New catalog arrived";
        }
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(body);
        notificationBuilder.setDefaults(DEFAULT_ALL);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.wholdus_logo);
        notificationBuilder.setColor(getResources().getColor(R.color.accent));
        notificationBuilder.setContentIntent(pendingIntent);
        NotificationManager notificationManagerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManagerCompat.notify(0, notificationBuilder.build());
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }
}
