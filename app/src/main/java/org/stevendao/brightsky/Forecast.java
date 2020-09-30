package org.stevendao.brightsky;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Forecast implements Parcelable {
    public static final Parcelable.Creator<Forecast> CREATOR
            = new Parcelable.Creator<Forecast>() {
        public Forecast createFromParcel(Parcel in) {
            return new Forecast(in);
        }

        public Forecast[] newArray(int size) {
            return new Forecast[size];
        }
    };

    private final @NonNull GeographicPoint mPoint;
    private final @NonNull List<ForecastPeriod> mForecastPeriods;
    private final @Nullable String mDescription;

    public static Forecast request(
            @NonNull GeographicPoint point,
            @NonNull RequestQueue volleyQueue) {
        if (point.getForecastUrl() == null || point.getForecastHourlyUrl() == null) {
            return new Forecast(point);
        }

        RequestFuture<JSONObject> forecastFuture =
                Utils.requestJsonObject(volleyQueue, point.getForecastUrl());
        RequestFuture<JSONObject> forecastHourlyFuture =
                Utils.requestJsonObject(volleyQueue, point.getForecastHourlyUrl());

        try {
            JSONObject forecast = forecastFuture.get();
            JSONObject forecastHourly = forecastHourlyFuture.get();
            return new Forecast(point, forecast, forecastHourly);
        } catch (InterruptedException | ExecutionException e) {
            return new Forecast(point);
        }
    }

    private Forecast(@NonNull GeographicPoint point)
    {
        mPoint = point;
        mForecastPeriods = Collections.emptyList();
        mDescription = null;
    }

    private Forecast(
            @NonNull GeographicPoint point,
            @NonNull JSONObject rawForecast,
            @NonNull JSONObject rawHourlyForecast)
    {
        mPoint = point;

        String description;
        try {
            description = rawForecast
                    .getJSONObject("properties")
                    .getJSONArray("periods")
                    .getJSONObject(0)
                    .getString("detailedForecast");
        } catch (JSONException e) {
            description = null;
        }
        mDescription = description;

        mForecastPeriods = new ArrayList<>();
        try {
            JSONArray periods = rawHourlyForecast
                    .getJSONObject("properties")
                    .getJSONArray("periods");
            for (int i = 0; i < periods.length(); ++i) {
                JSONObject period = periods.getJSONObject(i);
                mForecastPeriods.add(new ForecastPeriod(period));
            }
        } catch (JSONException e) {
            mForecastPeriods.clear();
        }
    }

    public @NonNull GeographicPoint getGeographicPoint() {
        return mPoint;
    }

    public @Nullable String getDescription() {
        return mDescription;
    }

    public @NonNull List<ForecastPeriod> getForecastPeriods() {
        return Collections.unmodifiableList(mForecastPeriods);
    }

    public @NonNull List<ForecastPeriod> get24HourForecastPeriods() {
        // Get the current time in our local time zone.
        final OffsetDateTime now = OffsetDateTime.now();

        // Assume that the weather location doesn't change time zones, so its offset is always the
        // offset from the first period. Convert the current time to the weather location's offset
        // and move it to the beginning of the hour.
        final ZoneOffset offset = mForecastPeriods.isEmpty()
                ? now.getOffset()
                : mForecastPeriods.get(0).getStartTime().getOffset();
        final OffsetDateTime beginningOfHour = now
                .withOffsetSameInstant(offset)
                .withMinute(0).withSecond(0).withNano(0);

        ArrayDeque<ForecastPeriod> periodsQueue = new ArrayDeque<>(mForecastPeriods);
        ArrayList<ForecastPeriod> result = new ArrayList<>();
        for (int i = 0; i < 24; ++i) {
            final OffsetDateTime target = beginningOfHour.plusHours(i);

            // Pop off all the items that end before or at the target time.
            ForecastPeriod period;
            while ((period = periodsQueue.peek()) != null) {
                if (period.getEndTime().isAfter(target)) {
                    break;
                }
                periodsQueue.pop();
            }

            // Check to see if the top of the queue (the last peeked) starts before or at the target
            // time. (Null means that we are missing data for the target time -- the periods have
            // "skipped" over the target time, or there is no more data.)
            if (period != null && !period.getStartTime().isAfter(target)) {
                result.add(period.withTimeRange(target, target.plusHours(1)));
            }
            else {
                result.add(new ForecastPeriod(
                        WeatherCondition.UNKNOWN,
                        target,
                        target.plusHours(1),
                        Optional.empty(),
                        true));
            }
        }

        return result;
    }

    private Forecast(Parcel in) {
        GeographicPoint point = in.readParcelable(GeographicPoint.class.getClassLoader());
        if (point == null) {
            throw new NullPointerException("GeographicPoint stored in Parcel is unexpectedly null");
        }

        mPoint = point;

        mForecastPeriods = new ArrayList<>();
        in.readParcelableList(mForecastPeriods, ForecastPeriod.class.getClassLoader());

        mDescription = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mPoint, flags);
        dest.writeParcelableList(mForecastPeriods, flags);
        dest.writeString(mDescription);
    }
}
