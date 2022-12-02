package com.androidhiddencamera;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zohai on 1/23/2018.
 */

public class ExposureSupportedModels {
    private static String[] speechRecogSupportModels = {

            //s8
            "SM-G950W",
            "SM-G950V",
            "SM-G950U",
            "SM-G950T",
            "SM-G950R4",
            "SM-G950P",

            //s8+
            "SM-G955FD",
            "SM-G955A",
            "SM-G955F",
            "SM-G955N",
            "SM-G955P",
            "SM-G955R4",
            "SM-G955T",
            "SM-G955U",
            "SM-G955V",
            "SM-G955W"

    };

    public static String choose() {

        DeviceModelList list1 = new DeviceModelList();

        Collections.addAll(list1,speechRecogSupportModels);

        String model = Build.MODEL.toLowerCase();
        if (list1.contains(model)) {
            return model;
        }
        else{
            return "Null";
        }
    }

    private static class DeviceModelList extends ArrayList<String> {
        @Override
        public boolean contains(Object o) {
            if (o instanceof String) {
                String model = String.valueOf(o);
                for (String item : this) {
                    String modelFromList = item.toLowerCase();
                    if (model.startsWith(modelFromList) || model.contains(modelFromList)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
