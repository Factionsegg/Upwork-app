package com.me.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.DataOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
    private String TAG = "MyService";
    public static boolean isServiceRunning;
    private String CHANNEL_ID = "NOTIFICATION_CHANNEL";
    private WifiManager wifiManager;
    WifiConfiguration currentConfig;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public MyService() {
        Log.d(TAG, "constructor called");
        isServiceRunning = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "MyDummySSID";

        Log.d(TAG, "onCreate called");
        int delay = 0; // delay for 0 sec.
        int period = 10000; // repeat every 10 sec.
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run()
            {
                enableMobileData();
                Log.d(TAG,"hello");
            }
        }, delay, period);
        createNotificationChannel();
        isServiceRunning = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service is Running")
                .setContentText("Listening for Screen Off/On events")
                .setSmallIcon(R.drawable.ic_wallpaper_black_24dp)
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    appName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        isServiceRunning = false;
        stopForeground(true);

        // call MyReceiver which will restart this service via a worker
        Intent broadcastIntent = new Intent(this, MyReceiver.class);
        sendBroadcast(broadcastIntent);

        super.onDestroy();
    }

    private void enableMobileData(){

        try {
            String[] cmds = {"svc data enable"};
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
