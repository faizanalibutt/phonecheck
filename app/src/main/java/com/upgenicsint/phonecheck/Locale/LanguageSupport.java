package com.upgenicsint.phonecheck.Locale;

import android.content.Context;

import java.util.Locale;

/**
 * Created by zohai on 2/9/2018.
 */

public class LanguageSupport {
    public static void changeLang(Context context, String lang) {
        Locale myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = myLocale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}
