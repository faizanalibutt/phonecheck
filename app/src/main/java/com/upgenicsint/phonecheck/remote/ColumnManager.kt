package com.upgenicsint.phonecheck.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ColumnManager {

    companion object {
        var retrofit: Retrofit? = null
        fun getClient(baseUrl: String): Retrofit? {
            retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            return retrofit
        }
    }
}