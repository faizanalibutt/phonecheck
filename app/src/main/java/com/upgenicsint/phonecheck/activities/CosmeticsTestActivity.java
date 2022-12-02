package com.upgenicsint.phonecheck.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.adapter.CosmeticsAdapter;
import com.upgenicsint.phonecheck.misc.WriteObjectFile;
import com.upgenicsint.phonecheck.models.CosmeticsKeys;
import com.upgenicsint.phonecheck.models.CosmeticsOptions;
import com.upgenicsint.phonecheck.models.RecordTest;
import com.upgenicsint.phonecheck.test.misc.CosmeticsTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;

public class CosmeticsTestActivity extends DeviceTestableActivity<CosmeticsTest> {
    private List<CosmeticsKeys> cosmeticList = new ArrayList<>();
    private List<CosmeticsKeys> cosmeticList2;
    private RecyclerView recyclerView;
    private CosmeticsAdapter mAdapter;
    private TextView cosmeticError;
    public static final int REQ = 786;
    CosmeticsTest test;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cosmetics_test);
        onCreateNav();
        setNavTitle("Cosmetics Test");

        Loader.TIME_VALUE = 0;
        COSMETICS_SCREEN_TIME = 0;
        Loader.RECORD_TIMER_TASK = new TimerTask() {

            @Override
            public void run() {
                Loader.RECORD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        Loader.TIME_VALUE++;
                    }
                });
            }
        };
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000);

        this.setTest(Loader.Companion.getInstance().getByClassType(CosmeticsTest.class));
        if (Loader.getInstance().loadCosmetics() != null) {
            cosmeticList = Loader.getInstance().loadCosmetics().getCosmetics();
        }
        List<CosmeticsKeys> filtered = new ArrayList<>();
        if (cosmeticList != null) {
            for (int i = 0; i < cosmeticList.size(); i++) {
                if (cosmeticList.get(i).getPlatform().equalsIgnoreCase("Android") || cosmeticList.get(i).getPlatform().equalsIgnoreCase("All")) {
                    List<CosmeticsOptions> cosmeticsOptionsList = cosmeticList.get(i).getCosmetics();

                    List<CosmeticsOptions> yes = null;
                    List<CosmeticsOptions> no = null;
                    List<CosmeticsOptions> other = new ArrayList<>();

                    for (int j = 0; j < cosmeticsOptionsList.size(); j++) {
                        CosmeticsOptions cosmeticsOptions = cosmeticsOptionsList.get(j);
                        switch (cosmeticsOptions.getResponse().toLowerCase()) {
                            case "yes":
                                yes = new ArrayList<>();
                                yes.add(cosmeticsOptions);
                                break;
                            case "no":
                                no = new ArrayList<>();
                                no.add(cosmeticsOptions);
                                break;
                            default:
                                other.add(cosmeticsOptions);
                                break;
                        }
                    }

                    cosmeticsOptionsList.clear();

                    if (yes != null) {
                        cosmeticsOptionsList.addAll(yes);
                    }
                    if (no != null) {
                        cosmeticsOptionsList.addAll(no);
                    }
                    if (other.size() != 0) {
                        cosmeticsOptionsList.addAll(other);
                    }
                    filtered.add(cosmeticList.get(i));
                }
            }
        }
        cosmeticList = filtered;

        recyclerView = findViewById(R.id.recyclerView);
        cosmeticError = findViewById(R.id.cosmeticsError);
//        mAdapter = new CosmeticsAdapter(Objects.requireNonNull(Loader.getInstance().loadCosmetics()).cosmetics, getActivity());
        if (cosmeticList.size() == 0) {
            cosmeticError.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        mAdapter = new CosmeticsAdapter(cosmeticList, getActivity());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
//        recyclerView.getRecycledViewPool().setMaxRecycledViews(TYPE_CAROUSEL, 0);
        mAdapter.notifyDataSetChanged();
        cosmeticError.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

    }

    public void setTestStatus() {
//        CosmeticsTest test = (CosmeticsTest)CosmeticsTestActivity.this.getTest();
        test = CosmeticsTestActivity.this.getTest();
        if (test != null) {
            finalizeTest();
        }

    }

    @Override
    protected void finalizeTest() {
        super.finalizeTest();
        closeTimerTest();
    }

    public static int COSMETICS_SCREEN_TIME = 0;
    @Override
    protected void closeTimerTest() throws IllegalArgumentException {
        super.closeTimerTest();
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK.cancel();
                Loader.RECORD_TIMER_TASK = null;
                COSMETICS_SCREEN_TIME = Loader.TIME_VALUE;
                try {
                    SharedPreferences recordPrefs = getSharedPreferences(getResources().getString(R.string.record_tests), Context.MODE_PRIVATE);
                    Loader.getInstance().getRecordList().set(recordPrefs.getInt(getString(R.string.record_cosmetics), -1), new RecordTest(getString(R.string.report_cosmetics_test), COSMETICS_SCREEN_TIME));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Loader.RECORD_TESTS_TIME.put("Cosmetics" , COSMETICS_SCREEN_TIME + "s");
                Loader.TIME_VALUE = 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
