package com.upgenicsint.phonecheck.activities;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.adapter.AmplitudeAdapter;
import com.upgenicsint.phonecheck.misc.Constants;
import com.upgenicsint.phonecheck.models.Amplitude;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class AmplitudeResultsDetail extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amplitude_results_detail);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.resultsPref), MODE_PRIVATE);
        List<Amplitude> results = null;
        Gson gson = new Gson();

        if (sharedPreferences != null && sharedPreferences.getString(getString(R.string.micresultsList), null)
                != null && getIntent().getExtras().getBoolean(Constants.LS_MIC, false)) {
            String resultsS = sharedPreferences.getString(getString(R.string.micresultsList), "");
            results = gson.fromJson(resultsS, new TypeToken<List<Amplitude>>(){}.getType());
        }
        if (sharedPreferences != null && sharedPreferences.getString(getString(R.string.micesresultsList), null)
                != null && getIntent().getExtras().getBoolean(Constants.ES_VID_MIC, false)) {
            String resultsS = sharedPreferences.getString(getString(R.string.micesresultsList), "");
            results = gson.fromJson(resultsS, new TypeToken<List<Amplitude>>(){}.getType());
        }
        if (sharedPreferences != null && sharedPreferences.getString(getString(R.string.autovibratorresultsList), null)
                != null && getIntent().getExtras().getBoolean(Constants.AUTO_VIBRATOR, false)) {
            String resultsS = sharedPreferences.getString(getString(R.string.autovibratorresultsList), "");
            resultsS = resultsS.replace("[", "");
            resultsS = resultsS.replace("]", "");
            //results = Arrays.asList(resultsS.split(","));
        }
        listView = findViewById(R.id.results);
        if (results != null && results.size() > 0) {
            AmplitudeAdapter adapter = new AmplitudeAdapter(this, results);
            listView.setAdapter(adapter);
        }
    }
}
