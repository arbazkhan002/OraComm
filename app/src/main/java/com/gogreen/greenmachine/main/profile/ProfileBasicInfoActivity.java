package com.gogreen.greenmachine.main.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.gogreen.greenmachine.util.Utils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class ProfileBasicInfoActivity extends ActionBarActivity {

    // UI references.
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText homeCityEditText;
    private EditText phoneEditText;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_basic_info);

        // Set up Edit Texts
        firstNameEditText = (EditText) findViewById(R.id.first_name_edit_text);
        lastNameEditText = (EditText) findViewById(R.id.last_name_edit_text);
        homeCityEditText = (EditText) findViewById(R.id.home_city_edit_text);
        phoneEditText = (EditText) findViewById(R.id.phone_edit_text);

        prePopulateFields();

        // Set up the handler for the next button click
        ImageButton nextButton = (ImageButton) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submit();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile_basic_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void prePopulateFields() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        PrivateProfile privProfile = (PrivateProfile) currentUser.get("privateProfile");
        Utils.getInstance().fetchParseObject(privProfile);

        this.firstNameEditText.setText(privProfile.getFirstName());
        this.lastNameEditText.setText(privProfile.getLastName());
        this.homeCityEditText.setText(privProfile.getHomeCity());
        this.phoneEditText.setText(privProfile.getPhoneNumber());
    }

    private void submit() {
        // Set up private profile with data
        final ParseUser currentUser = ParseUser.getCurrentUser();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String homeCity = homeCityEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        savePrivateProfile(firstName, lastName, homeCity, phone);
        savePublicProfile(firstName, lastName, phone);
    }

    private void savePrivateProfile(String firstName, String lastName, String homeCity, String phone) {
        PrivateProfile privProfile = (PrivateProfile) ParseUser.getCurrentUser().get("privateProfile");
        privProfile.setFirstName(firstName);
        privProfile.setLastName(lastName);
        privProfile.setHomeCity(homeCity);
        privProfile.setPhoneNumber(phone);
        privProfile.saveInBackground();
    }

    private void savePublicProfile(String firstName, String lastName, String phone) {
        PublicProfile pubProfile = (PublicProfile) ParseUser.getCurrentUser().get("publicProfile");
        pubProfile.setFirstName(firstName);
        pubProfile.setLastName(lastName);
        pubProfile.setPhoneNumber(phone);
        pubProfile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                startNextActivity();
            }
        });
    }

    private void startNextActivity() {
        Intent intent = new Intent(ProfileBasicInfoActivity.this, ProfileDriverInfoActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideSoftKeyboard(ProfileBasicInfoActivity.this);
        return false;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
