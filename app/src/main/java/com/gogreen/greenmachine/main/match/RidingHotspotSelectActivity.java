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
import com.gogreen.greenmachine.interBack.InterBack;
import com.gogreen.greenmachine.interBack.objects.InterUser;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRequest;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
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

    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private final static String LOCATION_KEY = "location-key";
    private final static String HOTSPOTS_KEY = "hotspots";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private double mLatitude;
    private double mLongitude;
    private boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private GoogleMap mMap;

    private Toolbar toolbar;

    private MatchRequest riderRequest;
    private MatchRoute matchRoute;
    private Set<Hotspot> serverHotspots;
    private Set<Hotspot> selectedHotspots;
    private Date matchByDate;
    private Date arriveByDate;
    private boolean riderMatched;
    private MatchRoute.Destination destination;
    private InterBack backend = new InterBack();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riding_hotspot_select);

        // Process intent items
        this.matchByDate = Utils.getInstance().convertToDateObject(getIntent().getExtras().get("matchDate").toString());
        this.arriveByDate = Utils.getInstance().convertToDateObject(getIntent().getExtras().get("arriveDate").toString());
        this.destination = processDestination(getIntent().getExtras().get("destination").toString());

        // Turn on location updates
        this.mRequestingLocationUpdates = true;

        this.riderMatched = false;

        // Grab server hotspots
        this.serverHotspots = backend.getAllHotspots();

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
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

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
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }


    private void updateLocation() {
        if (mCurrentLocation != null){
            mLatitude = mCurrentLocation.getLatitude();
            mLongitude = mCurrentLocation.getLongitude();
            backend.setUserLastKnownLocation(mLatitude, mLongitude);
        }
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

        // Save selected hotspots
        Iterator selectedHspotsIter = this.selectedHotspots.iterator();
        List<String> hSpotIds = new ArrayList<>();
        while (selectedHspotsIter.hasNext()) {
            Hotspot h = (Hotspot) selectedHspotsIter.next();
            hSpotIds.add(h.getObjectId());
        }
        savedInstanceState.putStringArrayList(HOTSPOTS_KEY, (ArrayList) hSpotIds);
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

            if (savedInstanceState.keySet().contains(HOTSPOTS_KEY)) {
                List<String> selectedHspotList = savedInstanceState.getStringArrayList(HOTSPOTS_KEY);
                Set<String> selectedHspotStrings = new HashSet<>(selectedHspotList);

                Iterator serverHotspotIter = this.serverHotspots.iterator();
                while (serverHotspotIter.hasNext()) {
                    Hotspot h = (Hotspot) serverHotspotIter.next();
                    if (selectedHspotStrings.contains(h.getObjectId())) {
                        // Set the opacity of the marker if it is actually a marker on the map
                        this.selectedHotspots.add(h);
                    }
                }
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
                backend.fetchIfNeeded(h);
                LatLng hotspotLoc = h.getHotspotLocation();
                MarkerOptions markerOptions = new MarkerOptions().position(hotspotLoc)
                        .icon(BitmapDescriptorFactory.defaultMarker(30))
                        .title(h.getName())
                        .alpha(0.75f);
                Marker marker = mMap.addMarker(markerOptions);
                mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener)this);

                if (this.selectedHotspots.contains(h)) {
                    setMarker(marker);
                }
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

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            backend.fetchIfNeeded(hSpot);
            if (hSpot.isHotspotAtLocation(mPoint)) {
                this.selectedHotspots.add(hSpot);
                break;
            }
        }
    }

    public void resetMarker(Marker m) {
        m.setAlpha(0.75f);
        m.setIcon(BitmapDescriptorFactory.defaultMarker(30));

        LatLng mPoint = m.getPosition();

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            backend.fetchIfNeeded(hSpot);
            if (hSpot.isHotspotAtLocation(mPoint)) {
                this.selectedHotspots.remove(hSpot);
                break;
            }
        }
    }

    private boolean createMatchRequest() {
        // Create a match request
        this.riderRequest = new MatchRequest();
        MatchRequest marray[] = new MatchRequest[]{riderRequest};
        return backend.sendDriverRequest(marray, selectedHotspots, matchByDate, arriveByDate, MatchRequest.MatchStatus.ACTIVE);
    }

    // Check if c1 is within c2's window of seconds
    private boolean isInTimeWindow(Calendar c1, Calendar c2, int seconds) {
        Calendar after = c2;
        after.add(Calendar.SECOND, seconds);
        Date highTime = after.getTime();

        Calendar before = c2;

        // since 'add' works by reference subtract the time added above first
        before.add(Calendar.SECOND, -2 * seconds);

        Date lowTime = before.getTime();
        Date c1Time = c1.getTime();

        // restore the calendar's time to original before returning control
        before.add(Calendar.SECOND, seconds);
        if ((c1Time.compareTo(highTime) <= 0) && (c1Time.compareTo(lowTime) >= 0)) {
            return true;
        }

        return false;
    }

    private boolean findDriver() {
        // Grab all routes from the server
        List<MatchRoute> matchRoute = backend.fetchAllRoutes();

        // Loop through routes to find a route for the same hotspots
        Iterator iter = matchRoute.iterator();
        while (iter.hasNext()) {
            MatchRoute route = (MatchRoute) iter.next();
            ArrayList<Hotspot> potentialHotspots = route.getPotentialHotspots();
            // First check if the potential hotspots is empty. It is cleared if there was a prev. match
            int remainingCapacity = route.getCapacity();

            Calendar routeCal = Calendar.getInstance();
            Calendar myCal = Calendar.getInstance();

            routeCal.setTime(route.getArriveBy());
            myCal.setTime(this.arriveByDate);

            if (potentialHotspots.isEmpty() && remainingCapacity > 0
                    && isInTimeWindow(routeCal,myCal,600)) {
                // Use the hotspot
                Hotspot routeHotspot = route.getHotspot();
                backend.fetchIfNeeded(routeHotspot);

                // Check if the route hotspot is in selected hotspots
                if (this.selectedHotspots.contains(routeHotspot)) {
                    // If it is check that the rider isn't already in the route rider (sync issues)
                    InterUser user = new InterUser();
                    user.setUser(backend.getCurrentUser());
                    boolean alreadyRider = backend.isRiderInRoute(user, route);
                    if (!alreadyRider) {
                        this.matchRoute = route;
                        this.matchRoute.setCapacity(remainingCapacity - 1);
                        this.matchRoute.addRider(user);
                        this.matchRoute.setStatus(MatchRoute.TripStatus.EN_ROUTE_HOTSPOT);
                        try {
                            this.matchRoute.save();
                            return true;
                        } catch (ParseException e) {
                            return false;
                        }
                    }
                }
            } else if (remainingCapacity > 0  && isInTimeWindow(routeCal,myCal,600)) {
                // Search for an intersection to initialize the hotspot and clear the online potentialHotspots list
                Set<Hotspot> routesOnline = new HashSet<Hotspot>(potentialHotspots);
                Set<Hotspot> intersection = new HashSet<Hotspot>(selectedHotspots);

                intersection.retainAll(routesOnline);

                if (!intersection.isEmpty()) {
                    Iterator hspotIterator = intersection.iterator();
                    Hotspot hotspot = (Hotspot) hspotIterator.next();
                    backend.fetchIfNeeded(hotspot);
                    if (remainingCapacity > 0) {
                        InterUser user = new InterUser();
                        user.setUser(backend.getCurrentUser());
                        this.matchRoute = route;
                        this.matchRoute.setCapacity(remainingCapacity - 1);
                        this.matchRoute.addRider(user);
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
            if (!riderMatched) {
                riderRequest.saveInBackground();
            }
            pdLoading.dismiss();
            processResult();
        }
    }
}