package com.gogreen.greenmachine.main.navigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.gogreen.greenmachine.util.Utils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Calendar;
import java.util.HashMap;


public class SettingsActivity extends ActionBarActivity {

    // UI Components
    private EditText mFirstName;
    private EditText mLastName;
    private EditText mHomeCity;
    private EditText mPhone;
    private EditText mArriveHQBy;
    private EditText mArriveHomeBy;

    private static PrivateProfile userProfile;
    private boolean isDriver;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        userProfile = (PrivateProfile) ParseUser.getCurrentUser().get("privateProfile");
        Utils.getInstance().fetchParseObject(userProfile);

        // Set up proper links to UI Components
        mFirstName = (EditText) findViewById(R.id.first_name_edit_text);
        mLastName = (EditText) findViewById(R.id.last_name_edit_text);
        mHomeCity = (EditText) findViewById(R.id.home_city_edit_text);
        mPhone = (EditText) findViewById(R.id.phone_edit_text);
        mArriveHQBy = (EditText) findViewById(R.id.arrive_hq_by_edit_text);
        mArriveHomeBy = (EditText) findViewById(R.id.arrive_home_by_edit_text);

        // Set user data into fields
        mFirstName.setText(userProfile.getFirstName());
        mLastName.setText(userProfile.getLastName());
        mHomeCity.setText(userProfile.getHomeCity());
        mPhone.setText(userProfile.getPhoneNumber());
        mArriveHQBy.setText(userProfile.getArriveHQBy());
        mArriveHomeBy.setText(userProfile.getArriveHomeBy());

        mArriveHQBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String suffix = " AM";
                        if (selectedHour == 00){
                            selectedHour = 12;
                        }
                        else if (selectedHour > 12){
                            selectedHour = selectedHour - 12;
                            suffix = " PM";
                        }
                        String time = selectedHour + ":";
                        if (selectedMinute < 10){
                            time = time + "0" + selectedMinute;
                        }
                        else {
                            time = time + selectedMinute;
                        }
                        time = time + suffix;
                        mArriveHQBy.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

        mArriveHomeBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String suffix = " AM";
                        if (selectedHour == 00){
                            selectedHour = 12;
                        }
                        else if (selectedHour > 12){
                            selectedHour = selectedHour - 12;
                            suffix = " PM";
                        }
                        String time = selectedHour + ":";
                        if (selectedMinute < 10){
                            time = time + "0" + selectedMinute;
                        }
                        else {
                            time = time + selectedMinute;
                        }
                        time = time + suffix;
                        mArriveHomeBy.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

        Button submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateProfile(userProfile);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProfile(PrivateProfile p) {
        // Set up a progress dialog
        final ProgressDialog dialog = new ProgressDialog(SettingsActivity.this);
        dialog.setMessage(getString(R.string.progress_update_profile));
        dialog.show();

        // Prepare user inputs for validator
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put("firstName", mFirstName.getText().toString().trim());
        fields.put("lastName", mLastName.getText().toString().trim());
        fields.put("homeCity", mHomeCity.getText().toString().trim());
        fields.put("phone", mPhone.getText().toString().trim());
        fields.put("arriveHQBy", mArriveHQBy.getText().toString().trim());
        fields.put("arriveHomeBy", mArriveHomeBy.getText().toString().trim());

        validateFields(fields);

        userProfile.setFirstName((String) fields.get("firstName"));
        userProfile.setLastName((String) fields.get("lastName"));
        userProfile.setHomeCity((String) fields.get("homeCity"));
        userProfile.setPhoneNumber((String) fields.get("phone"));
        userProfile.setDriverStatus((Boolean) fields.get("driving"));
        userProfile.setDriverCar((String) fields.get("car"));
        userProfile.setArriveHQBy((String) fields.get("arriveHQBy"));
        userProfile.setArriveHomeBy((String) fields.get("arriveHomeBy"));

        userProfile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                dialog.dismiss();
                if (e != null) {
                    // Show the error message
                    Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    // show user the activity was complete
                    Toast.makeText(SettingsActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Validate user inputs
    private void validateFields(HashMap<String, Object> fields) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideSoftKeyboard(SettingsActivity.this);
        return false;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}
