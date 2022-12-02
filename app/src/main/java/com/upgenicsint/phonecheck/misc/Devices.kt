package com.upgenicsint.phonecheck.misc

import android.os.Build
import com.upgenicsint.phonecheck.containsIgnoreCase
import java.util.Arrays

/**
 * Created by farhanahmed on 04/12/2017.
 */

object Devices {

    private val noHeadPhoneJack = arrayOf (
            "HTC U11 Life NA ",
            "HTC U11 Life APAC ",
            "Huawei Mate 10 Lite JP RNE-L23 ",
            "HTC U11 Life NA ",
            "HTC X2 Android One JP X2-HT ",
            "Oppo F5 Youth  ",
            "Razer Phone RZ35-0215",
            "Xiaomi Mi Note 3 CN MCE8",
            "Huawei Mate 10 Lite RNE-L01 ",
            "Huawei Mate 10 Lite RNE-L21 ",
            "HTC U11+ CN ",
            "Huawei Mate 10 Pro BLA-A09 ",
            "Oppo F5 IN V1 ",
            "Xiaomi Mi 6 CN MCE16 ",
            "HTC U11 Life Global ",
            "Huawei Mate 10 Pro CN BLA-TL00 ",
            "HTC U11+ ",
            "Huawei Honor Changwan 7X CN BND-TL10",
            "Motorola Moto Z 2018 CN",
            "Huawei Mate 10 Pro BLA-L09 ",
            "HTC U11+ U11 Plus",
            "ZTE Z957 Grand X 4 LTE CA Z957A",
            "Huawei Mate 10 Porsche Design BLA-L29 ",
            "Huawei Mate 10 Pro BLA-L29 ",
            "Archos Diamond Omega Nubia Z17S EU",
            "ZTE Nubia Z17S NX595J ",
            "GiONEE S11s  ",
            "Huawei Honor Changwan 7X CN BND-AL10 Honor V10",
            "Bluboo S1 LTE",
            "Huawei Mate 10 Pro CN BLA-AL00 ",
            "Xiaomi Mi Mix 2 Black Ceramic Edition Global MDE5",
            "Motorola Moto Z2 Force Edition XT1789-06",
            "Xiaomi Mi Mix 2 Black Ceramic Edition Global MDE5",
            "Huawei G10 CN RNE-AL00 G10 Plus Mate 10 Lite ",
            "Xiaomi Mi Note 3 CN MCE8",
            "Xiaomi Mi Note 3 CN MCE8",
            "Xiaomi Mi Mix 2 Black Ceramic Edition Global MDE5",
            "Xiaomi Mi Mix 2 Exclusive Ceramic Edition Global MDE5",
            "Google Pixel 2",
            "Google Pixel 2 XL",
            "ZTE Nubia Z17 NX563H ",
            "HTC U11 Life Global ",
            "Yota Phone 3 Yota3 ",
            "Sharp AQUOS S2 LTE FS8010",
            "Yota Phone 3 Yota3 ",
            "Bluboo S8 LTE",
            "Xiaomi Mi 6 Mercury Silver Limited Edition CN MCE16 ",
            "Motorola Moto Z2 Force Edition XT1789-02",
            "Moto Z2 Force XT1789",
            "Motorola Moto Z2 Force XT1789",
            "XT1789",
            "Motorola Moto Z2 Force Edition Global XT1789-05",
            "Motorola Moto Z2 Force Edition XT1789-03",
            "Motorola Moto Z2 Force Edition XT1789-04",
            "HTC U11 NA U-3f ",
            "Motorola Moto Z2 Force Edition XT1789-01 Moto Z Force 2nd gen",
            "Sharp Aquos R 605SH ",
            "Sharp Aquos R SH-03J ",
            "Xiaomi Mi6 Ceramic Gold Edition TW ",
            "Xiaomi Mi 6 TW ",
            "Essential Phone PH-1  ",
            "HTC U11 CN U-3w ",
            "HTC U11 CN U-3w ",
            "ZTE Nubia Z17 NX563H ",
            "ZTE Nubia Z17 NX563J ",
            "Xiaomi Mi 6 CN MCT1 ",
            "HTC U11 601HT ",
            "Sprint HTC U11 ",
            "HTC U11 WiMAX 2+ HTV33 ",
            "HTC U11 NA U-3f ",
            "HTC U11 U-3u ",
            "GiONEE Elife F100SD  ",
            "GiONEE Elife S10  ",
            "ZTE Nubia Z17 NX563J ",
            "GiONEE Elife S10C S10CL",
            "GiONEE M6S Plus ",
            "Sharp AQUOS R WiMAX 2+ SHV39 ",
            "Xiaomi Mi 6 CN MCE16 ",
            "Xiaomi Mi6 Ceramic Gold Edition CN MCE16 ",
            "HTC U11 U-3u ",
            "LeEco Le Pro 3 AI Edition CN ",
            "LeEco X656 Le Pro 3 AI Edition CN X658 X659",
            "GiONEE M6S Plus ",
            "LeEco X850 Le Max 3 CN ",
            "Xiaomi Mi 6 CN MCE16 ",
            "LeEco Le Pro3 Elite Edition CN",
            "HTC U Ultra CN U-1w ",
            "HTC U Ultra CN U-1w ",
            "HTC U Play ",
            "HTC U Ultra ",
            "HTC U Ultra U-1u ",
            "HTC U Play ",
            "HTC U Ultra U-1u ",
            "GiONEE M2017 ",
            "ZTE Z956 Grand X 4 LTE US",
            "GiONEE M2017 ",
            "HTC 10 Evo M10f ",
            "LeEco X726 Le Pro3 CN ",
            "LeEco X726 Le Pro3 CN ",
            "LeEco Le Pro3 AM",
            "HTC Bolt ",
            "LeEco X652 Le3 LEX652",
            "LeEco X522 Le S3 ",
            "Motorola Moto Z XT1650-03 ",
            "LeEco X720 Le Pro3",
            "LeEco X720 Le Pro3 ",
            "LeEco X720 Le Pro3 ",
            "LeEco X720 Le Pro3 LEX720 Le 3 Pro",
            "Motorola Moto Z CN XT1650-05 ",
            "GiONEE GN5003 M6 Mini Dajingang",
            "GiONEE GN8003 M6 ",
            "GiONEE GN8003 M6 ",
            "GiONEE GN8002 M6 Plus ",
            "GiONEE GN8002 M6 Plus ",
            "Motorola Moto Z Droid Edition XT1650-01 ",
            "Motorola Moto Z Droid Edition XT1650-01 ",
            "LeEco X502 LTE ",
            "LeEco X820 Le Max 2 X821 X822",
            "LeEco X820 Le Max 2 Pro ",
            "LeEco X620 Le2 ",
            "LeEco X621 Le2 ",
            "LeEco X520 Le 2 X521 X525 X526 X527 X528 X529",
            "Samsung SM-W2017 Galaxy Golden 4 ",
            "LeEco X620 Le2 ",
            "Motorola Moto Z XT1650-03 ",
            "Motorola Moto Z Force Droid Edition XT1650-02 ",
            "Moto Z (2)",
            "LeEco X625 Le2 Pro X620",
            "Sony XZ2",
            "EML-AL00", "EML-L09C", "EML-L29C",
            "LYA-L09",
            "CLT-AL00", "CLT-AL01", "CLT-L09", "CLT-L29", "CLT-TL01",
            "A6013", "OnePlus 6T",
            "Google Pixel 3", "Google Pixel 3 XL", "Google Pixel XL3"
    )

