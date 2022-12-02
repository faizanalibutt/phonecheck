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
import com.upgenicsint.phonecheck.adapter.TestPagerAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class TestsFragment extends Fragment {

    private ViewPager testPager;
    private TabLayout testTab;

    public TestsFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the test_item for this fragment
        View view = inflater.inflate(R.layout.fragment_test, container, false);
        initViews(view);
        bindViewPagerAdaptertoTab();
        setupTabLayout();
        return view;
    }

    private void initViews(View itemView) {
        testTab = itemView.findViewById(R.id.testTab);
        testPager = itemView.findViewById(R.id.testViewPager);
    }

    private void bindViewPagerAdaptertoTab() {
        TestPagerAdapter testPagerAdapter = new TestPagerAdapter(getChildFragmentManager());
        testPager.setAdapter(testPagerAdapter);
        testTab.setTabMode(TabLayout.MODE_SCROLLABLE);
        testPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        testTab.setupWithViewPager(testPager);
    }

}
