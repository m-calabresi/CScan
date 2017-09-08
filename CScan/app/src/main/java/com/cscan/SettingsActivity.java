package com.cscan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {
    static {
        //allow custom vector images to be used as icons
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MyPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //back arrow action
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        private boolean scanBarcodes;
        private boolean openLinks;
        private int browserType;

        private CheckBoxPreference prefScanBarcodes;
        private CheckBoxPreference prefOpenLinks;
        private ListPreference prefBrowserType;

        private SharedPreferences sharedPreferences;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            sharedPreferences = getActivity().getSharedPreferences(
                    getString(R.string.cscan_shared_preference_name), MODE_PRIVATE);

            scanBarcodes = sharedPreferences.getBoolean(getString(R.string.pref_key_scan_barcode), false);
            openLinks = sharedPreferences.getBoolean(getString(R.string.pref_key_open_links), false);
            browserType = sharedPreferences.getInt(getString(R.string.pref_key_browser_type), 0);

            prefBrowserType = (ListPreference) findPreference(getString(R.string.pref_key_browser_type));

            prefScanBarcodes = (CheckBoxPreference) findPreference(getString(R.string.pref_key_scan_barcode));
            prefScanBarcodes.setChecked(scanBarcodes);
            prefScanBarcodes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    scanBarcodes = !scanBarcodes;

                    sharedPreferences.edit()
                            .putBoolean(getString(R.string.pref_key_scan_barcode), scanBarcodes)
                            .apply();
                    prefScanBarcodes.setChecked(scanBarcodes);
                    return false;
                }
            });

            prefOpenLinks = (CheckBoxPreference) findPreference(getString(R.string.pref_key_open_links));
            prefOpenLinks.setChecked(openLinks);
            prefOpenLinks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    openLinks = !openLinks;

                    if(openLinks)
                        prefBrowserType.setIcon(ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_browser_type_blue));
                    else
                        prefBrowserType.setIcon(ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_browser_type_gray));

                    sharedPreferences.edit()
                            .putBoolean(getString(R.string.pref_key_open_links), openLinks)
                            .apply();
                    prefOpenLinks.setChecked(openLinks);
                    return false;
                }
            });

            prefBrowserType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    browserType = Integer.parseInt(o.toString());
                    sharedPreferences.edit()
                            .putInt(getString(R.string.pref_key_browser_type), browserType)
                            .apply();
                    return true;
                }
            });

            if(openLinks)
                prefBrowserType.setIcon(ContextCompat.getDrawable(getActivity(),
                        R.drawable.ic_browser_type_blue));
            else
                prefBrowserType.setIcon(ContextCompat.getDrawable(getActivity(),
                        R.drawable.ic_browser_type_gray));
        }
    }
}