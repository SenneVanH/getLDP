package com.example.getldp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Uri CONTENT_URI = Uri.parse("content://com.example.ldp.provider/locations");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @SuppressLint("Range")
    public void onClickShowDetails(View view) throws RemoteException {
        // inserting complete table details in this text field
        TextView resultView= (TextView) findViewById(R.id.res);
        // creating a cursor object of the
        // content URI
        Cursor cursor = getContentResolver().query(Uri.parse("content://com.example.ldp.provider/locations"), null, null, null, null);

        // iteration of the cursor
        // to print whole table
        if(cursor.moveToFirst()) {
            StringBuilder strBuild=new StringBuilder();
            while (!cursor.isAfterLast()) {
                strBuild.append("\n"+cursor.getString(cursor.getColumnIndex("id"))+ "-"+ cursor.getString(cursor.getColumnIndex("latitude"))+ ":"+ cursor.getString(cursor.getColumnIndex("longitude")));
                cursor.moveToNext();
            }
            resultView.setText(strBuild);
            cursor.close();
        }
        else {
            cursor.close();
            resultView.setText("No Records Found");
        }
    }
}