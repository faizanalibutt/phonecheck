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
public class TestCustomizationsFragment extends Fragment {

    private RecyclerView testCustomizations;
    private List<TestModel> testModelList;
    private TestAdapter testListAdapter;
    private TextView showErrorText;

    public TestCustomizationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_test_customizations, container, false);
        initViews(view);
        initListeners();
        return view;

    }

    private void initViews(View itemView) {
        testCustomizations = itemView.findViewById(R.id.testCustom);
        showErrorText = itemView.findViewById(R.id.errorText);
        testCustomizations.setLayoutManager(new LinearLayoutManager(getActivity()));
        ReadTestJsonFile check = new ReadTestJsonFile(showErrorText, testCustomizations);
        testModelList = check.getCustomizationList();
        testListAdapter = new TestAdapter(testModelList, 0);
        testCustomizations.setAdapter(testListAdapter);
    }

    private void initListeners() {

    }

}
