package com.upgenicsint.phonecheck.models

/**
 * Created by farhanahmed on 11/09/2017.
 */

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ClientCustomization {

    @SerializedName("Beep")
    @Expose
    var beep = false
    @SerializedName("FullScreenBubbleTest")
    @Expose
    var fullScreenBubbleTest = false
    @SerializedName("LCD_ColorBlack")
    @Expose
    var lCDColorBlack = false
    @SerializedName("LCD_ColorBlue")
    @Expose
    var lCDColorBlue = false
    @SerializedName("LCD_ColorGreen")
    @Expose
    var lCDColorGreen = false
    @SerializedName("LCD_ColorRed")
    @Expose
    var lCDColorRed = false
    @SerializedName("LCD_ColorWhite")
    @Expose
    var lCDColorWhite = false
    @SerializedName("NumberToDial")
    @Expose
    var numberToDial: String? = null
    @SerializedName("UserName")
    @Expose
    var userName: String? = null
    @SerializedName("id")
    @Expose
    var id: Int = 0
    @Expose
    @SerializedName("BatteryDrainDuration")
    var duration: Int = 0
    @Expose
    @SerializedName("AutoStartBatteryTest")
    var isAutoBatteryDrain = false
    @Expose
    @SerializedName("showComments")
    var isCommentsAdded = false
    @SerializedName("LCD_ColorGray")
    @Expose
    var LCDColorGray = false
    @SerializedName("DigitizerBoost")
    @Expose
    var DigiTizerBoost = false
    @Expose
    @SerializedName("AutoStartBatteryOnConnection")
    var isAutoStartBatteryDrain = false
    @Expose
    @SerializedName("Guided_Digi")
    var GuidedDigi = false
}
