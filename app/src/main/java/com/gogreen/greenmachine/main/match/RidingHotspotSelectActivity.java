package com.gogreen.greenmachine.main.match;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRequest;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RidingHotspotSelectActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private double mLatitude;
    private double mLongitude;
    private boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;
    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private final static String LOCATION_KEY = "location-key";
    private String mLastUpdateTime;
    private GoogleMap mMap;

    private Toolbar toolbar;

    private MatchRequest riderRequest;
    private MatchRoute matchRoute;
    private Set<Hotspot> serverHotspots;
    private Set<Hotspot> selectedHotspots;
    private Date matchByDate;
    private Date arriveByDate;
    private boolean riderMatched = false;
    private MatchRoute.Destination destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riding_hotspot_select);

        // Process intent items
        this.matchByDate = convertToDateObject(getIntent().getExtras().get("matchDate").toString());
        this.arriveByDate = convertToDateObject(getIntent().getExtras().get("arriveDate").toString());
        this.destination = processDestination(getIntent().getExtras().get("destination").toString());

        // Turn on location updates
        this.mRequestingLocationUpdates = true;

        this.riderMatched = false;

        // Grab server hotspots
        this.serverHotspots = getAllHotspots();

        // Initialize selectedHotspots
        this.selectedHotspots = new HashSet<Hotspot>();

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Initialize selectedHotspots
        this.selectedHotspots = new HashSet<Hotspot>();

        ButtonFloat matchButton = (ButtonFloat) findViewById(R.id.buttonFloat);
        matchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new FindMatchTask().execute();
            }
        });

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rider_hotspot_select, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private void startNextActivity() {
        Intent intent = new Intent(RidingHotspotSelectActivity.this, RiderMatchedActivity.class);
        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = (mLastLocation.getLatitude());
            mLongitude = (mLastLocation.getLongitude());
        }
        else {
            Toast.makeText(getApplicationContext(),"Please turn on location.", Toast.LENGTH_SHORT).show();
        }
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        //map can be loaded after the current location is known
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int code) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void updateLocation() {
        mLatitude = mCurrentLocation.getLatitude();
        mLongitude = mCurrentLocation.getLongitude();

        // Fetch user's public profile
        ParseUser currentUser = ParseUser.getCurrentUser();
        PublicProfile pubProfile = (PublicProfile) currentUser.get("publicProfile");
        Utils.getInstance().fetchParseObject(pubProfile);

        // Insert coordinates into the user's public profile lastKnownLocation
        ParseGeoPoint userLoc = new ParseGeoPoint(mLatitude, mLongitude);
        pubProfile.setLastKnownLocation(userLoc);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateLocation();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private Date convertToDateObject(String s) {
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd h:m a");
        Calendar cal = Calendar.getInstance();
        String input = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DATE)
                + " " + s;

        Date t = new Date();

        try {
            t = ft.parse(input);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return t;
    }

    private MatchRoute.Destination processDestination(String s) {
        if (s.equals("HQ")) {
            return MatchRoute.Destination.HQ;
        } else {
            return MatchRoute.Destination.HOME;
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLatitude, mLongitude))      // Sets the center of the map
                .zoom(10)
                .build();                   // Creates a CameraPosition from the builder
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.serverHotspots != null) {
            Iterator iter = this.serverHotspots.iterator();
            while (iter.hasNext()) {
                Hotspot h = (Hotspot) iter.next();
                ParseGeoPoint parsePoint = h.getParseGeoPoint();
                LatLng hotspotLoc = new LatLng(parsePoint.getLatitude(), parsePoint.getLongitude());
                mMap.addMarker(new MarkerOptions().position(hotspotLoc)
                                .icon(BitmapDescriptorFactory.defaultMarker(30))
                                .title(h.getName())
                                .alpha(0.75f)
                );
                mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener)this);
            }
        } else {
            // Handle the server not getting hotspots
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public boolean onMarkerClick(Marker m) {
        if (m.getAlpha() == 0.75f) {
            setMarker(m);
        }
        else{
            resetMarker(m);
        }

        return true;
    }

    public void setMarker(Marker m) {
        m.setIcon(BitmapDescriptorFactory.defaultMarker(150));
        m.setAlpha(1.0f);
        // Grab location & add to Hotspot set
        LatLng mPoint = m.getPosition();
        ParseGeoPoint hPoint = new ParseGeoPoint(mPoint.latitude, mPoint.longitude);

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            Utils.getInstance().fetchParseObject(hSpot);
            if (isEqualParseGeoPoint(hPoint, hSpot.getParseGeoPoint())) {
                this.selectedHotspots.add(hSpot);
                break;
            }
        }
    }

    private boolean isEqualParseGeoPoint(ParseGeoPoint p1, ParseGeoPoint p2) {
        return (p1.getLatitude() == p2.getLatitude() && p1.getLongitude() == p2.getLongitude());
    }

    public void resetMarker(Marker m) {
        m.setAlpha(0.75f);
        m.setIcon(BitmapDescriptorFactory.defaultMarker(30));

        LatLng mPoint = m.getPosition();
        ParseGeoPoint hPoint = new ParseGeoPoint(mPoint.latitude, mPoint.longitude);

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            Utils.getInstance().fetchParseObject(hSpot);
            if (isEqualParseGeoPoint(hPoint, hSpot.getParseGeoPoint())) {
                this.selectedHotspots.remove(hSpot);
                break;
            }
        }
    }

    private Set<Hotspot> getAllHotspots() {
        // Grab the hotspot set from the server
        ParseQuery<Hotspot> hotspotQuery = ParseQuery.getQuery("Hotspot");
        hotspotQuery.orderByDescending("hotspotId");
        try {
            serverHotspots = new HashSet<Hotspot>(hotspotQuery.find());
        } catch (ParseException e) {
            // Handle a server query fail
            return null;
        }

        return serverHotspots;
    }

    private boolean createMatchRequest() {
        // Create a match request
        this.riderRequest = new MatchRequest();
        this.riderRequest.populateMatchRequest(ParseUser.getCurrentUser(), this.selectedHotspots,
                matchByDate, arriveByDate, MatchRequest.MatchStatus.ACTIVE);
        try {
            this.riderRequest.save();
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    //Check if c1 is within c2's window of seconds
    private boolean isInTimeWindow(Calendar c1, Calendar c2, int seconds) {
        Calendar after = c2;
        after.add(Calendar.SECOND, seconds);
        Date highTime = after.getTime();

        Calendar before = c2;

        //since 'add' works by reference, subtract the time added above first
        before.add(Calendar.SECOND, -2*seconds);

        Date lowTime = before.getTime();
        Date c1Time = c1.getTime();

        //restore the calendar's time to original before returning control
        before.add(Calendar.SECOND, seconds);
        if ((c1Time.compareTo(highTime) <= 0) && (c1Time.compareTo(lowTime) >= 0)) {
            return true;
        }

        return false;

    }

    private boolean findDriver() {
        List<MatchRoute> matchRoute;

        // Grab all routes from the server
        ParseQuery<MatchRoute> notStartedQuery = ParseQuery.getQuery("MatchRoute");
        notStartedQuery = notStartedQuery.whereEqualTo("tripStatus", MatchRoute.TripStatus.NOT_STARTED.toString());

        ParseQuery<MatchRoute> enRouteQuery = ParseQuery.getQuery("MatchRoute");
        enRouteQuery = enRouteQuery.whereEqualTo("tripStatus", MatchRoute.TripStatus.EN_ROUTE_HOTSPOT.toString());

        //ParseQuery.or
        ParseQuery<MatchRoute> finalQuery = ParseQuery.or(Arrays.asList(notStartedQuery, enRouteQuery));

        try {
            matchRoute = new ArrayList<>(finalQuery.find());
        } catch (ParseException e) {
            // Handle server retrieval failure
            return false;
        }

        // Loop through routes to find a route for the same hotspots
        Iterator iter = matchRoute.iterator();
        while (iter.hasNext()) {
            MatchRoute route = (MatchRoute) iter.next();
            ArrayList<Hotspot> potentialHotspots = route.getPotentialHotspots();
            // First check if the potential hotspots is empty. It is cleared if there was a prev. match
            int remainingCapacity = route.getCapacity();

            Calendar route_cal = Calendar.getInstance();
            Calendar my_cal = Calendar.getInstance();
            route_cal.setTime(route.getArriveBy());
            my_cal.setTime(this.arriveByDate);


            //If the ride is in my time window, get a match
            if (potentialHotspots.isEmpty() && remainingCapacity > 0 && isInTimeWindow(route_cal,my_cal,600)) {
                // Use the hotspot

                Hotspot routeHotspot = route.getHotspot();
                Utils.getInstance().fetchParseObject(routeHotspot);

                // Check if the route hotspot is in selected hotspots
                if (this.selectedHotspots.contains(routeHotspot)) {
                    // If it is check that the rider isn't already in the route rider (sync issues)
                    boolean alreadyRider = false;
                    ArrayList<PublicProfile> riders = (ArrayList<PublicProfile>) route.getRiders();
                    PublicProfile myProfile = (PublicProfile) ParseUser.getCurrentUser().get("publicProfile");
                    Utils.getInstance().fetchParseObject(myProfile);

                    Iterator profIterator = riders.iterator();
                    while (profIterator.hasNext()) {
                        PublicProfile riderProfile = (PublicProfile) profIterator.next();
                        if (riderProfile.equals(myProfile)) {
                            alreadyRider = true;
                            break;
                        }
                    }
                    if (!alreadyRider) {
                        this.matchRoute = route;
                        this.matchRoute.setCapacity(remainingCapacity - 1);
                        this.matchRoute.addRider(myProfile);
                        this.matchRoute.setStatus(MatchRoute.TripStatus.EN_ROUTE_HOTSPOT);
                        try {
                            this.matchRoute.save();
                            return true;
                        } catch (ParseException e) {
                            return false;
                        }
                    }
                }
            } else if (remainingCapacity > 0 && isInTimeWindow(route_cal,my_cal,600)) {
                // Search for an intersection to initialize the hotspot and clear the online potentialHotspots list
                Set<Hotspot> routesOnline = new HashSet<Hotspot>(potentialHotspots);
                Set<Hotspot> intersection = new HashSet<Hotspot>(selectedHotspots);

                intersection.retainAll(routesOnline);

                if (!intersection.isEmpty()) {
                    Iterator hspotIterator = intersection.iterator();
                    Hotspot hotspot = (Hotspot) hspotIterator.next();
                    Utils.getInstance().fetchParseObject(hotspot);
                    if (remainingCapacity > 0) {
                        PublicProfile myProfile = (PublicProfile) ParseUser.getCurrentUser().get("publicProfile");
                        Utils.getInstance().fetchParseObject(myProfile);
                        this.matchRoute = route;
                        this.matchRoute.setCapacity(remainingCapacity - 1);
                        this.matchRoute.addRider(myProfile);
                        this.matchRoute.setPotentialHotspots(new ArrayList<Hotspot>());
                        this.matchRoute.setStatus(MatchRoute.TripStatus.EN_ROUTE_HOTSPOT);
                        this.matchRoute.setHotspot(hotspot);
                        try {
                            this.matchRoute.save();
                            return true;
                        } catch (ParseException e) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void processResult() {
        if (!this.riderMatched) {
            Toast.makeText(RidingHotspotSelectActivity.this, getString(R.string.progress_no_driver_found), Toast.LENGTH_SHORT).show();
        } else {
            startNextActivity();
        }
    }

    private class FindMatchTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(RidingHotspotSelectActivity.this);
        boolean requestCreated = false;
        boolean driverFound = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading.setMessage(getString(R.string.progress_matching_rider));
            pdLoading.show();
            pdLoading.setCancelable(false);
            pdLoading.setCanceledOnTouchOutside(false);
        }
        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < 100; i++) {
                if (!requestCreated) {
                    requestCreated = createMatchRequest();
                } else if (!driverFound) {
                    driverFound = findDriver();
                } else if (driverFound) {
                }
            }

            riderMatched = driverFound;
            riderRequest.setStatus(MatchRequest.MatchStatus.INACTIVE);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pdLoading.dismiss();
            processResult();
        }
    }
}