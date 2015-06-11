package com.gogreen.greenmachine.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.main.login.DispatchActivity;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.ArrayList;


public class ProfileHotspotInfoActivity extends ActionBarActivity {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_hotspot_info);

        // Set up the handler for the next button click
        ImageButton nextButton = (ImageButton) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submit();
            }
        });

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile_hotspot_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void submit() {
        // Set up public profile with data
        ParseUser currentUser = ParseUser.getCurrentUser();
        //Double hotspotLat = Double.parseDouble(hotspotLatEditText.getText().toString().trim());
        //Double hotspotLong = Double.parseDouble(hotspotLongEditText.getText().toString().trim());
        Double hotspotLat = 37.775;
        Double hotspotLong = -122.4183333;

        savePrivateProfile(hotspotLat, hotspotLong);
        setUserProfileComplete(currentUser);

        Intent intent = new Intent(ProfileHotspotInfoActivity.this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void savePrivateProfile(Double hotspotLat, Double hotspotLong) {
        PrivateProfile privProfile = (PrivateProfile) ParseUser.getCurrentUser().get("privateProfile");

        ArrayList<ParseGeoPoint> hotspots = new ArrayList<ParseGeoPoint>();
        hotspots.add(new ParseGeoPoint(hotspotLat, hotspotLong));

        privProfile.setPreferredHotspots(hotspots);
        privProfile.saveInBackground();
    }

    private void setUserProfileComplete(ParseUser user) {
        user.put("profileComplete", true);
        user.saveInBackground();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            try {
                mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                        .getMap();
            } catch (NullPointerException e) {
                // Handle null pointer exception
            }
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(37.775, -122.4183333))      // Sets the center of the map
                .zoom(10)
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mMap.addMarker(new MarkerOptions().position(new LatLng(37.775, -122.4183333)).title("San Francisco"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(37.783973,-122.401375 )).title("Moscone"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(37.776439,-122.394256 )).title("SF Caltrain"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(37.600675,-122.385764 )).title("Millbrae BART"));
    }


}
