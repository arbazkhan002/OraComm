package com.gogreen.greenmachine.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonRectangle;
import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.distmatrix.Element;
import com.gogreen.greenmachine.distmatrix.Result;
import com.gogreen.greenmachine.distmatrix.RetrieveDistanceMatrix;
import com.gogreen.greenmachine.distmatrix.Row;
import com.gogreen.greenmachine.main.badges.BadgeActivity;
import com.gogreen.greenmachine.main.login.DispatchActivity;
import com.gogreen.greenmachine.main.match.DrivingActivity;
import com.gogreen.greenmachine.main.match.RidingActivity;
import com.gogreen.greenmachine.main.navigation.AboutUsActivity;
import com.gogreen.greenmachine.main.navigation.NavDrawerAdapter;
import com.gogreen.greenmachine.main.navigation.SettingsActivity;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Tuple;
import com.gogreen.greenmachine.util.Utils;
import com.gogreen.greenmachine.util.Tuple;
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
import com.google.api.client.http.GenericUrl;
import com.parse.LogOutCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private double mLatitude;
    private double mLongitude;

    private boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;

    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private final static String LOCATION_KEY = "location-key";

    private ParseUser currUser;
    private PrivateProfile privProfile;
    private String firstName;
    private String lastName;

    private String mLastUpdateTime;

    // Menu positions
    private final int HOME = 1;
    private final int BADGES = 2;
    private final int HOTSPOTS = 3;
    private final int ABOUT_US = 4;
    private final int LOGOUT = 5;

    // Navigation Drawer resources
    private RecyclerView.LayoutManager mLayoutManager;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawer;

    // Nav Drawer Display Resources
    private String navDrawerName;
    private String navDrawerEmail;
    private int navDrawerProfileImage;

    private Set<Hotspot> serverHotspots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize location updates
        this.mRequestingLocationUpdates = true;

        // Fetch the parse objects needed
        fetchParseObjects();

        // Grab appropriate data for adapter
        String[] navRowTitles = getResources().getStringArray(R.array.navigation_drawer_titles);
        TypedArray navRowIcons = getResources().obtainTypedArray(R.array.navigation_drawer_icons);

        // Set up the toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up recycler and provide it with the proper adapter
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView);
        mRecyclerView.setHasFixedSize(true);

        navDrawerName = this.firstName + " " + this.lastName;
        navDrawerProfileImage = R.drawable.jonathan_lui;

        RecyclerView.Adapter mAdapter = new NavDrawerAdapter(navRowTitles, navRowIcons,
                navDrawerName, navDrawerEmail, navDrawerProfileImage, this);
        mRecyclerView.setAdapter(mAdapter);

        // Navigation Drawer listeners
        final GestureDetector mGestureDetector = new GestureDetector(MainActivity.this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if(child != null && mGestureDetector.onTouchEvent(motionEvent)){
                    int childPosition = recyclerView.getChildPosition(child);
                    mDrawer.closeDrawers();
                    switch(childPosition) {
                        case HOME:
                            return true;
                        case BADGES:
                            startActivity(new Intent(MainActivity.this, BadgeActivity.class));
                            return true;
                        case HOTSPOTS:
                            return true;
                        case ABOUT_US:
                            startActivity(new Intent(MainActivity.this, AboutUsActivity.class));
                            return true;
                        case LOGOUT:
                            logout();
                            return true;
                        default:
                            return false;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }
        });

        // Set the proper layout
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Set the drawer
        this.mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.mDrawerToggle = new ActionBarDrawerToggle(this,
                this.mDrawer,
                mToolbar,
                R.string.open_drawer,
                R.string.close_drawer) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                syncState();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                syncState();
            }
        };
        this.mDrawer.setDrawerListener(mDrawerToggle);
        this.mDrawerToggle.syncState();

        // Set up the handler for the driving button click
        ButtonRectangle drivingButton = (ButtonRectangle) findViewById(R.id.driving_button);
        drivingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DrivingActivity.class);
                startActivity(intent);
            }
        });

        // Set up the handler for the riding button click
        ButtonRectangle ridingButton = (ButtonRectangle) findViewById(R.id.riding_button);
        ridingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RidingActivity.class);
                startActivity(intent);
            }
        });

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_user:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            mLatitude = (mLastLocation.getLatitude());
            mLongitude = (mLastLocation.getLongitude());
        }
        else {
            Toast.makeText(getApplicationContext(), R.string.notification_location_on, Toast.LENGTH_SHORT).show();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        // Load the Google Map Fragment
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

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLatitude, mLongitude))
                .zoom(10)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.serverHotspots != null) {
            Iterator iter = this.serverHotspots.iterator();
            while (iter.hasNext()) {
                Hotspot h = (Hotspot) iter.next();
                Utils.getInstance().fetchParseObject(h);
                ParseGeoPoint parsePoint = h.getParseGeoPoint();
                LatLng hotspotLoc = new LatLng(parsePoint.getLatitude(), parsePoint.getLongitude());
                Marker m = mMap.addMarker(new MarkerOptions().position(hotspotLoc)
                                .icon(BitmapDescriptorFactory.defaultMarker(30))
                                .title("Next Pickup: N/A")
                                .alpha(0.75f)
                );

                mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener)this);
            }
        } else {
            // Handle the server not getting hotspots
        }

        // Enable My Location layer on the map
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateLocation();
        simulateDriverStep();
    }

    @Override
    public boolean onMarkerClick(Marker m) {
        updateMarkerTags(m);
        return false;
    }

    private void updateLocation() {
        if (mCurrentLocation != null){
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
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
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

    public int getHotspotId(Marker m){
        Iterator iter = this.serverHotspots.iterator();
        int i = 1;
        while (iter.hasNext()) {
            Hotspot h = (Hotspot) iter.next();
            Utils.getInstance().fetchParseObject(h);
            ParseGeoPoint parsePoint = h.getParseGeoPoint();
            LatLng hotspotLoc = new LatLng(parsePoint.getLatitude(), parsePoint.getLongitude());
            if (m.getPosition().longitude == parsePoint.getLongitude() && m.getPosition().latitude == parsePoint.getLatitude()){
                return i;
            }
            i += 1;
        }
        return -1;
    }

    public Hotspot getMyHotspot(Marker m){
        Iterator iter = this.serverHotspots.iterator();
        int i = 1;
        while (iter.hasNext()) {
            Hotspot h = (Hotspot) iter.next();
            Utils.getInstance().fetchParseObject(h);
            ParseGeoPoint parsePoint = h.getParseGeoPoint();
            LatLng hotspotLoc = new LatLng(parsePoint.getLatitude(), parsePoint.getLongitude());
            if (m.getPosition().longitude == parsePoint.getLongitude() && m.getPosition().latitude == parsePoint.getLatitude()){
                return h;
            }
            i += 1;
        }
        return (Hotspot)null;
    }

    public void updateMarkerTags(Marker m){
        Hotspot h = getMyHotspot(m);
        if (h == null){
            m.setTitle("Next Pickup: N/A");
            m.setSnippet("no drivers yet");
            return;
        }
        else {
            String origins = "";
            try {
                Marker marray[] = new Marker[]{m};
                new updateMarkers().execute(new Tuple<Hotspot,Marker[]>(h,marray));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    
    public ArrayList<PublicProfile> getActiveDrivers(Hotspot h){
        ArrayList<MatchRoute> matchRoute;
        ArrayList<PublicProfile> driversPubProf = new ArrayList<PublicProfile>();
        ParseQuery<MatchRoute> query = ParseQuery.getQuery("MatchRoute");

        try {
            matchRoute = new ArrayList<MatchRoute>(query.find());
            for(MatchRoute r:matchRoute){
                ParseUser driver = r.getDriver();
                try {
                    driver.fetchIfNeeded();
                } catch (ParseException e) {

                }
                PublicProfile pubProfile= (PublicProfile) driver.get("publicProfile");
                Utils.getInstance().fetchParseObject(pubProfile);

                ParseGeoPoint lkl = pubProfile.getLastKnownLocation();

                Hotspot g = r.getHotspot();
                if (g != null) {
                    if (g.getObjectId() == h.getObjectId()) {
                        driversPubProf.add(pubProfile);
                    }
                }
            }
            return driversPubProf;

        } catch (ParseException e) {
            // Handle server retrieval failure
            e.printStackTrace();
            return null;
        }
    }

    private void fetchParseObjects() {
        this.currUser = ParseUser.getCurrentUser();

        this.navDrawerEmail = currUser.getEmail();

        this.privProfile = (PrivateProfile) currUser.get("privateProfile");

        try {
            this.privProfile.fetchIfNeeded();
            this.firstName = this.privProfile.getFirstName();
            this.lastName = this.privProfile.getLastName();
        } catch (ParseException e) {
            logout();
        }

        this.serverHotspots = getAllHotspots();

    }

    private Set<Hotspot> getAllHotspots() {
        // Grab the hotspot set from the server
        Set<Hotspot> hSpots;
        ParseQuery<Hotspot> hotspotQuery = ParseQuery.getQuery("Hotspot");
        hotspotQuery.orderByDescending("hotspotId");
        try {
            hSpots = new HashSet<Hotspot>(hotspotQuery.find());
        } catch (ParseException e) {
            // Handle a server query fail
            return null;
        }

        return hSpots;
    }

    public LatLng makeLatLng(double a, double b){
        return new LatLng(a,b);
    }

    public void simulateDriverStep(){
        if (simulateStep == 0){
            simulatedDriver = mMap.addMarker(new MarkerOptions().position(simulatePoints.get(0))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_black))
                    .title("Jaden Smith")
                    .alpha(0.75f));

            simulatedDriver_2 = mMap.addMarker(new MarkerOptions().position(simulatePoints_2.get(0))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_black))
                    .title("Will Smith")
                    .alpha(0.75f));

            simulateStep += 1;

        }
        else{
            if (simulateStep<simulatePoints.size()){
                simulatedDriver.setPosition(simulatePoints.get(simulateStep));

            }
            if (simulateStep<simulatePoints_2.size()){
                simulatedDriver_2.setPosition(simulatePoints_2.get(simulateStep));
            }

            simulateStep += 1;
            if (simulateStep>Math.min(simulatePoints.size()-1,simulatePoints_2.size()-1))
                simulateStep = Math.min(simulatePoints.size()-1,simulatePoints_2.size()-1);
        }
    }

    public List<LatLng> simulatePoints = Arrays.asList(
            makeLatLng(37.6476749,-122.4066639),
            makeLatLng(37.641694,-122.405891),
            makeLatLng(37.6347100,-122.4067497),
            makeLatLng(37.634042,-122.4136162),
            makeLatLng(37.6338779,-122.4170494),
            makeLatLng(37.6330622,-122.4191952),
            makeLatLng(37.6307511,-122.4178219),
            makeLatLng(37.6292556,-122.4167061),
            makeLatLng(37.6270124,-122.415247),
            makeLatLng(37.6246331,-122.4138737),
            makeLatLng(37.6226617,-122.4121571),
            makeLatLng(37.6202823,-122.4104404),
            makeLatLng(37.6155912,-122.4064922),
            makeLatLng(37.615035, -122.405966),
            makeLatLng(37.614185, -122.405140),
            makeLatLng(37.614686, -122.405623),
            makeLatLng(37.613819, -122.404786),
            makeLatLng(37.609642, -122.400763)
    );

    public List<LatLng> simulatePoints_2 = Arrays.asList(
            makeLatLng(37.475430,-122.221943),
            makeLatLng(37.477158,-122.221364),
            makeLatLng(37.476213,-122.220817),
            makeLatLng(37.476221,-122.221471),
            makeLatLng(37.477073,-122.222684),
            makeLatLng(37.478614,-122.224722),
            makeLatLng(37.480768,-122.227876),
            makeLatLng(37.482258,-122.229786),
            makeLatLng(37.483999,-122.231900),
            makeLatLng(37.485689, -122.234341),
            makeLatLng(37.484701, -122.233096),
            makeLatLng(37.485374, -122.234019),
            makeLatLng(37.486787, -122.235553),
            makeLatLng(37.487179, -122.236015),
            makeLatLng(37.487179, -122.236015),
            makeLatLng(37.487179, -122.236015),
            makeLatLng(37.487179, -122.236015)
    );

    public int simulateStep = 0;
    Marker simulatedDriver;
    Marker simulatedDriver_2;

    private void logout() {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage(getString(R.string.progress_logout));
        dialog.show();

        ParseUser.logOutInBackground(new LogOutCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    dialog.dismiss();
                    startWelcomeActivity();
                }
            }
        });
    }

    private void startWelcomeActivity() {
        // Start and intent for the dispatch activity
        Intent intent = new Intent(MainActivity.this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    //Marker argument had to be put in an array to force pass by reference as,
    //operations related to the markers should be performed on the original markers
    private class updateMarkers extends AsyncTask<Tuple<Hotspot,Marker[]>, String, Tuple<String,Marker[]>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Tuple<String,Marker[]> doInBackground(Tuple<Hotspot,Marker[]>... params) {
            // Loop through every 30 seconds and try to find a rider
            String origins = "";
            Hotspot h = params[0].x;
            HashMap<String, Object> cloudParams = new HashMap<String, Object>();
            cloudParams.put("hotspotObj", h.getObjectId());
            try {
                List<ParseGeoPoint> result = ParseCloud.callFunction("getActiveDrivers", cloudParams);

                for (ParseGeoPoint p : result) {
                    origins += p.getLatitude() + "," + p.getLongitude() + "|";
                }
                origins = origins.length()>0 ? (origins.substring(0, origins.length() - 1)) : "";
                return new Tuple<String,Marker[]>(origins,(params[0].y));

            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(Tuple<String,Marker[]> result){
            super.onPostExecute(result);
            if (result == null)
                return;

            Marker m = (result.y)[0];
            String origins = result.x;
            if (origins == "") {
                (result.y)[0].setTitle("Next Pickup: N/A");
                (result.y)[0].setSnippet("no drivers yet");
                return;
            }
            int minTime = 24*60*60;  //pickupTime in seconds

            //pack the query for distance matrix
            String destinations = Double.toString((result.y)[0].getPosition().latitude)+","+Double.toString((result.y)[0].getPosition().longitude);
            String mode = getString(R.string.driving_mode);
            String language = getString(R.string.us_english);
            String key = getString(R.string.google_maps_key);
            String urlString = getString(R.string.distmatrixURL);

            GenericUrl url = new GenericUrl(urlString);
            url.put("origins", origins);
            url.put("destinations", destinations);
            url.put("mode", mode);
            url.put("language", language);
            url.put("key", key);
            try {
                Result r = new RetrieveDistanceMatrix().execute(url).get();
                List<Row> rows = r.rows;
                for (Row row : rows) {
                    List<Element> elements = row.elements;
                    for (Element e : elements) {
                        minTime = Math.min(minTime,e.duration.value);
                    }
                }

            }
            catch (Exception e){
                e.printStackTrace();
                return;
            }
            //if minTime has not been reduced, possibly no drivers on the hotspot
            if (minTime < 1440){
                (result.y)[0].setTitle("Next Pickup: " + (minTime / 60) + " min");
                (result.y)[0].setSnippet("Text @ 650-290-2120");
            }
            else {
                (result.y)[0].setTitle("Next Pickup: N/A");
                (result.y)[0].setSnippet("no drivers yet");
            }
            return;
        }
    }
}

