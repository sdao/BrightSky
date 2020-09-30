package org.stevendao.brightsky;

import com.ibm.icu.impl.CalendarAstronomer;

import java.time.Instant;
import java.time.OffsetDateTime;

public class Daytime {
    private final OffsetDateTime mSunrise;
    private final OffsetDateTime mSunset;

    /// See <frameworks/base/services/core/java/com/android/server/twilight/TwilightService.java>.
    public Daytime(GeographicPoint point, OffsetDateTime offsetDateTime) {
        final CalendarAstronomer ca = new CalendarAstronomer(
                point.getLongitude(), point.getLatitude());

        final OffsetDateTime noon =
                offsetDateTime.withHour(12).withMinute(0).withSecond(0).withNano(0);
        ca.setTime(noon.toInstant().toEpochMilli());

        final long sunriseTimeMillis = ca.getSunRiseSet(/*rise=*/ true);
        final long sunsetTimeMillis  = ca.getSunRiseSet(/*rise=*/ false);
        mSunrise = Instant.ofEpochMilli(sunriseTimeMillis).atOffset(offsetDateTime.getOffset());
        mSunset = Instant.ofEpochMilli(sunsetTimeMillis).atOffset(offsetDateTime.getOffset());
    }

    public OffsetDateTime getSunrise() {
        return mSunrise;
    }

    public OffsetDateTime getSunset() {
        return mSunset;
    }
}
