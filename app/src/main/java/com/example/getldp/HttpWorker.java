package com.example.getldp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * credit to https://stackoverflow.com/questions/53043183/how-to-register-a-periodic-work-request-with-workmanger-system-wide-once-i-e-a/53507670#53507670
 */
public class HttpWorker extends Worker {
    private static final String uniqueWorkName = "com.example.getldp.HttpWorker";
    private static final String postURL = "https://first-spring-app-locldp.azuremicroservices.io/db/addjson";
    private static final long repeatIntervalMillis = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS; //15 minutes
    private static final long flexIntervalMillis = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS; //5 minutes
    private static RequestQueue requestQueue;

    public HttpWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        requestQueue = Volley.newRequestQueue(context);
        ;
    }

    private static PeriodicWorkRequest getOwnWorkRequest() {
        return new PeriodicWorkRequest.Builder(
                HttpWorker.class, repeatIntervalMillis, TimeUnit.MILLISECONDS, flexIntervalMillis, TimeUnit.MILLISECONDS
        ).build();
    }

    public static void enqueueSelf(Context ctx) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.KEEP, getOwnWorkRequest());
    }

    @NonNull
    @Override
    public Worker.Result doWork() {
        //send 2 POST request every 15 min
        LocEntity testLocEntity = new LocEntity();
        testLocEntity.setEpoch(123);
        testLocEntity.setExact(true);
        testLocEntity.setUserId(123);
        testLocEntity.setLatitude(321);
        testLocEntity.setLongitude(321);
        //real location
        try {
            JSONObject request = new JSONObject(new Gson().toJson(testLocEntity));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postURL, request, response -> {
                Log.d("HTTP_POST","post done of:"+response.toString());
            }, error -> {
                Log.e("HTTP_POST", "something went wrong, got: "+error.getMessage());
            });
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
            return Result.failure(); //idk what this does exactly tho
        }
        return Result.success();
    }
}
//TODO: check if internet permission needed for requests
