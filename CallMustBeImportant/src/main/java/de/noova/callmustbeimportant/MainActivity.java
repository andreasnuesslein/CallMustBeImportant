package de.noova.callmustbeimportant;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

    }
}
