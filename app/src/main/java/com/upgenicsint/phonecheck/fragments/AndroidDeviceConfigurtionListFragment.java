package com.upgenicsint.phonecheck.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.adapter.TestAdapter;
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile;
import com.upgenicsint.phonecheck.models.TestModel;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class AndroidDeviceConfigurtionListFragment extends Fragment {

    private RecyclerView testResults;
    private List<TestModel> testModelList;
    private TestAdapter testListAdapter;
    private TextView showErrorText;

    public AndroidDeviceConfigurtionListFragment() {

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the test_item for this fragment
        View view = inflater.inflate(R.layout.fragment_test_results, container, false);
        initViews(view);
        initListeners();
        return  view;
    }

    private void initViews(View itemView) {
        testResults = itemView.findViewById(R.id.testResults);
        showErrorText = itemView.findViewById(R.id.errorText);
        testResults.setLayoutManager(new LinearLayoutManager(getActivity()));
        ReadTestJsonFile check = new ReadTestJsonFile(showErrorText, testResults);
        testModelList = check.getAllFilesOfNoObjName("AndroidDeviceConfiguration.json");
        testListAdapter = new TestAdapter(testModelList, 0);
        testResults.setAdapter(testListAdapter);
    }

    private void initListeners() {

    }

}
