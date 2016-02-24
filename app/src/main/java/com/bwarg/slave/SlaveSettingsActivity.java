package com.bwarg.slave;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LM on 15.02.2016.
 */
public class SlaveSettingsActivity extends ActionBarActivity {
    private static final String TAG = "SETTINGS";

    private StreamPreferences streamPrefs = new StreamPreferences();
    private StreamPreferences receivedStreamPrefs = new StreamPreferences();

    RadioGroup camera_group;
    SwitchCompat flash_switch;
    Spinner resolution_spinner;
    SeekBar quality_seekbar;
    TextView quality_text;
    EditText port_input;
    EditText name_input;

    RadioGroup port_group;
    Button settings_done;


    private static final int REQUEST_SETTINGS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        Bundle extras = getIntent().getExtras();
        Gson gson = new Gson();
        streamPrefs = gson.fromJson(extras.getString("stream_prefs"), StreamPreferences.class);
        receivedStreamPrefs = streamPrefs;

        camera_group = (RadioGroup) findViewById(R.id.camera_radiogroup);
        flash_switch = (SwitchCompat) findViewById(R.id.flash_switch);

        resolution_spinner = (Spinner) findViewById(R.id.resolution_spinner);

        quality_seekbar = (SeekBar) findViewById(R.id.quality_seekbar);
        quality_text = (TextView) findViewById(R.id.quality_text);

        name_input = (EditText) findViewById(R.id.name_input);

        port_input = (EditText) findViewById(R.id.port_input);
        port_group = (RadioGroup) findViewById(R.id.port_radiogroup);

        fillUI(streamPrefs);

        camera_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.camera_0) {
                    streamPrefs.setCamIndex(0);
                    initSpinner(streamPrefs);
                }else if(checkedId == R.id.camera_1){
                    streamPrefs.setCamIndex(1);
                    initSpinner(streamPrefs);
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
        resolution_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                streamPrefs.setCameraPreviewIndex(spinner.getSelectedItemPosition());
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        quality_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                quality_text.setText(String.valueOf(progress+1)+"%");
                streamPrefs.setQuality(progress+1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        port_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.camera_0) {
                    streamPrefs.setCamIndex(0);
                }else if(checkedId==R.id.camera_1){
                    streamPrefs.setCamIndex(1);
                }
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
                            streamPrefs.setIp_port(Integer.parseInt(s));
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
        initSpinner(prefs);
        name_input.setText(prefs.getName());
        quality_seekbar.setProgress(prefs.getQuality()-1);
        quality_text.setText(String.valueOf(prefs.getQuality()-1)+"%");
        port_input.setText(String.valueOf(prefs.getIp_port()), TextView.BufferType.NORMAL);

    }

    private void initSpinner(StreamPreferences prefs){
        final Camera camera = Camera.open(prefs.getCamIndex());
        final Camera.Parameters params = camera.getParameters();
        camera.release();

        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        ArrayList<String> stringSupportedPreviewSizeList = new ArrayList<>();
        for(Camera.Size s : supportedPreviewSizes) {
            stringSupportedPreviewSizeList.add(s.width + "x" + s.height);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, stringSupportedPreviewSizeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner.setAdapter(adapter);
        resolution_spinner.setSelection(prefs.getCameraPreviewIndex());
    }
}

