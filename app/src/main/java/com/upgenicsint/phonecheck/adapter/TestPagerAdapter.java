package com.upgenicsint.phonecheck.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.upgenicsint.phonecheck.fragments.AndroidDeviceConfigurtionListFragment;
import com.upgenicsint.phonecheck.fragments.BatteryAPIListFragment;
import com.upgenicsint.phonecheck.fragments.TestCustomizationsFragment;
import com.upgenicsint.phonecheck.fragments.TestListFragment;
import com.upgenicsint.phonecheck.misc.Constants;

/**
 * Created by VNUM PC on 12/29/2017.
 */

public class TestPagerAdapter extends FragmentPagerAdapter {

    public TestPagerAdapter(FragmentManager fm) {
        super(fm);
    }
    // This determines the fragment for each tab
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new TestListFragment();
        } else if (position == 1) {
            return new TestCustomizationsFragment();
        } else if (position == 2) {
            return new AndroidDeviceConfigurtionListFragment();
        } else if (position == 3) {
            return new BatteryAPIListFragment();
        } else {
            return null;
        }
        /*else if (position == ) {
            return new
        } */
    }

    // This determines the number of tabs
    @Override
    public int getCount() {
        return 4;
    }

    // This determines the title for each tab
    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return Constants.TEST;
            case 1:
                return Constants.CUSTOM;
            case 2:
                return Constants.ANDROID_DEVICE_CONFIG;
            case 3:
                return Constants.BATTERY_API;
            default:
                return null;
        }
    }

}
