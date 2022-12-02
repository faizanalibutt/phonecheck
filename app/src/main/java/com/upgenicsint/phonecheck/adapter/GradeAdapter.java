package com.upgenicsint.phonecheck.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.activities.GradingsActivity;
import com.upgenicsint.phonecheck.misc.WriteObjectFile;
import com.upgenicsint.phonecheck.models.GradeChild;

import java.util.List;

public class GradeAdapter extends RecyclerView.Adapter<GradeAdapter.MyViewHolder> {

    private List<GradeChild> dataSet;
    private Context mContext;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(GradeChild item, MyViewHolder viewHolder);
    }

    public GradeAdapter(List<GradeChild> data, Context context, OnItemClickListener listener) {
        this.dataSet = data;
        this.mContext = context;
        this.listener = listener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView grades;

        MyViewHolder(View itemView) {
            super(itemView);
            grades = itemView.findViewById(R.id.grades);
        }

        void bind(final GradeChild item, final OnItemClickListener listener, final MyViewHolder viewHolder) {
            grades.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.getSharedPreferences(mContext.getString(R.string.save_grade), Context.MODE_PRIVATE).edit().clear().apply();
                    if (GradingsActivity.Companion.getGradingResults().exists()) {GradingsActivity.Companion.getGradingResults().delete();}
                    listener.onItemClick(item, viewHolder);
                    WriteObjectFile.getInstance().writeObject(item.getGrade(), "/GradeResults.json");
                }
            });
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grades, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder, int position) {
        final GradeChild dataModel = dataSet.get(position);
        assert dataModel != null;
        viewHolder.grades.setText(dataModel.getGrade());
        if (dataModel.getGradeState()) {
            viewHolder.grades.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gradebg));
            viewHolder.grades.setTextColor(ContextCompat.getColor(mContext, R.color.white_color));
        } else {
            viewHolder.grades.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white_color));
            viewHolder.grades.setTextColor(ContextCompat.getColor(mContext, R.color.dark_black));
        }
        viewHolder.bind(dataModel, listener, viewHolder);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

}
