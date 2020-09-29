package org.stevendao.brightsky;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class PreferenceFragment extends PreferenceFragmentCompat {
    private static final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_REQUEST_CODE) {
            return;
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted. Set the "use current location" pref on, and update all the
            // corresponding UI.
            getPreferenceManager()
                    .getSharedPreferences()
                    .edit()
                    .putBoolean(getString(R.string.key_use_current_location), true)
                    .apply();

            SwitchPreferenceCompat useCurrentLocation =
                    findPreference(getString(R.string.key_use_current_location));
            useCurrentLocation.setChecked(true);
            EditTextPreference location = findPreference(getString(R.string.key_static_place_name));
            location.setEnabled(false);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Context context = requireContext();

        // Start the notification foreground service when the option is toggled on.
        SwitchPreferenceCompat alwaysOnNotification =
                findPreference(getString(R.string.key_always_on_notification));
        alwaysOnNotification.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof Boolean)) {
                return true;
            }
            if ((Boolean) newValue) {
                AlwaysOnNotificationService.startServiceForcibly(context);
            }
            return true;
        });

        // Disable the location preference when the current-location switch is toggled on, and vice
        // versa. Also request location permission when the current-location switch is toggled on.
        // Also clear the cached current location if the current-location switch is toggled off.
        SwitchPreferenceCompat useCurrentLocation =
                findPreference(getString(R.string.key_use_current_location));
        EditTextPreference location = findPreference(getString(R.string.key_static_place_name));
        location.setEnabled(!useCurrentLocation.isChecked());
        useCurrentLocation.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof Boolean)) {
                return true;
            }

            // If the "use current location" switch was enabled but the permission hasn't been
            // granted, then request the permission and reject the pref change. We'll actually
            // toggle the pref change once the permission has been granted.
            if ((Boolean) newValue && ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_REQUEST_CODE);
                return false;
            }

            // Clear the cached current location when toggling the "use current location" switch.
            Utils.setCurrentLocation(context, null);

            // Update the location pref.
            location.setEnabled(!(Boolean) newValue);

            return true;
        });

        // Sync the location summary with its text.
        location.setSummary(location.getText());
        location.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof String)) {
                return true;
            }

            location.setSummary((String) newValue);
            return true;
        });
    }
}
