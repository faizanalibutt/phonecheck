package com.upgenicsint.phonecheck.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.upgenicsint.phonecheck.fragments.CosmeticsResultsFragment;
import com.upgenicsint.phonecheck.fragments.TestResultsFragment;
import com.upgenicsint.phonecheck.misc.Constants;

/**
 * Created by VNUM PC on 12/29/2017.
 */

public class ResultsPagerAdapter extends FragmentPagerAdapter {

    public ResultsPagerAdapter(FragmentManager fm) {
        super(fm);
    }
    // This determines the fragment for each tab
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new TestResultsFragment();
        } else if (position == 1) {
            return new CosmeticsResultsFragment();
        } else {
            return null;
        }
    }

    // This determines the number of tabs
    @Override
    public int getCount() {
        return 2;
    }

    // This determines the title for each tab
    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return Constants.TESTR;
            case 1:
                return Constants.COSMETICSR;
            default:
                return null;
        }
    }

}
