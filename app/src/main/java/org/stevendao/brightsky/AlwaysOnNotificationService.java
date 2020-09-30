package org.stevendao.brightsky;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class AlwaysOnNotificationService
        extends Service
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Retention(SOURCE)
    @IntDef({MESSAGE_START_SERVICE, MESSAGE_NEW_FORECAST, MESSAGE_API_FAILURE,
            MESSAGE_NO_CURRENT_LOCATION, MESSAGE_INVALID_STATIC_LOCATION})
    public @interface Message {}

    /// Sentinel message used to ensure the service is alive.
    public static final int MESSAGE_START_SERVICE = 0;

    /// Message with corresponding, valid forecast data.
    public static final int MESSAGE_NEW_FORECAST = 1;

    /// Message indicating a problem reaching the weather API.
    public static final int MESSAGE_API_FAILURE = 2;

    /// Message indicating that the current location hasn't been obtained yet from the system.
    public static final int MESSAGE_NO_CURRENT_LOCATION = 3;

    /// Message indicating that the user-specified static location isn't valid.
    public static final int MESSAGE_INVALID_STATIC_LOCATION = 4;

    private static final String TAG = AlwaysOnNotificationService.class.getName();
    private static final int LOCATION_INTERVAL_MINS = 15;
    private static final int LOCATION_FASTEST_INTERVAL_MINS = 1;
    private static final float LOCATION_SMALLEST_DISPLACEMENT_M = 1000f;
    private static final String CHANNEL_ID = "org.stevendao.brightsky.ALWAYS_ON_CHANNEL";
    private static final int NOTIFICATION_ID = 42; // Cannot be 0.
    private static final String FORECAST_EXTRAS_KEY = "forecast";
    private static final String STATUS_EXTRAS_KEY = "status";

    private Set<String> mOldPrefsKeys = Collections.emptySet();

    private final IBinder mBinder = new Binder();

    private FusedLocationProviderClient mLocationProvider = null;

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location loc =locationResult.getLastLocation();
            if (loc != null) {
                Log.d(TAG, "Location retrieval successful, updating cached location");
                Utils.setCurrentLocation(AlwaysOnNotificationService.this, loc);
            }
            else {
                Log.d(TAG, "Location retrieval unsuccessful");
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service onCreate");

        // Register the channel with the system; you can't change the importance or other
        // notification behaviors after this.
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Always-on notification", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Weather conditions and temperature");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        // Listen to shared preferences changes (this is a weak reference).
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mOldPrefsKeys = sharedPreferences.getAll().keySet();

        // Start periodic data updates and listen to location updates (if needed).
        Worker.startPeriodic(this);

        mLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        updateLocationListener();

        // Show the loading notification while we wait for data.
        startNotification("Loading weather forecast...");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");

        // Stop periodic data updates and stop location updates.
        Worker.stopPeriodic(this);
        mLocationProvider.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        @Message int message;
        Forecast forecast;
        if (intent == null) {
            message = MESSAGE_START_SERVICE;
            forecast = null;
        }
        else {
            message = intent.getIntExtra(STATUS_EXTRAS_KEY, MESSAGE_START_SERVICE);
            forecast = intent.getParcelableExtra(FORECAST_EXTRAS_KEY);
        }

        switch (message) {
            case MESSAGE_START_SERVICE:
                // This is only used to ensure that the service starts, but otherwise does nothing.
                break;
            case MESSAGE_NEW_FORECAST:
                // Create the regular forecast notification.
                if (forecast != null) {
                    startNotification(forecast);
                }
                else {
                    startNotification("Something is broken");
                }
                break;
            case MESSAGE_API_FAILURE:
                startNotification("Forecast is currently unavailable");
                break;
            case MESSAGE_NO_CURRENT_LOCATION:
                startNotification("Getting current location...");
                break;
            case MESSAGE_INVALID_STATIC_LOCATION:
                startNotification("Invalid location specified");
                break;
        }

        return START_STICKY;
    }

    public void startNotification(@NonNull Forecast forecast) {
        RemoteViews bigContent = new RemoteViews(getPackageName(), R.layout.notification);
        RemoteViews smallContent = new RemoteViews(getPackageName(), R.layout.notification_small);

        String contentText;
        final String desc = forecast.getDescription();
        final String city = forecast.getGeographicPoint().getCity();
        if (city == null || desc == null) {
            contentText = "No information available.";
        } else {
            contentText = city + ": " + desc;
        }
        bigContent.setTextViewText(R.id.descTextView, contentText);

        final List<ForecastPeriod> twentyFour = forecast.get24HourForecastPeriods();
        final Icon icon = Utils.createIcon(twentyFour.get(0).getTemperature());

        final int[] periods = {2, 6, 10, 14, 18, 22};
        final @IdRes int[] tempViews =
                {R.id.temp2, R.id.temp6, R.id.temp10, R.id.temp14, R.id.temp18, R.id.temp22};
        final @IdRes int[] timeViews =
                {R.id.time2, R.id.time6, R.id.time10, R.id.time14, R.id.time18, R.id.time22};
        for (int i = 0; i < periods.length; ++i) {
            ForecastPeriod fp = twentyFour.get(periods[i]);
            bigContent.setTextViewText(
                    tempViews[i],
                    fp
                            .getTemperature()
                            .map(x -> String.format(Locale.ROOT, "\u00a0%d\u00b0", x))
                            .orElse("--"));
            bigContent.setTextViewText(
                    timeViews[i],
                    fp.getFormattedStartTime());
        }

        bigContent.setImageViewBitmap(
                R.id.imageView,
                Utils.createTimelineImage(
                        this,
                        forecast.getGeographicPoint(),
                        twentyFour,
                        1600,
                        160,
                        40));
        smallContent.setImageViewBitmap(
                R.id.imageView,
                Utils.createTimelineImage(
                        this,
                        forecast.getGeographicPoint(),
                        twentyFour,
                        1600,
                        160,
                        0));

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Current conditions")
                .setContentText(contentText)
                .setOnlyAlertOnce(true)
                .setSmallIcon(icon)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomContentView(smallContent)
                .setCustomBigContentView(bigContent)
                .setShowWhen(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Updated foreground notification with forecast");
    }

    public void startNotification(String contentText) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Current conditions")
                .setContentText(contentText)
                .setOnlyAlertOnce(true)
                .setSmallIcon(Utils.createIcon(Optional.empty()))
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setShowWhen(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Updated foreground notification, contentText = " + contentText);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // If the notification is disabled, then stop this service.
        final String alwaysOnNotificationKey = getString(R.string.key_always_on_notification);
        if (key.equals(alwaysOnNotificationKey)) {
            if (!sharedPreferences.getBoolean(alwaysOnNotificationKey, false)) {
                stopSelf();
                return;
            }
        }

        // If the "use current location" preference is changed, then update the location listener
        // correspondingly, then do an immediate data update.
        final String useCurrentLocationKey = getString(R.string.key_use_current_location);
        if (key.equals(useCurrentLocationKey)) {
            Log.d(TAG, "Use current location pref changed");
            updateLocationListener();
            Worker.doOnce(this);
        }

        // Check either the current location or the static place name.
        if (sharedPreferences.getBoolean(useCurrentLocationKey, false)) {
            // If the current location has been newly-cached (i.e., it didn't previously exist
            // in the shared preferences), then trigger an update immediately. If the current
            // location was already previously cached, then wait until the next periodic update to
            // pick up the new value.
            final String currentLatLongKey = getString(R.string.key_current_lat_long);
            if (key.equals(currentLatLongKey) && !mOldPrefsKeys.contains(currentLatLongKey)) {
                Log.d(TAG, "Current location updated for the first time");
                Worker.doOnce(this);
            }
        }
        else {
            // If the static place name was changed, then do an immediate data update.
            final String staticPlaceNameKey = getString(R.string.key_static_place_name);
            if (key.equals(staticPlaceNameKey)) {
                Log.d(TAG, "Static place name updated");
                Worker.doOnce(this);
            }
        }

        mOldPrefsKeys = sharedPreferences.getAll().keySet();
    }

    private void updateLocationListener() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String useCurrentLocationKey = getString(R.string.key_use_current_location);
        if (sharedPreferences.getBoolean(useCurrentLocationKey, false)) {
            Log.d(TAG, "Use current location pref is on, requesting location updates");
            LocationRequest request = LocationRequest.create()
                    .setInterval(TimeUnit.MILLISECONDS.convert(
                            LOCATION_INTERVAL_MINS, TimeUnit.MINUTES))
                    .setFastestInterval(TimeUnit.MILLISECONDS.convert(
                            LOCATION_FASTEST_INTERVAL_MINS, TimeUnit.MINUTES))
                    .setSmallestDisplacement(LOCATION_SMALLEST_DISPLACEMENT_M)
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            try {
                mLocationProvider.requestLocationUpdates(request, mLocationCallback, null);
            }
            catch (SecurityException ignored) {
                Log.w(TAG, "Unable to request location updates");
            }
        }
        else {
            Log.d(TAG, "Use current location pref is off, removing location updates");
            mLocationProvider.removeLocationUpdates(mLocationCallback);
        }
    }

    public static void startServiceIfEnabled(@NonNull Context context) {
        sendStartCommand(context, AlwaysOnNotificationService.MESSAGE_START_SERVICE, null, false);
    }

    public static void startServiceForcibly(@NonNull Context context) {
        sendStartCommand(context, AlwaysOnNotificationService.MESSAGE_START_SERVICE, null, true);
    }

    public static void notifyService(
            @NonNull Context context,
            @Message int status,
            @Nullable Forecast forecast) {
        sendStartCommand(context, status, forecast, false);
    }

    private static void sendStartCommand(
            @NonNull Context context,
            @Message int status,
            @Nullable Forecast forecast,
            boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.key_always_on_notification);
        if (force || prefs.getBoolean(key, false)) {
            Intent intent = new Intent(context, AlwaysOnNotificationService.class);
            intent.putExtra(FORECAST_EXTRAS_KEY, forecast);
            intent.putExtra(STATUS_EXTRAS_KEY, status);
            context.startForegroundService(intent);
        }
    }
}
