package com.bwarg.slave;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;

/**
 * Created by LM on 15.02.2016.
 */
public class SlaveSettingsActivity extends ActionBarActivity {
    private static final String TAG = "SETTINGS";

    private StreamPreferences streamPrefs = new StreamPreferences();

    RadioGroup camera_group;
    SwitchCompat flash_switch;
    EditText port_input;
    EditText name_input;

    RadioGroup port_group;
    CheckBox lock_checkbox;

    Button settings_done;


    private static final int REQUEST_SETTINGS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        Bundle extras = getIntent().getExtras();
        Gson gson = new Gson();
        streamPrefs = gson.fromJson(extras.getString("stream_prefs"), StreamPreferences.class);

        camera_group = (RadioGroup) findViewById(R.id.camera_radiogroup);
        flash_switch = (SwitchCompat) findViewById(R.id.flash_switch);

        name_input = (EditText) findViewById(R.id.name_input);

        port_input = (EditText) findViewById(R.id.port_input);
        port_group = (RadioGroup) findViewById(R.id.port_radiogroup);

        lock_checkbox=(CheckBox) findViewById(R.id.lockCheckbox);

        fillUI(streamPrefs);

        camera_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.camera_0) {
                    streamPrefs.setCamIndex(0);
                }else if(checkedId == R.id.camera_1){
                    streamPrefs.setCamIndex(1);
                }
            }
        });
        flash_switch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.equals(flash_switch)) {
                    streamPrefs.setUseFlashLight(isChecked);
                }
            }
        });
        port_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.camera_0) {
                    streamPrefs.setCamIndex(0);
                } else if (checkedId == R.id.camera_1) {
                    streamPrefs.setCamIndex(1);
                }
            }
        });

        lock_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StreamCameraActivity.LOCK_PHYS_KEYS = isChecked;
            }
        });

        settings_done = (Button) findViewById(R.id.settings_done);
        settings_done.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {

                        String s;
                        s = name_input.getText().toString();
                        if(!"".equals(s)){
                            streamPrefs.setName(s);
                        }else{
                            streamPrefs.setName(StreamPreferences.UNKNOWN_NAME);
                        }
                        s = port_input.getText().toString();
                        if (!"".equals(s)) {
                            streamPrefs.setIpPort(Integer.parseInt(s));
                        }

                        Intent intent = new Intent();
                        Gson gson = new Gson();
                        String stringPref = gson.toJson(streamPrefs);
                        intent.putExtra("stream_prefs", stringPref);

                        Log.d(TAG, stringPref);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
        );
    }
    private void fillUI(StreamPreferences prefs){
        if(prefs.getCamIndex() == 0){
            camera_group.check(R.id.camera_0);
        }else if(prefs.getCamIndex() == 1){
            camera_group.check(R.id.camera_1);
        }
        flash_switch.setChecked(prefs.useFlashLight());
        name_input.setText(prefs.getName());
        port_input.setText(String.valueOf(prefs.getIpPort()), TextView.BufferType.NORMAL);
        lock_checkbox.setChecked(StreamCameraActivity.LOCK_PHYS_KEYS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    Gson gson = new Gson();
                    streamPrefs = gson.fromJson(data.getStringExtra("stream_prefs"), StreamPreferences.class);
                    fillUI(streamPrefs);
                }
                break;
        }
    }
    public void openImageSettings(View v){
        Intent intent = new Intent(this, ImageSettingsActivity.class);
        Gson gson = new Gson();
        String gsonString =  gson.toJson(streamPrefs);
        Log.d(TAG, "GSON string given to settings : "+gsonString);
        intent.putExtra("stream_prefs", gsonString);
        startActivityForResult(intent, REQUEST_SETTINGS);
    }
}

