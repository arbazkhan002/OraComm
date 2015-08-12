package com.gogreen.greenmachine.main.match;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.gc.materialdesign.views.ButtonRectangle;
import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.interBack.InterBack;

import java.util.Calendar;


public class DrivingActivity extends ActionBarActivity {
    private Spinner mCarSpinner;
    private Spinner mStartSpinner;
    private Spinner mDestSpinner;

    private Toolbar toolbar;

    // UI references.
    private EditText driverCarEditText;
    private EditText matchByEditText;
    private EditText arriveByEditText;

    InterBack backend = new InterBack();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up EditText for Car Type
        this.driverCarEditText = (EditText) findViewById(R.id.driver_car_info);
        setDefaultCar();

        // Set up spinners
        mCarSpinner = (Spinner) findViewById(R.id.car_spinner);
        ArrayAdapter<CharSequence> carAdapter = ArrayAdapter.createFromResource(this,
                R.array.car_seats_array, android.R.layout.simple_spinner_item);
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCarSpinner.setAdapter(carAdapter);

        mStartSpinner = (Spinner) findViewById(R.id.start_spinner);
        ArrayAdapter<CharSequence> startAdapter = ArrayAdapter.createFromResource(this,
                R.array.destinations_array, R.layout.spinner);
        startAdapter.setDropDownViewResource(R.layout.spinner);
        mStartSpinner.setAdapter(startAdapter);

        mDestSpinner = (Spinner) findViewById(R.id.destination_spinner);
        ArrayAdapter<CharSequence> destAdapter = ArrayAdapter.createFromResource(this,
                R.array.destinations_array, R.layout.spinner);
        destAdapter.setDropDownViewResource(R.layout.spinner);
        mDestSpinner.setAdapter(destAdapter);

        mStartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mStartSpinner.getSelectedItemPosition()==0){
                    mDestSpinner.setSelection(1);
                }else{
                    mDestSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        mDestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mDestSpinner.getSelectedItemPosition()==0){
                    mStartSpinner.setSelection(1);
                }else{
                    mStartSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        matchByEditText = (EditText) findViewById(R.id.match_by_edit_text);
        arriveByEditText = (EditText) findViewById(R.id.arrive_by_edit_text);

        matchByEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(DrivingActivity.this, new TimePickerDialog.OnTimeSetListener() {
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
                        matchByEditText.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

        arriveByEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(DrivingActivity.this, new TimePickerDialog.OnTimeSetListener() {
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
                        arriveByEditText.setText(time);
                    }
                }, hour, minute, false);//use AM/PM time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }

        });

        // Set up the handler for the match button click
        ButtonRectangle nextButton = (ButtonRectangle) findViewById(R.id.driver_match_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startNextActivity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_driving, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }


    private void setDefaultCar() {
        this.driverCarEditText.setText(backend.getDriverCarInfo());
    }

    private void startNextActivity() {
        Intent intent = new Intent(DrivingActivity.this, DrivingHotspotSelectActivity.class);
        intent.putExtra("capacity", Integer.parseInt(mCarSpinner.getSelectedItem().toString()));
        intent.putExtra("matchDate", matchByEditText.getText().toString());
        intent.putExtra("arriveDate", arriveByEditText.getText().toString());
        intent.putExtra("destination", mDestSpinner.getSelectedItem().toString());
        intent.putExtra("driverCar", driverCarEditText.getText().toString());
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideSoftKeyboard(DrivingActivity.this);
        return false;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }    }
}
