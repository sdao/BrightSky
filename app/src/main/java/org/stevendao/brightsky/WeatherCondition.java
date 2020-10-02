package org.stevendao.brightsky;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public enum WeatherCondition {
    UNKNOWN(R.string.unknown, R.color.stripe_unknown, R.color.text_unknown),
    FOG(R.string.fog, R.color.stripe_fog, R.color.text_fog),
    ICE(R.string.ice, R.color.stripe_ice, R.color.text_ice),
    HAZE(R.string.haze, R.color.stripe_haze, R.color.text_haze),
    CLEAR(R.string.clear, R.color.stripe_clear, R.color.text_clear),
    MOSTLY_CLEAR(R.string.mostly_clear, R.color.stripe0, R.color.text0),
    PARTLY_CLOUDY(R.string.partly_cloudy, R.color.stripe1, R.color.text1),
    MOSTLY_CLOUDY(R.string.mostly_cloudy, R.color.stripe2, R.color.text2),
    OVERCAST(R.string.overcast, R.color.stripe3, R.color.text3),
    LIGHT_RAIN(R.string.light_rain, R.color.stripe4, R.color.text4),
    RAIN(R.string.rain, R.color.stripe5, R.color.text5),
    LIGHT_SNOW(R.string.light_snow, R.color.stripe6, R.color.text6),
    SNOW(R.string.snow, R.color.stripe7, R.color.text7),
    ;

    private final @StringRes int mDescription;
    private final @ColorRes int mColorId;
    private final @ColorRes int mTextColorId;

    WeatherCondition(
            @StringRes int descriptionId,
            @ColorRes int colorId,
            @ColorRes int textColorId) {
        mDescription = descriptionId;
        mColorId = colorId;
        mTextColorId = textColorId;
    }

    public @StringRes int getDescriptionId() {
        return mDescription;
    }

    public @ColorRes int getColorId() {
        return mColorId;
    }

    public @ColorRes int getTextColorId() {
        return mTextColorId;
    }

    public static @NonNull WeatherCondition find(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("fog")) {
            return FOG;
        }
        else if (lower.contains("ice") || lower.contains("frost")) {
            return ICE;
        }
        else if (lower.contains("haze")
                || lower.contains("dust")
                || lower.contains("sand")
                || lower.contains("smoke")
                || lower.contains("ash")) {
            return HAZE;
        }
        else if (lower.contains("clear") || lower.contains("sunny")) {
            if (lower.contains("partly")) {
                return PARTLY_CLOUDY;
            }
            else if (lower.contains("mostly")) {
                return MOSTLY_CLEAR;
            }
            else {
                return CLEAR;
            }
        }
        else if (lower.contains("cloud")) {
            if (lower.contains("partly")) {
                return PARTLY_CLOUDY;
            }
            else if (lower.contains("mostly")) {
                return MOSTLY_CLOUDY;
            }
            else {
                return OVERCAST;
            }
        }
        else if (lower.contains("rain")
                || lower.contains("drizzle")
                || lower.contains("shower")
                || lower.contains("thunderstorm")
                || lower.contains("spray")) {
            if (lower.contains("isolated") || lower.contains("slight")) {
                return LIGHT_RAIN;
            }
            else {
                return RAIN;
            }
        }
        else if (lower.contains("snow")
                || lower.contains("sleet")
                || lower.contains("flurries")
                || lower.contains("blizzard")
                || lower.contains("wintry")) {
            if (lower.contains("isolated") || lower.contains("slight")) {
                return LIGHT_SNOW;
            }
            else {
                return SNOW;
            }
        }

        return UNKNOWN;
    }
}
