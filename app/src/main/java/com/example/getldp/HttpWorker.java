package com.example.getldp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * credit to https://stackoverflow.com/questions/53043183/how-to-register-a-periodic-work-request-with-workmanger-system-wide-once-i-e-a/53507670#53507670
 */
public class HttpWorker extends Worker {
    private static Uri PERSONAL_CONTENT_URI;
    private static final String instantUniqueWorkName = "com.example.getldp.HttpWorker.instant";
    private static final String uniqueWorkName = "com.example.getldp.HttpWorker";
    private static final String postURL = "https://first-spring-app-locldp.azuremicroservices.io/db/addjson";
    private static final long repeatIntervalMillis = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS; //15 minutes
    private static final long flexIntervalMillis = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS; //5 minutes
    private static final String CHANNEL_ID = "notifications_getldp_from_background";
    private static RequestQueue requestQueue;
    private static Location realLocation; // request to be updated is in constructor of httpworker
    SharedPreferences sharedpreferences;
    String MyPREFERENCES = "GETLDP_PREF";
    public static long userId;

    @SuppressLint("MissingPermission")
    //permissions are checked in checkPermissions() but linter does not detect
    public HttpWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        PERSONAL_CONTENT_URI = Uri.parse("content://" + MainActivity.provider_auth_uri + "/locations/" + getApplicationContext().getPackageName());
        sharedpreferences = getApplicationContext().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        userId = sharedpreferences.getLong("userId", 0L);
        while (userId == 0L) {
            userId = new SecureRandom().nextLong();
            SharedPreferences.Editor myEdit = sharedpreferences.edit();
            myEdit.putLong("userId", userId);
            myEdit.apply();
        }
        createNotificationChannel();
        requestQueue = Volley.newRequestQueue(context);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


        if (Looper.myLooper() == null)
            Looper.prepare(); //Looper is used internally in LocationManager.requestLocationUpdates (has a handler with messages in it)
        if (!checkPermissions()) {
            notifyNoBackgroundPermissions();
        }
        while (!checkPermissions()) {
            Thread.yield();
        }
        requestOrRefreshUriPermissions();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 900_000L, // 900000 == 15 min, so this location is at most 15 min old
                10.0f, location -> {
                    realLocation = location;
                    //maybe keep track of the exact location time here.
                });
        while (realLocation == null) {
            realLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Thread.yield();
        }
    }

    private static PeriodicWorkRequest getOwnWorkRequest() {
        return new PeriodicWorkRequest.Builder(
                HttpWorker.class, repeatIntervalMillis, TimeUnit.MILLISECONDS, flexIntervalMillis, TimeUnit.MILLISECONDS
        ).setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS).build();
    }

    public static void enqueueSelf(Context ctx) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.KEEP, getOwnWorkRequest());
    }

    @SuppressLint({"MissingPermission"})
    @NonNull
    @Override
    public Worker.Result doWork() {
        requestOrRefreshUriPermissions();
        if (!checkPermissions()) {
            return Result.retry();
        }
        LocEntity realLocEntity = new LocEntity();
        //perturbed send
        Result retry = consumeProviderAndPost();
        if (retry != null) return retry;
        //now sending real location
        realLocEntity.setEpoch(System.currentTimeMillis());
        realLocEntity.setExact(true);
//        perturbedLocEntity.setUserId();
        realLocEntity.setUserId(userId);
        realLocEntity.setLatitude(realLocation.getLatitude());
        realLocEntity.setLongitude(realLocation.getLongitude());
        if (doPostRequestForResult(realLocEntity)) return Result.success();
        return Result.retry();
    }

    @SuppressLint("Range")
    @Nullable
    private Result consumeProviderAndPost() {
        LocEntity perturbedLocEntity = new LocEntity();
        //start real location fetch because part of it will run in the background
        try {
            Cursor cursor = getApplicationContext().getContentResolver().query(PERSONAL_CONTENT_URI, null, null, null, null);
            //send 2 POST request every 15 min
            if (cursor.moveToFirst()) {
                perturbedLocEntity.setEpoch(cursor.getColumnIndex("timestamp"));
                perturbedLocEntity.setRadius(cursor.getColumnIndex("radius"));
                perturbedLocEntity.setExact(false);
                perturbedLocEntity.setUserId(userId);
                perturbedLocEntity.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
                perturbedLocEntity.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
                cursor.close();
                if (!doPostRequestForResult(perturbedLocEntity)) return Result.retry();
            } else {
                cursor.close();
                Log.e("Provider_access", "no record found in provider URI");
            }
        } catch (Throwable throwable) {
            //TODO: which exception when URIpermissions not given? A: it's a NullPointerException
            notifyUriAccessProblem();
        }
        return null;
    }

    private void notifyNoBackgroundPermissions() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0); //don't understand flags.

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Requesting Location")
                .setContentText("The app GETLDP does not have the necessary permissions to access location in the background. Go to the app to change your preferences")
//            .setStyle(new NotificationCompat.BigTextStyle()
//                    .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(1999, notificationBuilder.build());
    }

    private void notifyUriAccessProblem() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Provider permissions needed")
//            .setStyle(new NotificationCompat.BigTextStyle()
//                    .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        PackageManager packageManager = getApplicationContext().getPackageManager();
        String notificationText;

        try {
            packageManager.getPackageInfo("com.example.locldp2", PackageManager.GET_ACTIVITIES);
            notificationText = "GETLDP does not have location access settings in LOCLDP provider. Go to the app to change your preferences";
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(
                    new ComponentName("com.example.locldp2", "com.example.locldp2.MainActivity"));
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0); //don't understand flags.
            notificationBuilder.setContentText(notificationText)
                    .setContentIntent(pendingIntent);
        } catch (PackageManager.NameNotFoundException e) {
            notificationText = "The companion app locldp2 is not installed";
            notificationBuilder.setContentText(notificationText);
        }
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(1999, notificationBuilder.build());
    }

    private boolean doPostRequestForResult(LocEntity locEntity) {
        Log.d("HTTP_POST", "Start of doPostRequestForResult()");
        try {
            JSONObject request = new JSONObject(new Gson().toJson(locEntity));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postURL, request, response -> Log.d("HTTP_POST", "post done of:" + response.toString()), error -> Log.e("HTTP_POST", "something went wrong, got: " + error.getMessage()));
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("HTTP_POST_JSONException", e.getMessage());
            return false; //something went wrong
        }
        return true;
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //android Q == API 29. also see https://stackoverflow.com/a/69395540/13286640
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
        }
        return true;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getApplicationContext().getString(R.string.channel_name);
        String description = getApplicationContext().getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void requestOrRefreshUriPermissions(){
        final Intent uriReqIntent=new Intent();
        uriReqIntent.setAction("com.ldp.package.uri");
        uriReqIntent.setPackage(getApplicationContext().getPackageName());
        uriReqIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        uriReqIntent.setComponent(
                new ComponentName("com.example.locldp2","com.example.locldp2.UriRequestReceiver"));
        getApplicationContext().sendBroadcast(uriReqIntent);
    }
}
