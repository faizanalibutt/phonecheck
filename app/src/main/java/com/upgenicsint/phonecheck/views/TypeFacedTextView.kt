package com.upgenicsint.phonecheck.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

/**
 * Created by Farhan on 10/14/2016.
 */

class TypeFacedTextView : TextView {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onFinishInflate() {
        super.onFinishInflate()
        //setTypeface(Tools.getJunctionFontRegular(getContext()));
        //setLineSpacing(getContext().getResources().getDimensionPixelSize(R.dimen.text_line_space),1);
    }
}
