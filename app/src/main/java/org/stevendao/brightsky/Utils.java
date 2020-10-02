package org.stevendao.brightsky;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.common.base.Splitter;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class Utils {
    public static RequestFuture<JSONObject> requestJsonObject(
            @NonNull RequestQueue queue,
            @NonNull String url) {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                future,
                future);
        request.setShouldRetryServerErrors(true);
        request.setRetryPolicy(new DefaultRetryPolicy(500, 5, 2f));
        request.setShouldCache(true);
        queue.add(request);
        return future;
    }

    public static Location getLocationFromPlaceName(
            @NonNull Context context,
            @NonNull String placeName) {
        List<Address> addressList = null;
        try {
            addressList = new Geocoder(context).getFromLocationName(placeName, 1);
        } catch (IOException ignored) {
        }

        if (addressList != null && !addressList.isEmpty()) {
            Location location = new Location("");
            location.setLatitude(addressList.get(0).getLatitude());
            location.setLongitude(addressList.get(0).getLongitude());
            return location;
        }

        return null;
    }

    public static String getPlaceNameFromLatLong(
            @NonNull Context context,
            double latitude,
            double longitude) {
        List<Address> addressList = null;
        try {
            addressList = new Geocoder(context).getFromLocation(latitude, longitude, 1);
        } catch (IOException ignored) {
        }

        if (addressList != null && !addressList.isEmpty()) {
            if (addressList.get(0).getSubLocality() != null) {
                return addressList.get(0).getSubLocality();
            }
            return addressList.get(0).getLocality();
        }

        return null;
    }

    public static boolean getUseCurrentLocation(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.key_use_current_location);
        return prefs.getBoolean(key, false);
    }

    public static @Nullable Location getCurrentLocation(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String latLongKey = context.getString(R.string.key_current_lat_long);
        String latLong = prefs.getString(latLongKey, null);
        if (latLong == null) {
            return null;
        }

        try {
            NumberFormat format = NumberFormat.getInstance(Locale.ROOT);
            double[] coordinates = new double[2];
            int i = 0;
            for (String s : Splitter.on(' ').split(latLong)) {
                coordinates[i] = Objects.requireNonNull(format.parse(s)).doubleValue();
                i++;
            }

            Location l = new Location("");
            l.setLatitude(coordinates[0]);
            l.setLongitude(coordinates[1]);
            return l;
        }
        catch (ParseException | ArrayIndexOutOfBoundsException | NullPointerException ignored) {
            return null;
        }
    }

    public static void setCurrentLocation(@NonNull Context context, Location location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String latLongKey = context.getString(R.string.key_current_lat_long);

        if (location == null) {
            prefs.edit().remove(latLongKey).apply();
            return;
        }

        String latLong = String.format(
                Locale.ROOT, "%f %f", location.getLatitude(), location.getLongitude());
        prefs.edit()
                .putString(latLongKey, latLong)
                .apply();
    }

    public static String getStaticPlaceName(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.key_static_place_name);
        return prefs.getString(key, "New York, NY 10028");
    }

    public static Bitmap createTimelineImage(
            @NonNull Context context,
            @NonNull GeographicPoint point,
            @NonNull List<ForecastPeriod> forecast,
            @Px int width,
            @Px int height,
            @Px int gutter) {
        final int heightWithGutter = height + gutter;
        final float cornerRadius = height * 0.125f;

        Bitmap b = Bitmap.createBitmap(width, heightWithGutter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);

        TextPaint textPaint = new TextPaint();
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(height / 3f);

        Rect xBounds = new Rect();
        textPaint.getTextBounds("x", 0, 1, xBounds);
        final float textHeight = xBounds.height();

        // Draw the timeline segments.
        Paint paint = new Paint();
        float periodWidth = width / (float) forecast.size();
        for (int i = 0; i < forecast.size();) {
            final WeatherCondition periodStart = forecast.get(i).getCondition();

            // Combine all the subsequent periods with the same weather condition.
            int j = i;
            while (j < forecast.size() && forecast.get(j).getCondition() == periodStart) {
                j++;
            }

            // Set the paint colors from the condition enum.
            paint.setColor(context.getColor(periodStart.getColorId()));
            textPaint.setColor(context.getColor(periodStart.getTextColorId()));

            // Fill the background.
            final float left = periodWidth * i;
            final float right = periodWidth * j;
            canvas.drawRect(Math.round(left), 0, Math.round(right), height, paint);

            // Draw the text if it fits inside the segment.
            final String description = context.getString(periodStart.getDescriptionId());
            final float textWidth = textPaint.measureText(description);
            if (textWidth * 1.1 < (right - left)) {
                canvas.drawText(
                        description,
                        (left + right) / 2f,
                        (height + textHeight) / 2f,
                        textPaint);
            }

            i = j;
        }

        // Draw the daylight arcs over contiguous daytime periods.
        paint.setColor(context.getColor(R.color.daylight));
        final OffsetDateTime startTime = forecast.get(0).getStartTime();
        final Daytime[] daytimes = {
                new Daytime(point, startTime),
                new Daytime(point, startTime.plusDays(1)),
        };
        for (final Daytime daytime : daytimes) {
            Duration sunrise = Duration.between(startTime, daytime.getSunrise());
            Duration sunset = Duration.between(startTime, daytime.getSunset());

            float secondsPerDay = Duration.ofDays(1).getSeconds();
            float left = (sunrise.getSeconds() / secondsPerDay) * width;
            float right = (sunset.getSeconds() / secondsPerDay) * width;

            canvas.drawOval(left, height * 0.75f, right, height * 1.25f, paint);
        }

        // Clip out a rounded rectangle border.
        Path inverseRoundRect = new Path();
        inverseRoundRect.addRoundRect(
                0, 0, width, height, cornerRadius, cornerRadius, Path.Direction.CW);
        inverseRoundRect.toggleInverseFillType();

        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.TRANSPARENT);
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        canvas.drawPath(inverseRoundRect, xferPaint);

        // Draw the gutter with tick marks.
        if (gutter > 0) {
            paint.setColor(context.getColor(R.color.tick));
            paint.setStrokeWidth(4.0f);

            for (int i = 1; i < forecast.size(); ++i) {
                if ((i + 2) % 4 == 0) {
                    canvas.drawLine(
                            periodWidth * i,
                            height + gutter * 0.33f,
                            periodWidth * i,
                            heightWithGutter,
                            paint);
                } else {
                    canvas.drawLine(
                            periodWidth * i,
                            height + gutter * 0.33f,
                            periodWidth * i,
                            height + gutter * 0.67f,
                            paint);
                }
            }
        }

        return b;
    }

    public static Icon createIcon(Optional<Integer> temperature) {
        Bitmap b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);

        TextPaint textPaint = new TextPaint();
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        final String text = temperature.map(x -> x + "\u00B0").orElse("--\u00B0");
        if (text.length() <= 3) {
            textPaint.setTextSize(84); // = (7/8) * 96
        } else {
            textPaint.setTextSize(72); // = (6/8) * 96
        }

        // Squish the text horizontally so that it fits within the icon. (Don't stretch it, though.)
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        textPaint.setTextScaleX(Math.min(1.0f, (96f - 4f) / bounds.width()));

        // Get the bounds of the text without the degree symbol. Center the text vertically,
        // ignoring the degree symbol.
        textPaint.getTextBounds(text, 0, text.length() - 1, bounds);
        canvas.drawText(text, 96f / 2f, (96f / 2f) - (bounds.top / 2f), textPaint);
        return Icon.createWithBitmap(b);
    }
}
