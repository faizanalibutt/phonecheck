package com.upgenicsint.phonecheck.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.models.Amplitude;

import java.util.List;

public class AmplitudeAdapter extends ArrayAdapter<Amplitude> {

    public AmplitudeAdapter(Context context, List<Amplitude> amplitudes) {
       super(context, 0, amplitudes);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
       // Get the data item for this position
       Amplitude amplitude = getItem(position);
       // Check if an existing view is being reused, otherwise inflate the view
       if (convertView == null) {
          convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_amplitude, parent, false);
       }
       // Lookup view for data population
       TextView amplitudeText = convertView.findViewById(R.id.amplitude);
       // Populate the data into the template view using the data object
       if (amplitude != null) {
           amplitudeText.setText(String.valueOf(amplitude.getAmplitude()));
           if (amplitude.getGreen()) {
               amplitudeText.setBackgroundResource(android.R.color.holo_green_light);
           } else {
               amplitudeText.setBackgroundResource(android.R.color.white);
           }
       }
       // Return the completed view to render on screen
       return convertView;
   }
}