    private val motoZ2ForceList = arrayOf (
            "Motorola Moto Z2 Force Edition XT1789-01",
            "Motorola Moto Z2 Force Edition XT1789-02",
            "Motorola Moto Z2 Force Edition XT1789-03",
            "Motorola Moto Z2 Force Edition XT1789-04",
            "Motorola Moto Z2 Force Edition XT1789-05",
            "Motorola Moto Z2 Force Edition XT1789-07",
            "Moto Z (2)")

    private val swipeAutomation = arrayOf(
            "SM-G950",
            "SM-G955",
            "SM-N950",
            "SM-N960",
            "SM-G960",
            "SM-G965"
    )

    private val simpleSwieAutomation = arrayOf (
            "SM-N910",
            "SM-N915",
            "SM-G900",
            "SM-G950",
            "SM-G955",
            "SM-N950",
            "SM-N960",
            "SM-G960",
            "SM-G965"
    )

    private val s10Array = arrayOf(
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
    )

    fun isS10Available(): Boolean {
        for (s in s10Array) {
            if (Build.MODEL.containsIgnoreCase(s)) {
                return true
            }
        }
        return false
    }

    fun hasProximity(): Boolean {
        if (Build.MODEL.containsIgnoreCase("QMV7B")) {
            return false
        }
        if (Build.MODEL.containsIgnoreCase("QMV7A")) {
            return false
        }
        if (Build.MODEL.containsIgnoreCase("Moto C Plus")) {
            return false
        }
        return true
    }

    fun hasHeadPhoneJack(): Boolean {

        for (s in noHeadPhoneJack) {
            if (Build.MODEL.containsIgnoreCase(s)) {
                return false
            }
        }

        return true
    }

    fun isMicrophoneSensitive(): Boolean {
        if (Build.MODEL.containsIgnoreCase("SM-N900V") || Build.MODEL.containsIgnoreCase("SM-N915T") || Build.MODEL.containsIgnoreCase("SM-G920T")) {
            return true
        }
        return false
    }

    fun isTwoTouchSensor(): Boolean {
        if (Build.MODEL.containsIgnoreCase("SM-G360")) {
            return true
        }
        return false
    }

    fun chooseThreshold(): Boolean {
        for (s in motoZ2ForceList) {
            if (Build.MODEL.containsIgnoreCase(s)) {
                return true
            }
        }
        return false
    }

    fun isHomePressed(): Boolean {
        if (Build.MODEL.containsIgnoreCase("SM-G920R7") && Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            return true
        }
        return false
    }

    fun isSwipeAutomationAvailable(): Boolean {
        for (s : String in swipeAutomation) {
            if (Build.MODEL.containsIgnoreCase(s)) {
                return true
            }
        }
        return false
    }

    fun isSimpleSwipeAvailable(): Boolean {
        for (s : String in simpleSwieAutomation) {
            if (Build.MODEL.containsIgnoreCase(s)) {
                return true
            }
        }
        return false
    }

}
