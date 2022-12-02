package com.upgenicsint.phonecheck.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.adapter.ResultsPagerAdapter;


/**
 * A simple {@link Fragment} subclass.
 */
public class ResultsFragment extends Fragment {

    private ViewPager resultsPager;
    private TabLayout resultsTab;

    public ResultsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the results_item for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        initViews(view);
        bindViewPagerAdaptertoTab();
        setupTabLayout();
        return view;
    }

    private void initViews(View itemView) {
        resultsTab = itemView.findViewById(R.id.resultsTab);
        resultsPager = itemView.findViewById(R.id.resultsViewPager);
    }

    private void bindViewPagerAdaptertoTab() {

        resultsPager.setAdapter(new ResultsPagerAdapter(getChildFragmentManager()));
        resultsPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupTabLayout() {
        resultsTab.setupWithViewPager(resultsPager);
    }

}
