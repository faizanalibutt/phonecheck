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
public class GradeListFragment extends Fragment {

    private RecyclerView testList;
    private TestAdapter testListAdapter;
    private List<TestModel> objectsTests;
    private TextView showErrorText;

    public GradeListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_test_list, container, false);
        initViews(view);
        initListeners();
        return view;
    }

    private void initViews(View itemView) {
        testList = itemView.findViewById(R.id.testList);
        showErrorText = itemView.findViewById(R.id.errorText);
        testList.setLayoutManager(new LinearLayoutManager(getActivity()));
        ReadTestJsonFile testLists = new ReadTestJsonFile(showErrorText, testList);
        objectsTests = testLists.getTestsList();
        if (objectsTests != null && objectsTests.size() > 0) {
            testListAdapter = new TestAdapter(objectsTests, 0);
            testList.setAdapter(testListAdapter);
        }
    }

    private void initListeners() {

    }

}
