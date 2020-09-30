package org.stevendao.brightsky;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.concurrent.TimeUnit;

public class Worker extends androidx.work.Worker {
    private static final int WORK_INTERVAL_MINS = 30;
    private static final int WORK_RUN_ATTEMPTS = 5;
    private static final String UNIQUE_PERIODIC_WORK_NAME = "org.stevendao.brightsky.WORKER";
    private static final String UNIQUE_ONE_SHOT_WORK_NAME = "org.stevendao.brightsky.ONE_SHOT";

    public Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context context = getApplicationContext();
        Location location;
        if (Utils.getUseCurrentLocation(context)) {
            location = Utils.getCurrentLocation(context);
            Log.d(Worker.class.getName(), "Current location: " + location);

            if (location == null) {
                AlwaysOnNotificationService.notifyService(
                        context,
                        AlwaysOnNotificationService.MESSAGE_NO_CURRENT_LOCATION,
                        null);
                return Result.failure();
            }
        }
        else {
            location = Utils.getLocationFromPlaceName(context, Utils.getStaticPlaceName(context));
            Log.d(Worker.class.getName(), "Static location: " + location);

            if (location == null) {
                AlwaysOnNotificationService.notifyService(
                        context,
                        AlwaysOnNotificationService.MESSAGE_INVALID_STATIC_LOCATION,
                        null);
                return Result.failure();
            }
        }

        RequestQueue queue = Volley.newRequestQueue(context);

        GeographicPoint newPoint = GeographicPoint.request(location, queue, context);
        Log.d(Worker.class.getName(), "Point: " + newPoint.getCity());

        Forecast newForecast = Forecast.request(newPoint, queue);
        Log.d(Worker.class.getName(),
                "Forecast: " + newForecast.getForecastPeriods().size() + " periods");
        if (newForecast.getForecastPeriods().isEmpty()) {
            if (getRunAttemptCount() < WORK_RUN_ATTEMPTS) {
                return Result.retry();
            }

            AlwaysOnNotificationService.notifyService(
                    context,
                    AlwaysOnNotificationService.MESSAGE_API_FAILURE,
                    null);
            return Result.failure();
        }

        AlwaysOnNotificationService.notifyService(
                context, AlwaysOnNotificationService.MESSAGE_NEW_FORECAST, newForecast);
        return Result.success();
    }

    public static void startPeriodic(@NonNull Context context) {
        Log.d(Worker.class.getName(), "Starting periodic work request");
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                new PeriodicWorkRequest.Builder(Worker.class, WORK_INTERVAL_MINS, TimeUnit.MINUTES)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                WorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .build());
    }

    public static void stopPeriodic(@NonNull Context context) {
        Log.d(Worker.class.getName(), "Stopping periodic work request");
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME);
    }

    public static void doOnce(@NonNull Context context) {
        Log.d(Worker.class.getName(), "Enqueuing a one-time update");
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_SHOT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(Worker.class)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                WorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .build());
    }
}
