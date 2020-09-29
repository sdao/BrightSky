package org.stevendao.brightsky;

import androidx.annotation.NonNull;

public enum WeatherCondition {
    UNKNOWN("Unknown", R.color.stripe_unknown, R.color.text_unknown),
    FOG("Fog", R.color.stripe_fog, R.color.text_fog),
    HAZE("Haze", R.color.stripe_fog, R.color.text_fog),
    CLEAR("Clear", R.color.stripe_clear, R.color.text_clear),
    MOSTLY_CLEAR("Mostly Clear", R.color.stripe0, R.color.text0),
    PARTLY_CLOUDY("Partly Cloudy", R.color.stripe1, R.color.text1),
    MOSTLY_CLOUDY("Mostly Cloudy", R.color.stripe2, R.color.text2),
    OVERCAST("Overcast", R.color.stripe3, R.color.text3),
    LIGHT_RAIN("Light Rain", R.color.stripe4, R.color.text4),
    RAIN("Rain", R.color.stripe5, R.color.text5),
    LIGHT_SNOW("Light Snow", R.color.stripe6, R.color.text6),
    SNOW("Snow", R.color.stripe7, R.color.text7),
    ;

    final private String mDescription;
    final private int mColorId;
    final private int mTextColorId;

    WeatherCondition(String description, int colorId, int textColorId) {
        mDescription = description;
        mColorId = colorId;
        mTextColorId = textColorId;
    }

    public @NonNull String getDescription() {
        return mDescription;
    }

    public int getColorId() {
        return mColorId;
    }

    public int getTextColorId() {
        return mTextColorId;
    }

    public static @NonNull WeatherCondition find(String description)
    {
        String lower = description.toLowerCase();
        if (lower.contains("fog")) {
            return FOG;
        }
        else if (lower.contains("haze")) {
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
        else if (lower.contains("cloudy")) {
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
                || lower.contains("thunderstorm")) {
            if (lower.contains("isolated") || lower.contains("slight")) {
                return LIGHT_RAIN;
            }
            else {
                return RAIN;
            }
        }
        else if (lower.contains("snow")) {
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
