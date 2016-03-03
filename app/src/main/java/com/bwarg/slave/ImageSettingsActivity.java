package com.bwarg.slave;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
 * Created by LM on 03.03.2016.
 */
public class ImageSettingsActivity  extends ActionBarActivity {
    private static final String TAG = "SETTINGS";

    private Spinner resolution_spinner;
    private SeekBar quality_seekbar;
    private TextView quality_text;
    private CheckBox auto_white_lock_checkBox;
    private Spinner white_balance_spinner;
    private CheckBox auto_exposure_lock_checkBoc;
    private Spinner iso_spinner;
    private Spinner focus_mode_spinner;
    private CheckBox stabilize_image_checkBox;

    private StreamPreferences streamPrefs = new StreamPreferences();

    Button settings_done;


    private static final int REQUEST_SETTINGS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_settings);

        Bundle extras = getIntent().getExtras();
        Gson gson = new Gson();
        streamPrefs = gson.fromJson(extras.getString("stream_prefs"), StreamPreferences.class);

        resolution_spinner = (Spinner) findViewById(R.id.resolution_spinner);

        quality_seekbar = (SeekBar) findViewById(R.id.quality_seekbar);
        quality_text = (TextView) findViewById(R.id.quality_text);

        auto_white_lock_checkBox = (CheckBox) findViewById(R.id.auto_white_lock);
        white_balance_spinner = (Spinner) findViewById(R.id.white_balance_spinner);
        auto_exposure_lock_checkBoc = (CheckBox) findViewById(R.id.auto_exposure_lock);
        iso_spinner = (Spinner) findViewById(R.id.iso_spinner);
        focus_mode_spinner = (Spinner) findViewById(R.id.focus_spinner);
        stabilize_image_checkBox = (CheckBox) findViewById(R.id.image_stabilization);

        fillUI(streamPrefs);

        resolution_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                if (parent.equals(spinner))
                    streamPrefs.setSizeIndex(spinner.getSelectedItemPosition());
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        quality_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                quality_text.setText(String.valueOf(progress + 1) + "%");
                streamPrefs.setQuality(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        auto_white_lock_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (auto_white_lock_checkBox.equals((CheckBox) buttonView))
                    streamPrefs.setAuto_white_balance_lock(isChecked);
            }
        });
        white_balance_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                if (white_balance_spinner.equals(spinner))
                    streamPrefs.setWhitebalance((String) spinner.getSelectedItem());
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        auto_exposure_lock_checkBoc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(auto_exposure_lock_checkBoc.equals((CheckBox)buttonView))
                    streamPrefs.setAuto_exposure_lock(isChecked);
            }
        });
        iso_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                if (iso_spinner.equals(spinner))
                    streamPrefs.setIso((String) spinner.getSelectedItem());
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        focus_mode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View viw, int arg2, long arg3) {
                Spinner spinner = (Spinner) parent;
                if (focus_mode_spinner.equals(spinner))
                    streamPrefs.setFocus_mode((String) spinner.getSelectedItem());
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        stabilize_image_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (stabilize_image_checkBox.equals((CheckBox) buttonView))
                    streamPrefs.setImage_stabilization(isChecked);
            }
        });
        settings_done = (Button) findViewById(R.id.settings_done);
        settings_done.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
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
        final Camera camera = Camera.open(prefs.getCamIndex());
        final Camera.Parameters params = camera.getParameters();
        camera.release();

        quality_seekbar.setProgress(prefs.getQuality() - 1);
        quality_text.setText(String.valueOf(prefs.getQuality()) + "%");

        auto_white_lock_checkBox.setEnabled(params.isAutoWhiteBalanceLockSupported());
        if(auto_white_lock_checkBox.isEnabled()) {
            auto_white_lock_checkBox.setChecked(prefs.isAuto_white_balance_lock());
        }
        auto_exposure_lock_checkBoc.setEnabled(params.isAutoExposureLockSupported());
        if(auto_exposure_lock_checkBoc.isEnabled()) {
            auto_exposure_lock_checkBoc.setChecked(prefs.isAuto_exposure_lock());
        }
        stabilize_image_checkBox.setEnabled(params.isVideoStabilizationSupported());
        if(stabilize_image_checkBox.isEnabled()) {
            stabilize_image_checkBox.setChecked(prefs.isImage_stabilization());
        }
        initSpinners(prefs, params);
    }
    private void initSpinners(StreamPreferences prefs, Camera.Parameters params){
        //PREVIEW SIZES
        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        ArrayList<String> stringSupportedPreviewSizeList = new ArrayList<>();
        for(Camera.Size s : supportedPreviewSizes) {
            stringSupportedPreviewSizeList.add(s.width + "x" + s.height);
        }
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, stringSupportedPreviewSizeList);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolution_spinner.setAdapter(sizeAdapter);
        resolution_spinner.setSelection(prefs.getSizeIndex());

        //WHITE BALANCES MODE
        final List<String> supportedWhiteModes = params.getSupportedWhiteBalance();
        ArrayAdapter<String> whiteAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, supportedWhiteModes);
        whiteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        white_balance_spinner.setAdapter(whiteAdapter);
        white_balance_spinner.setSelection(supportedWhiteModes.indexOf(prefs.getWhitebalance()));

        //ISO MODE
        String isoModesString = params.get("iso-values");
        if(isoModesString == null){
            //Looks like Acer did implemented it as iso-speed and not iso
            isoModesString = params.get("iso-speed-values");
        }
        if(isoModesString == null){
            iso_spinner.setEnabled(false);
        }else{
            iso_spinner.setEnabled(true);
            final List<String> supportedISOModes = new ArrayList<>();
            String temp = "";
            for(int i=0; i< isoModesString.length(); i++){
                char c = isoModesString.charAt(i);
                if(c != ','){
                    temp+=c;
                }else{
                    supportedISOModes.add(temp);
                    temp="";
                }
            }
            if(!temp.equals("")){
                supportedISOModes.add(temp);
            }
            ArrayAdapter<String> isoAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, supportedISOModes);
            isoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            iso_spinner.setAdapter(isoAdapter);
            iso_spinner.setSelection(supportedISOModes.indexOf(prefs.getIso()));
        }


        //FOCUS MODE
        final List<String> supportedFocusModes = params.getSupportedFocusModes();
        ArrayAdapter<String> focusAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, supportedFocusModes);
        focusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        focus_mode_spinner.setAdapter(focusAdapter);
        focus_mode_spinner.setSelection(supportedFocusModes.indexOf(prefs.getFocus_mode()));


    }

}
