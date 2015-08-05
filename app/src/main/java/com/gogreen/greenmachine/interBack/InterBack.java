package com.gogreen.greenmachine.interBack;

import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Utils;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by arbkhan on 8/5/2015.
 */
public class InterBack {
    public void setUserLastKnownLocation(double lat, double lon) {
        // Fetch user's public profile
        ParseUser currentUser = ParseUser.getCurrentUser();
        PublicProfile pubProfile = (PublicProfile) currentUser.get("publicProfile");
        Utils.getInstance().fetchParseObject(pubProfile);

        // Insert coordinates into the user's public profile lastKnownLocation
        ParseGeoPoint userLoc = new ParseGeoPoint(lat, lon);
        pubProfile.setLastKnownLocation(userLoc);
    }

    public Set<Hotspot> getAllHotspots() {
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
                fetchIfNeeded(pubProfile);

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

    // get the car information of the current driver
    public String getDriverCarInfo() {
        ParseUser currUser = ParseUser.getCurrentUser();
        PrivateProfile privProfile = (PrivateProfile) currUser.get("privateProfile");
        try {
            privProfile.fetchIfNeeded();
        } catch(ParseException e) {
            // Handle fetch fail
        }
        return privProfile.getDriverCar();
    }

    public void fetchIfNeeded(ParseObject p) {
        Utils.getInstance().fetchParseObject(p);
    }

}
