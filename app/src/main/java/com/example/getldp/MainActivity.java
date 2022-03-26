package com.example.getldp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static final String provider_auth_uri = "com.ldp.provider";
    static final Uri CONTENT_URI = Uri.parse("content://" + provider_auth_uri + "/locations");
    public static final int FINE_REQUEST_CODE = 1999;
    public static final int BACKGROUND_REQUEST_CODE = 1998;
    public static String PACKAGE_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
        PACKAGE_NAME = getApplicationContext().getPackageName();
        // main code branch proceeds in onrequestpermissionsresult()
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FINE_REQUEST_CODE) {
            Log.i("GETLDP_PERMISSIONS", "Received response for Fine location permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == BACKGROUND_REQUEST_CODE) {
            Log.i("GETLDP_PERMISSIONS", "Received response for Background location permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "BACKGROUND location permission granted", Toast.LENGTH_SHORT).show();
            }
        }
        requestPermissions(); //another round of permission checks. if they are all OK we will not get here again
        HttpWorker.enqueueSelf(getApplicationContext());
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < 29) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT == 29) { //api level 29
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_REQUEST_CODE);
            }
        }
        else { //api level >=30
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_REQUEST_CODE);
            }
            // else if is use beneath to make sure only one permission request is done at a time. the onpermissionresult listener keeps calling requestpermissions until no more requests are done.
            else if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, FINE_REQUEST_CODE);
            }
        }
    }

    @SuppressLint("Range")
    public void onClickShowDetails(View view) throws RemoteException {
        // inserting complete table details in this text field
        TextView resultView = (TextView) findViewById(R.id.res);
        try {
            // creating a cursor object of the
            // content URI

            Cursor cursor = getContentResolver().query(Uri.parse(CONTENT_URI + "/" + getPackageName()), null, null, null, null);

            // iteration of the cursor
            // to print whole table
            if (cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                while (!cursor.isAfterLast()) {
                    strBuild.append("\n" + cursor.getString(cursor.getColumnIndex("id")) + "-" + cursor.getString(cursor.getColumnIndex("latitude")) + ":" + cursor.getString(cursor.getColumnIndex("longitude")));
                    cursor.moveToNext();
                }
                resultView.setText(strBuild);
                cursor.close();
            } else {
                cursor.close();
                resultView.setText(R.string.NoRecordsFound);
            }
        }
        catch (java.lang.SecurityException e) {
            resultView.setText(R.string.text_no_reaction_from_provider);
            e.printStackTrace();
        }
    }

    public void onClickRequestUri(View view){
        final Intent intent=new Intent();
        intent.setAction("com.ldp.package.uri");
        intent.setPackage(getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setComponent(
                new ComponentName("com.example.locldp2","com.example.locldp2.UriRequestReceiver"));
        sendBroadcast(intent);

        //test for consuming the provider uri:
        try {
            Uri appSpecificUri = Uri.parse("content://" + provider_auth_uri + "/locations/"+getPackageName());

        }catch (Exception e){
            Log.d("RequestingUri: ", e.getMessage());
        }
    }

    public void onClickTestBackground(View view) {
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(HttpWorker.class)
                        .build();
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(request);
    }

    public void onClickTestHttpPost(){
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        LocEntity locEntity = new LocEntity();
        locEntity.setRadius(123);
        locEntity.setLongitude(123);
        locEntity.setLatitude(123);
        locEntity.setEpoch(123);
        locEntity.setUserId(123);
        locEntity.setExact(false);
        Log.d("HTTP_POST", "Start of doPostRequestForResult()");
        try {
            JSONObject request = new JSONObject(new Gson().toJson(locEntity));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, "https://first-spring-app-locldp.azuremicroservices.io/db/addjson", request, response -> Log.d("HTTP_POST", "post done of:" + response.toString()), error -> Log.e("HTTP_POST", "something went wrong, got: " + error.getMessage()));
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("HTTP_POST_JSONException", e.getMessage());
        }
    }
}