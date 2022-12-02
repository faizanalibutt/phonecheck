package com.upgenicsint.phonecheck.models

/**
 * Created by Farhan on 10/18/2016.
 */

class TestStatusInfo {
    var title: String
    var status: Int = 0
    var statusText: String? = null

    constructor(title: String, status: Int) {
        this.title = title
        this.status = status
    }

    constructor(title: String, statusText: String) {
        this.title = title
        this.statusText = statusText
    }
}
