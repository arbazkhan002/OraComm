package com.gogreen.greenmachine.main.match;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.interBack.InterBack;
import com.gogreen.greenmachine.main.MainActivity;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;


public class RiderMatchedActivity extends ActionBarActivity implements OnMapReadyCallback {

    private Toolbar toolbar;
    private GoogleMap mMap;

    private ParseGeoPoint driverLocation;
    private ParseGeoPoint hotspotLocation;
    private String driverPhone;
    private String driverName;
    private String driverCar;

    private TextView mDriverPhoneTextView;
    private TextView mDriverName;
    private TextView mDriverCar;
    private Button mRideComplete;

    private MatchRoute mRoute;

    public ParseGeoPoint getDriverLocation() {
        return driverLocation;
    }

    public void setDriverLocation(ParseGeoPoint driverLocation) {
        this.driverLocation = driverLocation;
    }

    public ParseGeoPoint getHotspotLocation() {
        return hotspotLocation;
    }

    public void setHotspotLocation(ParseGeoPoint hotspotLocation) {
        this.hotspotLocation = hotspotLocation;
    }

    public String getDriverCar() {
        return driverCar;
    }

    public void setDriverCar(String driverCar) {
        this.driverCar = driverCar;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public MatchRoute getmRoute() {
        return mRoute;
    }

    public void setmRoute(MatchRoute mRoute) {
        this.mRoute = mRoute;
    }

    InterBack backend = new InterBack();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_matched);

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        // Grab info to populate to a rider (about a driver)
        backend.getRiderInfo(new RiderMatchedActivity[]{this});
        mapFragment.getMapAsync(this);

        mDriverPhoneTextView = (TextView) findViewById(R.id.driver_phone_text);
        mDriverPhoneTextView.setText(this.driverPhone);

        mDriverName = (TextView) findViewById(R.id.driver_name_text);
        mDriverName.setText(this.driverName);

        mDriverCar = (TextView) findViewById(R.id.driver_car_text);
        mDriverCar.setText(this.driverCar);

        mRideComplete = (Button) findViewById(R.id.button_ride_complete);
        mRideComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRoute.setStatus(MatchRoute.TripStatus.COMPLETED);
                mRoute.saveInBackground();
                Intent intent = new Intent(RiderMatchedActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        ImageView callButton = (ImageView) findViewById(R.id.call);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Allow a phone call
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + driverPhone));
                startActivity(callIntent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rider_matched, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        double driverLat = this.driverLocation.getLatitude();
        double driverLong = this.driverLocation.getLongitude();

        double hotspotLat = this.hotspotLocation.getLatitude();
        double hotspotLong = this.hotspotLocation.getLongitude();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(driverLat, driverLong))      // Sets the center of the map
                .zoom(10)
                .build();                   // Creates a CameraPosition from the builder
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        LatLng driverLoc = new LatLng(driverLat, driverLong);
        LatLng hotspotLoc = new LatLng(hotspotLat, hotspotLong);
        mMap.addMarker(new MarkerOptions().position(driverLoc)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_black))
                .alpha(0.75f));
        mMap.addMarker(new MarkerOptions().position(hotspotLoc)
                .icon(BitmapDescriptorFactory.defaultMarker(30))
                .alpha(0.75f));
    }
}
