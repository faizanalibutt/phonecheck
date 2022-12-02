package com.upgenicsint.phonecheck.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.models.TestModel;

import java.util.List;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.TestHolder> {

    private List<TestModel> items;

    private int layoutType;

    public TestAdapter(List<TestModel> items, int layoutType) {
        this.items = items;
        this.layoutType = layoutType;
    }

    @NonNull
    @Override
    public TestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        if (layoutType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_item,
                    parent, false);
        }
        if (layoutType == 1) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.results_item,
                    parent, false);
        }
        return new TestHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestHolder holder, int position) {
        TestModel test = items.get(position);
        holder.textView.setText(test.getTest());
    }

    @Override
    public int getItemCount() {
        if (items != null && items.size() != 0) {
            return items.size();
        } else
            return 0;
    }

    class TestHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        TestHolder(View itemView) {
            super(itemView);
            if (layoutType == 0) {
                textView = itemView.findViewById(R.id.item_test);
            }
            if (layoutType == 1) {
                textView = itemView.findViewById(R.id.item_results);
            }
        }
    }
}
