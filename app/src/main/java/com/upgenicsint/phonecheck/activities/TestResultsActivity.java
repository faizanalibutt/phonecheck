package com.upgenicsint.phonecheck.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.fragments.ResultsFragment;
import com.upgenicsint.phonecheck.fragments.TestFragment;
import com.upgenicsint.phonecheck.fragments.TestsFragment;
import com.upgenicsint.phonecheck.misc.Constants;

public class TestResultsActivity extends AppCompatActivity {

    Fragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_results);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String check = bundle.getString(Constants.DETAIL);
            if (check != null && check.equals(Constants.CHECK)) {
                replaceFragment(new ResultsFragment());
            } else {
                replaceFragment(new TestsFragment());
            }
        }

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 1000);*/
    }

    /**
     *
     * @param fragment is going to replaced when passed to it.
     */
    private void replaceFragment(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
}
