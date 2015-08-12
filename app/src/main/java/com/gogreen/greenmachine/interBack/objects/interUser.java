package com.gogreen.greenmachine.interBack.objects;

import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * Created by arbkhan on 8/12/2015.
 */
public class interUser {
    ParseUser u;
    private PrivateProfile privProfile = null;
    private String firstName;
    private String lastName;
    private String email;

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
        if (privProfile == null) {
            getPrivProfile();
        }
        firstName = this.privProfile.getFirstName();
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        if (privProfile == null) {
            getPrivProfile();
        }
        lastName = privProfile.getLastName();
        return lastName;
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
