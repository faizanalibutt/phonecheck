package com.upgenicsint.phonecheck;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;

public class MictestSupportedModels {

    private static String[] micCheckSupportModels = {

            //s6
            "SM-G920",
            "SM-G920R7",
            "SM-G920x",
            "SM-G925x",
            "SM-G928x",
            "SM-G9200",
            "SM-G9208",
            "SM-G9209",
            "SM-G920A",
            "SM-G920F",
            "SM-G920FD",
            "SM-G920i",
            "SM-G920P",
            "SM-G920R4",
            "SM-G920S",
            "SM-G920T",
            "SM-G920V",
            "SM-G920W8",

            //s6 edge
            "SM-G925",
            "SM-G925S",
            "SM-G925A",
            "SM-G925F",
            "SM-G925FQ",
            "SM-G925i",
            "SM-G925P",
            "SM-G925R4",
            "SM-G925T",
            "SM-G925V",
            "SM-G925W8",

            //s6 edge plus
            "SM-G928",
            "SM-G9287",
            "SM-G928A",
            "SM-G928C",
            "SM-G928F",
            "SM-G928G",
            "SM-G928i",
            "SM-G928P",
            "SM-G928R4",
            "SM-G928T",
            "SM-G928V",

            //s7 edge
            "SM-G935F",
            "SM-G935FD",
            "SM-G935W8",
            "SM-G935S",
            "SM-G935K",
            "SM-G935L",
            "SM-G9350",
            "SM-G935V",
            "SM-G935A",
            "SM-G935P",
            "SM-G935T",
            "SM-G935U",
            "SM-G935R4",
            "SM-G935U",
            "SM-G930x",
            "SM-G935x",
            "SM-G930",
            "SM-G935",

            //s7
            "SM-G930F",
            "SM-G930FD",
            "SM-930W8",
            "SM-G930S",
            "SM-G930K",
            "SM-930L",
            "SM-G9300",
            "SM-G930V",
            "SM-G930AZ",
            "SM-G930P",
            "SM-G930T",
            "SM-930R4",
            "SM-G9308",
            "SM-G930U",
            "SM-G930A",

            //s8
            "SM-G950",
            "SM-G950W",
            "SM-G950V",
            "SM-G950U",
            "SM-G950T",
            "SM-G950R4",
            "SM-G950P",

            //s8+
            "SM-G955",
            "SM-G955FD",
            "SM-G955A",
            "SM-G955F",
            "SM-G955N",
            "SM-G955P",
            "SM-G955R4",
            "SM-G955T",
            "SM-G955U",
            "SM-G955V",
            "SM-G955W",

            //s9
            "SM-G960",
            "SM-G9600",
            "SM-G960F",
            "SM-G960F/DS",
            "SM-G960U",
            "SM-G960W",
            "SM-G9608",
            "SM-G960N",

            //s9+
            "SM-G965",
            "SM-G9650",
            "SM-G965F",
            "SM-G965F/DS",
            "SM-G965U",
            "SM-G965W",
            "SM-G9658",
            "SM-G965N",


            //galaxy note 4
            "SM-N910",
            "SM-N910W8",
            "SM-N910V",
            "SM-N910T",
            "SM-N910A",
            "SM-N910R4",
            "SM-N910P",

            //galaxy note 5
            "SM-N920",
            "SM-N9208",
            "SM-N920A",
            "SM-N920C",
            "SM-N920F",
            "SM-N920G",
            "SM-N920i",
            "SM-N920P",
            "SM-N920T",
            "SM-N920V",

            //galaxy note 7
            "SM-N930",
            "SM-N930F",
            "SM-N930FD",
            "SM-N930S",
            "SM-N930K",
            "SM-N930L",
            "SM-N9300",
            "SM-N930V",
            "SM-N930AZ",
            "SM-N930P",
            "SM-N930T",
            "SM-N930R4",
            "SM-N9308",
            "SM_N930U",
            "SM-N930A",
            "SM-N930W8",
            "SM-N935",
            "SM-N935F",
            "SM-N935F/DS",

            //galaxy note edge
            "SM-N915",
            "SM-N915T",
            "SM-N915S",
            "SM-N915A",

            //galaxy note 8
            "SM-N9508",
            "SM-N9500",
            "SM-N950FD",
            "SM-N950F",
            "SM-N950N",
            "SM-N950U",
            "SM-N950W",
            "SM-N950",

            //galaxy note 9
            "SM-N960U",

            //galaxy s10
            "SM-G973",
            "SM-G9730",
            "SM-G973F",
            "SM-G973U",
            "SM-SM-G973W",

            //galaxy s10e
            "SM-G970",
            "SM-G9700",
            "SM-G970F",
            "SM-G970U",
            "SM-G970W",

            //galaxy s10+
            "SM-G975",
            "SM-G9750",
            "SM-G975F",
            "SM-G975U",
            "SM-G975W"

    };

    public static String choose() {

        DeviceModelList list1 = new DeviceModelList();

        Collections.addAll(list1,micCheckSupportModels);

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
