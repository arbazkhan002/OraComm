package com.gogreen.greenmachine.interBack.objects;

import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

/**
 * Created by arbkhan on 8/12/2015.
 */
public class InterUser {
    ParseUser u;
    private PrivateProfile privProfile = null;
    private PublicProfile publicProfile = null;
    LatLng lastKnownLocation;
    private String firstName;
    private String lastName;
    private String email;

    public LatLng getLastKnownLocation() {
        ParseGeoPoint myLoc = getPublicProfile().getLastKnownLocation();
        return new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
    }

    public void setLastKnownLocation(LatLng lastKnownLocation) {
        getPublicProfile().setLastKnownLocation(new ParseGeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude));
    }

    public PublicProfile getPublicProfile() {
        PublicProfile myProfile = (PublicProfile) (this.u).get("publicProfile");
        Utils.getInstance().fetchParseObject(myProfile);
        return myProfile;
    }

    public void setPublicProfile(PublicProfile publicProfile) {
        this.publicProfile = publicProfile;
    }

    public String getEmail() {
        if (u == null)
            return null;
        email = u.getEmail();
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PrivateProfile getPrivProfile() {
        privProfile = (PrivateProfile) u.get("privateProfile");
        try {
            this.privProfile.fetchIfNeeded();
            return privProfile;
        }
        catch (ParseException e) {
            return null;
        }
    }

    public void setPrivProfile(PrivateProfile privProfile) {
        this.privProfile = privProfile;
    }

    public String getFirstName() {
        try {
            this.privProfile.fetchIfNeeded();
            firstName = this.privProfile.getFirstName();
            return firstName;
        }
        catch (ParseException e) {
            return null;
        }
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        try {
            this.privProfile.fetchIfNeeded();
            lastName = privProfile.getLastName();
            return lastName;
        }
        catch (ParseException e) {
            return null;
        }

    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUser(ParseUser user) {
        this.u = user;
    }

    public ParseUser getUser() {
        return this.u;
    }
}
