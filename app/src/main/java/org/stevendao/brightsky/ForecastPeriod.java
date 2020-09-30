package org.stevendao.brightsky;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ForecastPeriod implements Parcelable {
    public static final Parcelable.Creator<ForecastPeriod> CREATOR
            = new Parcelable.Creator<ForecastPeriod>() {
        public ForecastPeriod createFromParcel(Parcel in) {
            return new ForecastPeriod(in);
        }

        public ForecastPeriod[] newArray(int size) {
            return new ForecastPeriod[size];
        }
    };

    private static final DateTimeFormatter sFormatter = DateTimeFormatter.ofPattern("ha");

    private final @NonNull WeatherCondition mCondition;
    private final @NonNull OffsetDateTime mStartTime;
    private final @NonNull OffsetDateTime mEndTime;
    private final Optional<Integer> mTemperature;
    private final boolean mDaytime;

    public ForecastPeriod(
            @NonNull WeatherCondition condition,
            @NonNull OffsetDateTime start,
            @NonNull OffsetDateTime end,
            @NonNull Optional<Integer> temperature,
            boolean isDaytime) {
        mCondition = condition;
        mStartTime = start;
        mEndTime = end;
        mTemperature = temperature;
        mDaytime = isDaytime;
    }

    public ForecastPeriod(JSONObject jsonObject) {
        WeatherCondition condition;
        OffsetDateTime startTime;
        OffsetDateTime endTime;
        Optional<Integer> temp;
        boolean isDaytime;

        try {
            String shortForecast = jsonObject.getString("shortForecast");
            condition = WeatherCondition.find(shortForecast);

            String startTimeStr = jsonObject.getString("startTime");
            startTime = OffsetDateTime.parse(startTimeStr);

            String endTimeStr = jsonObject.getString("endTime");
            endTime = OffsetDateTime.parse(endTimeStr);

            temp = Optional.of(jsonObject.getInt("temperature"));

            isDaytime = jsonObject.getBoolean("isDaytime");

        } catch (JSONException ignored) {
            condition = WeatherCondition.UNKNOWN;
            startTime = OffsetDateTime.MIN;
            endTime = OffsetDateTime.MIN;
            temp = Optional.empty();
            isDaytime = true;
        }

        mCondition = condition;
        mStartTime = startTime;
        mEndTime = endTime;
        mTemperature = temp;
        mDaytime = isDaytime;
    }

    public @NonNull WeatherCondition getCondition() {
        return mCondition;
    }

    public @NonNull OffsetDateTime getStartTime() {
        return mStartTime;
    }

    public @NonNull OffsetDateTime getEndTime() {
        return mEndTime;
    }

    public Optional<Integer> getTemperature() {
        return mTemperature;
    }

    public boolean isDaytime() {
        return mDaytime;
    }

    public @NonNull String getFormattedStartTime() {
        return sFormatter.format(mStartTime).toLowerCase();
    }

    public @NonNull ForecastPeriod withTimeRange(OffsetDateTime start, OffsetDateTime end) {
        return new ForecastPeriod(mCondition, start, end, mTemperature, mDaytime);
    }

    private ForecastPeriod(Parcel in) {
        mCondition = WeatherCondition.values()[in.readInt()];
        mStartTime = OffsetDateTime.parse(in.readString());
        mEndTime = OffsetDateTime.parse(in.readString());

        boolean temperaturePresent = in.readBoolean();
        int temperature = in.readInt();
        if (temperaturePresent) {
            mTemperature = Optional.of(temperature);
        }
        else {
            mTemperature = Optional.empty();
        }

        mDaytime = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCondition.ordinal());
        dest.writeString(mStartTime.toString());
        dest.writeString(mEndTime.toString());

        dest.writeBoolean(mTemperature.isPresent());
        dest.writeInt(mTemperature.orElse(0));

        dest.writeBoolean(mDaytime);
    }
}
