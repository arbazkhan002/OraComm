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
import com.gogreen.greenmachine.distmatrix.RetrieveDistanceMatrix;
import com.gogreen.greenmachine.interBack.InterBack;
import com.gogreen.greenmachine.main.MainActivity;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class DriverMatchedActivity extends ActionBarActivity implements OnMapReadyCallback {

    private Toolbar toolbar;

    private GoogleMap mMap;
    private ArrayList<ParseGeoPoint> riderLocations;
    private ParseGeoPoint hotspotLocation;

    private TextView mRiderText;
    private TextView mRiderPhoneTextView;
    private String riderNumber;
    private Button mRideComplete;

    private MatchRoute mRoute;

    InterBack backend = new InterBack();

    static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                                                                                @Override
                                                                                public void initialize(HttpRequest request) {
                                                                                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                                                                                }
                                                                            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_matched);
        String origins="37.5505658,-122.3094177";
        String destinations="37.5505658,-122.3094177";
        String mode=getString(R.string.driving_mode);
        String language=getString(R.string.us_english);
        String key=getString(R.string.google_maps_key);
        String urlString=getString(R.string.distmatrixURL);

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        GenericUrl url = new GenericUrl(urlString);
        url.put("origins", origins);
        url.put("destinations", destinations);
        url.put("mode", mode);
        url.put("language", language);
        url.put("key", key);
        new RetrieveDistanceMatrix().execute(url);

        // Initialize rider textview
        this.mRiderText = (TextView) findViewById(R.id.rider_name_text);

        // Initialize riders to be empty
        this.riderLocations = new ArrayList<ParseGeoPoint>();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        getInfo();
        mapFragment.getMapAsync(this);

        mRiderPhoneTextView = (TextView) findViewById(R.id.rider_phone_text);
        mRiderPhoneTextView.setText(riderNumber);

        mRideComplete = (Button) findViewById(R.id.button_ride_complete);
        mRideComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRoute.setStatus(MatchRoute.TripStatus.COMPLETED);
                mRoute.saveInBackground();
                Intent intent = new Intent(DriverMatchedActivity.this, MainActivity.class);
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
                callIntent.setData(Uri.parse("tel:" + riderNumber));
                startActivity(callIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_driver_matched, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void getInfo() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        List<MatchRoute> matchRoutes = new ArrayList<MatchRoute>();
        boolean foundRoute = false;

        // Query for all MatchRoutes
        ParseQuery<MatchRoute> matchRoutesQuery = ParseQuery.getQuery("MatchRoute");
        try {
            matchRoutes = new ArrayList<MatchRoute>(matchRoutesQuery.find());
        } catch (ParseException e) {
            // handle later since low on time
        }

        Iterator routeIterator = matchRoutes.iterator();
        while (routeIterator.hasNext() && !foundRoute) {
            MatchRoute route = (MatchRoute) routeIterator.next();
            backend.fetchIfNeeded(route);

            if (route.getDriver().getObjectId().equals(currentUser.getObjectId())) {
                ArrayList<PublicProfile> riders = route.getRiders();
                Iterator ridersIter = riders.iterator();
                while (ridersIter.hasNext()) {
                    PublicProfile riderProfile = (PublicProfile) ridersIter.next();
                    backend.fetchIfNeeded(riderProfile);

                    this.mRiderText.setText(riderProfile.getFirstName());

                    ParseGeoPoint riderLocation = riderProfile.getLastKnownLocation();
                    this.riderLocations.add(riderLocation);
                    this.riderNumber = riderProfile.getPhoneNumber();
                }

                Hotspot hotspot = route.getHotspot();
                backend.fetchIfNeeded(hotspot);

                this.hotspotLocation = hotspot.getParseGeoPoint();
                this.mRoute = route;
                foundRoute = true;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        ParseUser currUser = ParseUser.getCurrentUser();
        PublicProfile myProfile = (PublicProfile) currUser.get("publicProfile");
        backend.fetchIfNeeded(myProfile);
        ParseGeoPoint myLoc = myProfile.getLastKnownLocation();

        double hotspotLat = this.hotspotLocation.getLatitude();
        double hotspotLong = this.hotspotLocation.getLongitude();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()))      // Sets the center of the map
                .zoom(10)
                .build();                   // Creates a CameraPosition from the builder
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        LatLng hotspotLoc = new LatLng(hotspotLat, hotspotLong);

        Iterator riderIter = this.riderLocations.iterator();
        while (riderIter.hasNext()) {
            ParseGeoPoint riderLoc = (ParseGeoPoint) riderIter.next();
            LatLng riderMarker = new LatLng(riderLoc.getLatitude(), riderLoc.getLongitude());
            mMap.addMarker(new MarkerOptions().position(riderMarker)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_rider))
                    .alpha(0.75f));
        }
        mMap.addMarker(new MarkerOptions().position(hotspotLoc)
                .icon(BitmapDescriptorFactory.defaultMarker(30))
                .alpha(0.75f));
    }
}
