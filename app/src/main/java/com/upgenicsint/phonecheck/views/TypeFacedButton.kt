package com.upgenicsint.phonecheck.views

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button

/**
 * Created by Farhan on 10/14/2016.
 */

class TypeFacedButton : Button {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    override fun onFinishInflate() {
        super.onFinishInflate()
        setAllCaps(false)
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        //setTypeface(Tools.getJunctionFontRegular(getContext()));
    }
}
