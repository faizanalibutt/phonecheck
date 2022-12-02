package com.upgenicsint.phonecheck.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.models.Cosmetics;
import com.upgenicsint.phonecheck.models.CosmeticsKeys;
import com.upgenicsint.phonecheck.models.CosmeticsOptions;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsAdapter extends RecyclerView.Adapter<CosmeticsAdapter.MyViewHolder> {

    private List<CosmeticsKeys> cosmeticsList, cosmeticsList2;
    private List<CosmeticsOptions> dataModels;
    private ListAdapter adapter;
    private CosmeticsKeys cosmetics;
    Context context;
    private int ind = 0;

    public CosmeticsAdapter(List<CosmeticsKeys> cosmeticsList, Context context) {
        this.cosmeticsList = cosmeticsList;
        this.context = context;
    }
    public CosmeticsAdapter(Context context, List<CosmeticsOptions> cosmeticList, int a) {
        this.ind = a;
        this.dataModels = cosmeticList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cosmetics_list_layout, parent, false);
        dataModels= new ArrayList<>();
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

//        for (int i = 0; i < cosmeticsList.size(); i++) {
//            if (cosmeticsList.get(i).getPlatform().equalsIgnoreCase("android")) {
//                cosmetics = cosmeticsList.get(i);
//                dataModels = cosmeticsList.get(i).getCosmetics();
//            }
//        }
////        dataModels = cosmetics.getCosmetics();
//        holder.title.setText(cosmetics.getTitle());
//        adapter= new ListAdapter(dataModels, context, cosmeticsList);
//        ViewGroup.LayoutParams layoutParams = holder.ansList.getLayoutParams();
//        layoutParams.height = context.getResources().getDimensionPixelSize(R.dimen.row_height) * dataModels.size();
//        holder.ansList.setLayoutParams(layoutParams);
//        holder.ansList.setAdapter(adapter);

        CosmeticsKeys cosmetics = cosmeticsList.get(position);
        dataModels = cosmetics.getCosmetics();
        holder.title.setText(cosmetics.getTitle());
        adapter= new ListAdapter(dataModels, context, cosmeticsList, position);
        ViewGroup.LayoutParams layoutParams = holder.ansList.getLayoutParams();
        layoutParams.height = context.getResources().getDimensionPixelSize(R.dimen.row_height) * dataModels.size();
        holder.ansList.setLayoutParams(layoutParams);
        holder.ansList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return cosmeticsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public ListView ansList;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.titleCosmetics);
            ansList = view.findViewById(R.id.cosmeticsResultsView);
        }
    }
}
