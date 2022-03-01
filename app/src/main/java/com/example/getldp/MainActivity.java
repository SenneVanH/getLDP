package com.example.getldp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
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

public class MainActivity extends AppCompatActivity {

    static final String provider_auth_uri = "com.ldp.provider";
    static final Uri CONTENT_URI = Uri.parse("content://" + provider_auth_uri + "/locations");
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 9;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION_BACKGROUND = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //android Q == API 29. also see https://stackoverflow.com/a/69395540/13286640
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION_BACKGROUND);
            }
        }
        HttpWorker.enqueueSelf(getApplicationContext()); //it is not a problem to enqueue multiple times, if it already exists it's a null operation.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permissions_ok = true;
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            Log.i("GETLDP_PERMISSIONS", "Received response for Camera permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_LONG).show();
            } else {
                permissions_ok = false;
            }
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION_BACKGROUND) {
            Log.i("GETLDP_PERMISSIONS", "Received response for Camera permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "BACKGROUND location permission granted", Toast.LENGTH_LONG).show();
            } else {
                permissions_ok = false;
            }
        }
        if (permissions_ok) HttpWorker.enqueueSelf(getApplicationContext());
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("Range")
    public void onClickShowDetails(View view) throws RemoteException {
        // inserting complete table details in this text field
        TextView resultView = (TextView) findViewById(R.id.res);
        // creating a cursor object of the
        // content URI
        Cursor cursor = getContentResolver().query(CONTENT_URI, null, null, null, null);

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
            resultView.setText("No Records Found");
        }
    }
}