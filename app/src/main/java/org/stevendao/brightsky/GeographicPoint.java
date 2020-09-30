package org.stevendao.brightsky;

import android.content.Context;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class GeographicPoint implements Parcelable {
    public static final Parcelable.Creator<GeographicPoint> CREATOR
            = new Parcelable.Creator<GeographicPoint>() {
        public GeographicPoint createFromParcel(Parcel in) {
            return new GeographicPoint(in);
        }

        public GeographicPoint[] newArray(int size) {
            return new GeographicPoint[size];
        }
    };

    private final double mLatitude;
    private final double mLongitude;
    private final @Nullable String mCity;
    private final @Nullable String mForecastUrl;
    private final @Nullable String mForecastHourlyUrl;

    public static @NonNull GeographicPoint request(
            @Nullable Location location,
            @NonNull RequestQueue volleyQueue,
            @Nullable Context context) {
        if (location == null) {
            return new GeographicPoint();
        }

        String url = String.format(
                Locale.ROOT,
                "https://api.weather.gov/points/%.4f,%.4f",
                location.getLatitude(),
                location.getLongitude());
        RequestFuture<JSONObject> future = Utils.requestJsonObject(volleyQueue, url);
        try {
            JSONObject jsonObject = future.get();
            return new GeographicPoint(jsonObject, context);
        } catch (InterruptedException | ExecutionException e) {
            return new GeographicPoint();
        }
    }

    private GeographicPoint() {
        mLatitude = Double.MAX_VALUE;
        mLongitude = Double.MAX_VALUE;
        mCity = null;
        mForecastUrl = null;
        mForecastHourlyUrl = null;
    }

    private GeographicPoint(@NonNull JSONObject jsonObject, @Nullable Context context) {
        double latitude;
        double longitude;
        String city;
        String forecastUrl;
        String forecastHourlyUrl;

        try {
            JSONArray coordinates = jsonObject
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");
            latitude = coordinates.getDouble(1);
            longitude = coordinates.getDouble(0);

            JSONObject properties = jsonObject.getJSONObject("properties");
            city = properties
                    .getJSONObject("relativeLocation")
                    .getJSONObject("properties")
                    .getString("city");
            double distance = properties
                    .getJSONObject("relativeLocation")
                    .getJSONObject("properties")
                    .getJSONObject("distance")
                    .getDouble("value");
            forecastUrl = properties.getString("forecast");
            forecastHourlyUrl = properties.getString("forecastHourly");

            // Sometimes the relativeLocation provided by api.weather.gov is really far away. If
            // it's more than 1000m (1km) away, then use the place name from the Geocoder API
            // instead.
            if (distance > 1000.0 && context != null) {
                String placeName = Utils.getPlaceNameFromLatLong(context, latitude, longitude);
                if (placeName != null) {
                    city = placeName;
                }
            }
        } catch (JSONException ignored) {
            latitude = Double.MAX_VALUE;
            longitude = Double.MAX_VALUE;
            city = null;
            forecastUrl = null;
            forecastHourlyUrl = null;
        }

        mLatitude = latitude;
        mLongitude = longitude;
        mCity = city;
        mForecastUrl = forecastUrl;
        mForecastHourlyUrl = forecastHourlyUrl;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public @Nullable String getCity() {
        return mCity;
    }

    public @Nullable String getForecastUrl() {
        return mForecastUrl;
    }

    public @Nullable String getForecastHourlyUrl() {
        return mForecastHourlyUrl;
    }

    private GeographicPoint(Parcel in) {
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mCity = in.readString();
        mForecastUrl = in.readString();
        mForecastHourlyUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeString(mCity);
        dest.writeString(mForecastUrl);
        dest.writeString(mForecastHourlyUrl);
    }
}
