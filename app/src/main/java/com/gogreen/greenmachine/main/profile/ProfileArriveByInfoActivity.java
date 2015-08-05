package com.gogreen.greenmachine.main.profile;

import android.app.Activity;
import android.app.TimePickerDialog;
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
import android.widget.TimePicker;

import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.parseobjects.PrivateProfile;
import com.parse.ParseUser;

import java.util.Calendar;

public class ProfileArriveByInfoActivity extends ActionBarActivity {

    // UI references.
    private EditText arriveHQByEditText;
    private EditText arriveHomeByEditText;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_arrive_by_info);

        arriveHQByEditText = (EditText) findViewById(R.id.arrive_hq_by_edit_text);
        arriveHomeByEditText = (EditText) findViewById(R.id.arrive_home_by_edit_text);

        arriveHQByEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(ProfileArriveByInfoActivity.this, new TimePickerDialog.OnTimeSetListener() {
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
                        arriveHQByEditText.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

        arriveHomeByEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(ProfileArriveByInfoActivity.this, new TimePickerDialog.OnTimeSetListener() {
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
                        arriveHomeByEditText.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

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
        getMenuInflater().inflate(R.menu.menu_profile_arrive_by_info, menu);
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
        String arriveHQBy = arriveHQByEditText.getText().toString().trim();
        String arriveHomeBy = arriveHomeByEditText.getText().toString().trim();

        savePrivateProfile(arriveHQBy, arriveHomeBy);

        Intent intent = new Intent(ProfileArriveByInfoActivity.this, ProfileHotspotInfoActivity.class);
        startActivity(intent);
    }

    private void savePrivateProfile(String arriveHQBy, String arriveHomeBy) {
        PrivateProfile privProfile = (PrivateProfile) ParseUser.getCurrentUser().get("privateProfile");
        privProfile.setArriveHQBy(arriveHQBy);
        privProfile.setArriveHomeBy(arriveHomeBy);
        privProfile.saveInBackground();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideSoftKeyboard(ProfileArriveByInfoActivity.this);
        return false;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }    }
}
