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


public class CosmeticsResultsFragment extends Fragment {

    private RecyclerView cosmeticsResults;
    private List<TestModel> testModelList;
    private TestAdapter testListAdapter;
    private TextView showErrorText;

    public CosmeticsResultsFragment() {

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cosmetics_results, container, false);
        initViews(view);
        initListeners();
        return view;
    }

    private void initViews(View itemView) {
        cosmeticsResults = itemView.findViewById(R.id.cosmeticsResults);
        showErrorText = itemView.findViewById(R.id.errorText);
        cosmeticsResults.setLayoutManager(new LinearLayoutManager(getActivity()));
        ReadTestJsonFile check = new ReadTestJsonFile(showErrorText, cosmeticsResults);
        testModelList = check.getCosmeticsResults();
        testListAdapter = new TestAdapter(testModelList, 1);
        cosmeticsResults.setAdapter(testListAdapter);
    }

    private void initListeners() {

    }
}
