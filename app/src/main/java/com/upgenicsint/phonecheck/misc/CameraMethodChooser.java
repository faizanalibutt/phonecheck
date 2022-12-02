package com.upgenicsint.phonecheck.misc;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by farhanahmed on 29/01/2017.
 */

public class CameraMethodChooser {

    private static String[] mCamera1Impl2Models = {
            //"SM-G930",
            //Test Devices
            //Samsung Galaxy Tab E 8.0
            //"T377",
            //"T375",
            //"T330",
            //"T555",
            //"T585",
            //"AGS2-L09",
            //galaxy s4
            "SCH-I545",
            "GT-I9505",
            "GT-I9515",
            "SGH-I337",
            "SPH-L720",
            "SGH-M919",
            "SCH-R970",
            //s5
            "SM-G900",
            "SM-G901",
            //"SM-G903",
            "SM-G870",
            //galaxy s3
            "SCH-I535",
            /*"GT-I9300",
            "GT-I9305",
            "SGH-T999",
            "SGH-I747",
            "SCH-R530",
            "SPH-L710",
            "SM-J320",*/
            //galaxy note 8
            "SM-N9508",
            "SM-N9500",
            //"SM-N950",
            "Pixel",
            //Motorola
            "XT1650",
            "XT1080",
            "Moto Z2 Play",
            "XT1710-02",
            "Nexus 6",
            "LG-M210",
            "SM-J320P",
            "SM-J320V",
            "SM-J500F",
//            "SM-J500M",
            "SM-J500FN",
//            "SM-J500H",
//            "SM-J5008",
//            "SM-J500N0"
            "HTC 10",
            "FIG-LX1",
            "WAS-LX1A",
            "ALE-CL00",
            "ALE-UL00",
            "STV100",
            "BLA-L09", "BLA-L29", "BLA-AL00",
            "SM-G360",
            "SM-A530F"
    };

    private static String[] mCamera2Models = {
            // Motorola nexus
//            "Nexus 6",
            "Nexus 5x"
    };


    public static int choose() {

        DeviceModelList list1 = new DeviceModelList();
        DeviceModelList list2 = new DeviceModelList();
        Collections.addAll(list1, mCamera1Impl2Models);
        Collections.addAll(list2, mCamera2Models);
        String model = Build.MODEL.toLowerCase();
        if (list1.contains(model)) {
            return 1;
        } else if (list2.contains(model)) {
            return 2;
        } else {
            return 0;
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