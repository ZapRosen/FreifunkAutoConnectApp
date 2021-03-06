package com.example.tobiastrumm.freifunkautoconnect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;


public class SettingsActivity extends AppCompatActivity {

    public static class SettingsFragment extends PreferenceFragment {
        private static final String NUMBER_NODES_STRING_DEFAULT = "10";

        SwitchPreference switchfNoNotificationConnected;
        SwitchPreference switchVibrate;
        SwitchPreference switchPlaySound;
        EditTextPreference editTextNumberNodes;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager prefMan = getPreferenceManager();
            prefMan.setSharedPreferencesName(getString(R.string.shared_preference_key_settings));

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Enable more notification settings when notification was activated.
            SwitchPreference switchPref = (SwitchPreference) prefMan.findPreference("pref_notification");

            switchfNoNotificationConnected = (SwitchPreference) prefMan.findPreference("pref_no_notification_connected");
            switchfNoNotificationConnected.setEnabled(switchPref.isChecked());

            switchVibrate = (SwitchPreference) prefMan.findPreference("pref_notification_vibrate");
            switchVibrate.setEnabled(switchPref.isChecked());

            switchPlaySound = (SwitchPreference) prefMan.findPreference("pref_notification_sound");
            switchPlaySound.setEnabled(switchPref.isChecked());

            switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean myValue = (Boolean) newValue;

                if (myValue)
                    getActivity().startService(new Intent(getActivity(), NotificationService.class));
                else
                    getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                switchfNoNotificationConnected.setEnabled(myValue);
                switchVibrate.setEnabled(myValue);
                switchPlaySound.setEnabled(myValue);

                return true;
            });

            // Restart NotificationService if a setting affecting notifications was changed.
            Preference.OnPreferenceChangeListener restartServiceListener = (preference, newValue) -> {
                // Restart NotificationService
                getActivity().stopService(new Intent(getActivity(), NotificationService.class));
                getActivity().startService(new Intent(getActivity(), NotificationService.class));
                return true;
            };
            switchfNoNotificationConnected.setOnPreferenceChangeListener(restartServiceListener);
            switchVibrate.setOnPreferenceChangeListener(restartServiceListener);
            switchPlaySound.setOnPreferenceChangeListener(restartServiceListener);

            editTextNumberNodes = (EditTextPreference) prefMan.findPreference("pref_nearest_ap_number_nodes_string");
            // Set summary to current saved value.
            editTextNumberNodes.setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_nearest_ap_number_nodes_string", NUMBER_NODES_STRING_DEFAULT));
            // Deactivate Ok button if user enters 0 or nothing.
            editTextNumberNodes.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    Dialog d = editTextNumberNodes.getDialog();
                    if(d instanceof AlertDialog){
                        String text = editable.toString();
                        ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(text.equals("") || text.equals("0")));
                    }
                }
            });
            // Number of APs was saved as String, now save it as int too.
            editTextNumberNodes.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences.Editor editor = prefMan.getSharedPreferences().edit();
                editor.putInt("pref_nearest_ap_number_nodes", Integer.valueOf((String) newValue));
                editor.apply();
                // update summary
                editTextNumberNodes.setSummary((String)newValue);
                return true;
            });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Necessary or a new SettingsFragment will be created every time the screen is rotated.
        if(savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SettingsFragment())
                    .commit();
        }
    }
}
