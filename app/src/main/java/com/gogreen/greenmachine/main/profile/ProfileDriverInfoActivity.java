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
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.main.login.DispatchActivity;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.parse.ParseUser;


public class ProfileDriverInfoActivity extends ActionBarActivity {

    // UI references.
    private  Switch drivingSwitch;
    private EditText carEditText;
    private LinearLayout carReveal;
    private Spinner mCarSpinner;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_driver_info);

        // Set up Edit Texts and Switch
        carReveal = (LinearLayout) findViewById(R.id.isdriving);
        drivingSwitch = (Switch) findViewById(R.id.driving_switch);
        drivingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (drivingSwitch.isChecked()){
                    carReveal.setVisibility(View.VISIBLE);}
                else{
                    carReveal.setVisibility(View.INVISIBLE);
                }
            }


        });

        carEditText = (EditText) findViewById(R.id.car_edit_text);
        mCarSpinner = (Spinner) findViewById(R.id.car_spinner);
        ArrayAdapter<CharSequence> carAdapter = ArrayAdapter.createFromResource(this,
                R.array.car_seats_array, android.R.layout.simple_spinner_item);
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCarSpinner.setAdapter(carAdapter);

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
        getMenuInflater().inflate(R.menu.menu_profile_driver_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void submit() {
        // Set up public profile with data
        ParseUser currentUser = ParseUser.getCurrentUser();
        String driving = drivingSwitch.getText().toString().trim();
        String car = carEditText.getText().toString().trim();

        savePrivateProfile(driving, car);
        setUserProfileComplete(currentUser);

        Intent intent = new Intent(ProfileDriverInfoActivity.this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setUserProfileComplete(ParseUser user) {
        user.put("profileComplete", true);
        user.saveInBackground();
    }

    private void savePrivateProfile(String driving, String car) {
        PrivateProfile privProfile = (PrivateProfile) ParseUser.getCurrentUser().get("privateProfile");
        boolean driverStatus = false;
        if (driving.toLowerCase().equals("yes")) {
            driverStatus = true;
        }
        privProfile.setDriverStatus(driverStatus);
        privProfile.setDriverCar(car);
        privProfile.saveInBackground();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideSoftKeyboard(ProfileDriverInfoActivity.this);
        return false;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }    }
}
