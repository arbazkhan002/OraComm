package com.gogreen.greenmachine.main.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.gogreen.greenmachine.main.MainActivity;
import com.gogreen.greenmachine.main.WelcomeActivity;
import com.gogreen.greenmachine.main.profile.ProfileInitDispatchActivity;
import com.parse.ParseUser;

/**
 * Created by jonathanlui on 4/19/15.
 *
 * Activity which starts an intent for either the logged in (MainActivity) or logged out
 * (SignUpOrLoginActivity) activity.
 */
public class DispatchActivity extends Activity {

    public DispatchActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if there is current user info
        if (ParseUser.getCurrentUser() != null) {
            try {
                if ((boolean) ParseUser.getCurrentUser().get("profileComplete")) {
                    // Start an intent for the logged in activity
                    startActivity(new Intent(this, MainActivity.class));
                } else {
                    Intent intent = new Intent(this, ProfileInitDispatchActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } catch (Exception e) {
                startActivity(new Intent(this, WelcomeActivity.class));
            }
        } else {
            // Start and intent for the logged out activity
            startActivity(new Intent(this, WelcomeActivity.class));
        }
    }
}
