package com.gogreen.greenmachine.parseobjects;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by jonathanlui on 4/26/15.
 */
@ParseClassName("MatchRoute")
public class MatchRoute extends ParseObject {

    public ParseUser getDriver() {
        return getParseUser("driver");
    }

    public void setDriver(ParseUser value) {
        put("driver", value);
    }

    public ArrayList<PublicProfile> getRiders() {
        return (ArrayList<PublicProfile>) get("riders");
    }

    public void setRiders(ArrayList<PublicProfile> value) {
        put("riders", value);
    }

    public void addRider(PublicProfile rider) {
        ArrayList<PublicProfile> riders;
        try {
            riders = getRiders();
            if (riders == null) {
                riders = new ArrayList<PublicProfile>();
            }
        } catch (Exception e) {
            riders = new ArrayList<PublicProfile>();
        }
        riders.add(rider);
        put("riders", riders);
    }

    public ArrayList<Hotspot> getPotentialHotspots() {
        return (ArrayList<Hotspot>) get("potentialHotspots");
    }

    public void setPotentialHotspots(ArrayList<Hotspot> value) {
        put("potentialHotspots", value);
    }

    public Hotspot getHotspot() {
        return (Hotspot) get("hotspot");
    }

    public void setHotspot(Hotspot value) {
        put("hotspot", value);
    }

    public Destination getDestination() {
        return Destination.parse(getString("destination"));
    }

    public void setDestination(Destination value) {
        put("destination", value.toString());
    }

    public void setStatus(TripStatus value) {
        put("tripStatus", value.toString());
    }

    public TripStatus getStatus() {
        return TripStatus.parse(getString("tripStatus"));
    }

    public Date getMatchBy() {
        return getDate("matchBy");
    }

    public void setMatchBy(Date value) {
        put("matchBy", value);
    }

    public Date getArriveBy() {
        return getDate("arriveBy");
    }

    public void setArriveBy(Date value) {
        put("arriveBy", value);
    }

    public void setCar(String value) {
        put("car", value);
    }

    public String getCar() {
        return getString("car");
    }

    public int getCapacity() {
        return getInt("capacity");
    }

    public void setCapacity(int value) {
        put("capacity", value);
    }

    public static ParseQuery<MatchRoute> getQuery() {
        return ParseQuery.getQuery(MatchRoute.class);
    }

    public void initializeMatchRoute(ParseUser driver, ArrayList<Hotspot> hotspots, Destination destination,
                                     TripStatus status, int capacity, Date matchBy, Date arriveBy, String car,
                                     ArrayList<PublicProfile> riders) {
        setDriver(driver);
        setPotentialHotspots(hotspots);
        setDestination(destination);
        setStatus(status);
        setCapacity(capacity);
        setMatchBy(matchBy);
        setArriveBy(arriveBy);
        setCar(car);
        setRiders(riders);
    }

    public void updateMatchRoute(Hotspot hotspot, PublicProfile rider, TripStatus status, int capacity) {
        setHotspot(hotspot);
        addRider(rider);
        setStatus(status);
        setCapacity(capacity);
    }

    public enum Destination {
        HQ,
        HOME;

        private static Destination parse(String s) {
            if (s.equals("HQ")) {
                return HQ;
            } else {
                return HOME;
            }
        }
    }

    public enum TripStatus {
        NOT_STARTED,
        EN_ROUTE_HOTSPOT,
        EN_ROUTE_DESTINATION,
        COMPLETED,
        CANCELED;

        private static TripStatus parse(String s) {
            if (s.equals("NOT_STARTED")) {
                return NOT_STARTED;
            } else if (s.equals("EN_ROUTE_HOTSPOT")) {
                return EN_ROUTE_HOTSPOT;
            } else if (s.equals("EN_ROUTE_DESTINATION")) {
                return EN_ROUTE_DESTINATION;
            } else {
                return COMPLETED;
            }
        }
    }
}
