package com.upgenicsint.phonecheck.test

/**
 * Created by farhanahmed on 12/11/2016.
 */

class SubTest(val title: String) {
    // immutable variables
    var value = Test.INIT
        set(value) {
            isClear = true
            field = value
        }
    var isClear = true
        private set
    // mutable variables
    val isPass: Boolean
        get() = this.value == Test.PASS
    val isFail: Boolean
        get() = this.value == Test.COMPLETED

}
