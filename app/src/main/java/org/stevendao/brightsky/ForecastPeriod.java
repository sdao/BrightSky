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
    private @NonNull WeatherCondition mCondition = WeatherCondition.UNKNOWN;
    private @NonNull OffsetDateTime mStartTime = OffsetDateTime.MIN;
    private @NonNull OffsetDateTime mEndTime = OffsetDateTime.MIN;
    private Optional<Integer> mTemperature = Optional.empty();
    private boolean mDaytime = true;

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
        try {
            String shortForecast = jsonObject.getString("shortForecast");
            WeatherCondition condition = WeatherCondition.find(shortForecast);

            String startTimeStr = jsonObject.getString("startTime");
            OffsetDateTime startTime = OffsetDateTime.parse(startTimeStr);

            String endTimeStr = jsonObject.getString("endTime");
            OffsetDateTime endTime = OffsetDateTime.parse(endTimeStr);

            Optional<Integer> temp = Optional.of(jsonObject.getInt("temperature"));

            boolean isDaytime = jsonObject.getBoolean("isDaytime");

            mCondition = condition;
            mStartTime = startTime;
            mEndTime = endTime;
            mTemperature = temp;
            mDaytime = isDaytime;
        } catch (JSONException ignored) {
        }
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
