package com.upgenicsint.phonecheck.remote

class ColumnAPIUtils {
    companion object {
        val BASE_URL: String = "http://cloudportal.phonecheck.com"
        fun getColumnAPIService(): ColumnAPIService {
            return ColumnManager.getClient(BASE_URL)!!.create(ColumnAPIService::class.java)
        }
    }
}