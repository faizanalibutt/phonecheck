package radonsoft.net.rta

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button

/**
 * Created by zohai on 3/20/2018.
 */
class TypeFacedButtonRta: Button {

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