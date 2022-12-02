package com.upgenicsint.phonecheck.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.activities.CosmeticsTestActivity;
import com.upgenicsint.phonecheck.activities.DeviceTestableActivity;
import com.upgenicsint.phonecheck.models.Cosmetics;
import com.upgenicsint.phonecheck.models.CosmeticsKeys;
import com.upgenicsint.phonecheck.models.CosmeticsOptions;
import com.upgenicsint.phonecheck.models.CosmeticsResults;
import com.upgenicsint.phonecheck.models.ListSelector;
import com.upgenicsint.phonecheck.test.Test;
import com.upgenicsint.phonecheck.test.misc.CosmeticsTest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class ListAdapter extends ArrayAdapter<CosmeticsOptions> {
    private List<CosmeticsOptions> dataSet;
    private List<CosmeticsKeys> cosmeticsKeys;
    Context mContext;
    int childPosition;
    View mLastView = null;
    ListSelector listSelector = new ListSelector();
    private SharedPreferences sharedPreferences;
    int sposition;

    private static class ViewHolder {
        TextView optionsAns;
    }

    public ListAdapter(List<CosmeticsOptions> data, Context context, List<CosmeticsKeys> cosmeticsKeys, int position) {
        super(context, R.layout.cosmetics_options, data);
        this.dataSet = data;
        this.mContext = context;
        this.cosmeticsKeys = cosmeticsKeys;
        this.childPosition = position;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.cosmetics_key),
                Context.MODE_PRIVATE);
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public CosmeticsOptions getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

//    @Override
//    public int getViewTypeCount() {
//        return 3;
//    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        final CosmeticsOptions dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        final ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {

            viewHolder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.cosmetics_options, parent, false);
            viewHolder.optionsAns = convertView.findViewById(R.id.options);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.optionsAns.setText(dataModel.getResponse());
        String jsonText = sharedPreferences.getString(cosmeticsKeys.get(childPosition).getShortkey(), null);

        if (jsonText != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonText);
                boolean option = jsonObject.getBoolean("option");
                int pos = jsonObject.getInt("position");

                if (pos == position) {
                    viewHolder.optionsAns.setBackgroundColor(option ? Color.parseColor("#DCEDC8") :
                            Color.parseColor("#FFCDD2"));
                } else {
                    viewHolder.optionsAns.setBackgroundColor(Color.parseColor("#FAFAFA"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            viewHolder.optionsAns.setBackgroundColor(Color.parseColor("#FAFAFA"));
        }

        viewHolder.optionsAns.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View v) {
                boolean res = dataModel.getResult();
                SharedPreferences.Editor editor = sharedPreferences.edit();

                if (mLastView != null)
                    deselect(mLastView);
                select(v);
                mLastView = v;

                boolean option;
                if (res) {
                    Loader.Companion.getInstance().getByClassType(CosmeticsTest.class).sub(cosmeticsKeys.get(childPosition).getShortkey()).setValue(Test.PASS);
                    option = true;
                } else {
                    option = false;
                    Loader.Companion.getInstance().getByClassType(CosmeticsTest.class).sub(cosmeticsKeys.get(childPosition).getShortkey()).setValue(Test.FAILED);
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("position", position);
                    jsonObject.put("option", option);
                    editor.putString(cosmeticsKeys.get(childPosition).getShortkey(), jsonObject.toString());
                    editor.commit();
                    //
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                CosmeticsTestActivity cosmeticsTestActivity = new CosmeticsTestActivity();
                cosmeticsTestActivity.setTestStatus();
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    private void select(View v) {
        v.setBackgroundColor(Color.parseColor("#DCEDC8"));
    }

    private void deselect(View v) {
        v.setBackgroundColor(Color.parseColor("#FAFAFA"));
    }

}
