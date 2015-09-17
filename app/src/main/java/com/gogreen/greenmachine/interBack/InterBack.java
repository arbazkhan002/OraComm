package com.gogreen.greenmachine.interBack;

import android.util.Log;

import com.gogreen.greenmachine.interBack.objects.InterUser;
import com.gogreen.greenmachine.main.match.DriverMatchedActivity;
import com.gogreen.greenmachine.main.match.RiderMatchedActivity;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRequest;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Ifunction;
import com.gogreen.greenmachine.util.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.parse.LogOutCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    public ArrayList<PublicProfile> getActiveDrivers(Hotspot h) {
        ArrayList<MatchRoute> matchRoute;
        ArrayList<PublicProfile> driversPubProf = new ArrayList<PublicProfile>();
        ParseQuery<MatchRoute> query = ParseQuery.getQuery("MatchRoute");

        try {
            matchRoute = new ArrayList<MatchRoute>(query.find());
            for (MatchRoute r : matchRoute) {
                ParseUser driver = r.getDriver();
                try {
                    driver.fetchIfNeeded();
                } catch (ParseException e) {

                }
                PublicProfile pubProfile = (PublicProfile) driver.get("publicProfile");
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

    public String getDriverLocations(Hotspot h) {
        String origins = "";
        try {
            HashMap<String, Object> cloudParams = new HashMap<String, Object>();
            cloudParams.put("hotspotObj", h.getObjectId());
            List<ParseGeoPoint> result = ParseCloud.callFunction("getActiveDrivers", cloudParams);

            for (ParseGeoPoint p : result) {
                origins += p.getLatitude() + "," + p.getLongitude() + "|";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return origins;
    }

    // get the car information of the current driver
    public String getDriverCarInfo() {
        ParseUser currUser = ParseUser.getCurrentUser();
        PrivateProfile privProfile = (PrivateProfile) currUser.get("privateProfile");
        try {
            privProfile.fetchIfNeeded();
        } catch (ParseException e) {
            // Handle fetch fail
        }
        return privProfile.getDriverCar();
    }

    public void fetchIfNeeded(ParseObject p) {
        Utils.getInstance().fetchParseObject(p);
    }

    public void logOutInBackground(final Ifunction f) {
        ParseUser.logOutInBackground(new LogOutCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    f.execute();
                }
            }
        });
    }

    public ParseUser getCurrentUser() {
        return ParseUser.getCurrentUser();
    }

    public boolean sendRiderRequest(MatchRoute[] matchRoute,
                                    ArrayList<Hotspot> s,
                                    int currentCapacity,
                                    String driverCar,
                                    Date matchByDate,
                                    Date arriveByDate,
                                    MatchRoute.Destination destination) {
        MatchRoute m = matchRoute[0];
        m.initializeMatchRoute(ParseUser.getCurrentUser(), s, destination,
                MatchRoute.TripStatus.NOT_STARTED, currentCapacity, matchByDate,
                arriveByDate, driverCar, new ArrayList<PublicProfile>());
        try {
            m.save();
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public boolean sendDriverRequest(MatchRequest[] riderRequest,
                                     Set<Hotspot> s,
                                     Date matchByDate,
                                     Date arriveByDate,
                                     MatchRequest.MatchStatus status) {
        riderRequest[0] = new MatchRequest();
        riderRequest[0].populateMatchRequest(ParseUser.getCurrentUser(), s,
                matchByDate, arriveByDate, status);
        try {
            riderRequest[0].save();
            return true;
        } catch (ParseException e) {
            return false;
        }

    }

    public ArrayList<PublicProfile> getRiders(MatchRoute matchRoute) {
       try {
           matchRoute.fetchIfNeeded();
           ArrayList<PublicProfile> riders = (ArrayList<PublicProfile>) matchRoute.get("riders");
           Log.i(InterBack.class.getSimpleName(), "routeId: "+matchRoute.getObjectId());
           Log.i(InterBack.class.getSimpleName(), "ridersList: "+Integer.toString(riders.size()));
           return riders;
       }
       catch (ParseException e) {
           e.printStackTrace();
           return null;
       }
    }

    public boolean checkForRiders(MatchRoute[] marray) {
        HashMap<String, Object> cloudParams = new HashMap<String, Object>();
        Boolean riderFound = false;
        Log.i(InterBack.class.getSimpleName(), "checkForRiders: ");

        for (int i=0; i<10000; i++) {
            try {
                MatchRoute matchRoute = marray[0];
                matchRoute.fetchIfNeeded();
                cloudParams.put("matchRouteId", matchRoute.getObjectId());
                riderFound = ParseCloud.callFunction("checkForRiders", cloudParams);
                Log.i(InterBack.class.getSimpleName(), "RouteId: "+matchRoute.getObjectId());
                Log.i(InterBack.class.getSimpleName(), "checkForRiders: "+riderFound.toString());
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }

            if (riderFound) {
                return true;
            }

            try {
                Thread.sleep(5000);
            }
            catch (java.lang.InterruptedException e){
                e.printStackTrace();
            }
        }

        return false;
    }

    // Grab all routes from the server
    public List<MatchRoute> fetchAllRoutes() {
        ParseQuery<MatchRoute> notStartedQuery = ParseQuery.getQuery("MatchRoute");
        notStartedQuery = notStartedQuery.whereEqualTo("tripStatus", MatchRoute.TripStatus.NOT_STARTED.toString());

        ParseQuery<MatchRoute> enRouteQuery = ParseQuery.getQuery("MatchRoute");
        enRouteQuery = enRouteQuery.whereEqualTo("tripStatus", MatchRoute.TripStatus.EN_ROUTE_HOTSPOT.toString());

        //ParseQuery.or
        ParseQuery<MatchRoute> finalQuery = ParseQuery.or(Arrays.asList(notStartedQuery, enRouteQuery));

        try {
            return new ArrayList<>(finalQuery.find());
        } catch (ParseException e) {
            // Handle server retrieval failure
            return null;
        }
    }

    // check if user is a rider in the route
    public boolean isRiderInRoute(InterUser user, MatchRoute route) {
        ArrayList<PublicProfile> riders = (ArrayList<PublicProfile>) route.getRiders();
        PublicProfile myProfile = user.getPublicProfile();
        Iterator profIterator = riders.iterator();
        while(profIterator.hasNext()) {
            PublicProfile riderProfile = (PublicProfile) profIterator.next();
            if (riderProfile.equals(myProfile)) {
                return true;
            }
        }
        return false;
    }

    public LatLng toLatLng(ParseGeoPoint p) {
        return new LatLng(p.getLatitude(), p.getLongitude());
    }

    //get MatchRoute of the currentUser
    public HashMap<String, Object> getRequestObject() {
        HashMap<String, Object> result = new HashMap<String, Object>();
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
            fetchIfNeeded(route);

            if (route.getDriver().getObjectId().equals(currentUser.getObjectId()) && route.getStatus() == MatchRoute.TripStatus.NOT_STARTED) {
                result.put("capacity", route.getCapacity());
                String matchBy = route.getMatchBy().toString();
                String arriveBy = route.getArriveBy().toString();
                String destination = route.getDestination().toString();
                result.put("matchDate", matchBy );
                result.put("arriveDate", arriveBy);
                result.put("destination", destination);
                result.put("driverCar", route.getCar());
                Set<Hotspot> selectedHotspots = new HashSet<Hotspot>();

                for (Hotspot h: route.getPotentialHotspots())
                    selectedHotspots.add(h);
                result.put("hotspots", selectedHotspots);

                Log.i(InterBack.class.getSimpleName(), "matchDate " + matchBy);
                foundRoute = true;
            }
        }

        if (foundRoute == true)
           return result;
        else
            return null;
    }

    // Is the current user a driver requesting for riders
    public Boolean isDriverRequesting(HashMap<String, Object>[] r) {
        HashMap<String, Object> result = r[0];
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
            fetchIfNeeded(route);

            if (route.getDriver().getObjectId().equals(currentUser.getObjectId()) && route.getStatus() == MatchRoute.TripStatus.NOT_STARTED) {
                result.put("capacity", route.getCapacity());
                String matchBy = route.getMatchBy().toString();
                String arriveBy = route.getArriveBy().toString();
                String destination = route.getDestination().toString();
                result.put("matchDate", matchBy );
                result.put("arriveDate", arriveBy);
                result.put("destination", destination);
                result.put("driverCar", route.getCar());
                Set<Hotspot> selectedHotspots = new HashSet<Hotspot>();
                MatchRoute savedRoute = route;
                for (Hotspot h: route.getPotentialHotspots())
                    selectedHotspots.add(h);
                result.put("hotspots", selectedHotspots);
                result.put("route", savedRoute);

                Log.i(InterBack.class.getSimpleName(), "matchDate " + matchBy);
                foundRoute = true;
            }
        }

        return foundRoute;
    }

    // Instead of passing a single instance, passing an array for a need of pass by reference
    public Boolean getDriverInfo(DriverMatchedActivity[] driverMatch) {
        DriverMatchedActivity t = driverMatch[0];
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
            fetchIfNeeded(route);

            if (route.getDriver().getObjectId().equals(currentUser.getObjectId())) {
                ArrayList<PublicProfile> riders = route.getRiders();
                Iterator ridersIter = riders.iterator();
                while (ridersIter.hasNext()) {
                    PublicProfile riderProfile = (PublicProfile) ridersIter.next();
                    fetchIfNeeded(riderProfile);

                    t.mRiderTextSetText(riderProfile.getFirstName());

                    ParseGeoPoint riderLocation = riderProfile.getLastKnownLocation();
                    t.addToRiderLocations(riderLocation);
                    t.setRiderNumber(riderProfile.getPhoneNumber());
                }

                Hotspot hotspot = route.getHotspot();
                fetchIfNeeded(hotspot);

                t.setHotspotLocation(hotspot.getParseGeoPoint());
                t.setmRoute(route);
                foundRoute = true;
            }
        }

        return foundRoute;
    }


    public Boolean getRiderInfo(RiderMatchedActivity[] riderMatch) {
        RiderMatchedActivity t = riderMatch[0];
        ParseUser currentUser = ParseUser.getCurrentUser();
        PublicProfile curUserPublicProfile = (PublicProfile) currentUser.get("publicProfile");
        fetchIfNeeded(curUserPublicProfile);
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
            fetchIfNeeded(route);

            ParseUser driver = route.getDriver();
            try {
                driver.fetchIfNeeded();
            } catch (ParseException e) {
                // handle later since low on time
            }

            ArrayList<PublicProfile> riders = route.getRiders();
            Iterator ridersIter = riders.iterator();
            while (ridersIter.hasNext()) {
                PublicProfile riderProfile = (PublicProfile) ridersIter.next();
                if (riderProfile.getObjectId().equals(curUserPublicProfile.getObjectId())) {
                    PublicProfile driverProfile = (PublicProfile) driver.get("publicProfile");
                    fetchIfNeeded(driverProfile);

                    Hotspot hotspot = route.getHotspot();
                    fetchIfNeeded(hotspot);

                    t.setHotspotLocation(hotspot.getParseGeoPoint());
                    t.setDriverLocation(driverProfile.getLastKnownLocation());
                    t.setDriverPhone(driverProfile.getPhoneNumber());
                    t.setDriverName(driverProfile.getFirstName());
                    t.setDriverCar(route.getCar());
                    t.setmRoute(route);
                    foundRoute = true;
                }
            }
        }

        return foundRoute;
    }
}
