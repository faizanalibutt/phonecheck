package com.upgenicsint.phonecheck.remote

import com.upgenicsint.phonecheck.models.Column
import com.upgenicsint.phonecheck.models.TestResults
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ColumnAPIService {

    @POST("/cloud/CloudDbSync/updateByColumn")
    fun pushColumn(@Body columnBody: Column) : Call<String>
    @POST("/cloud/CloudDbSync/updateByColumn")
    fun pushTestResults(@Body columnBody: TestResults) : Call<String>

}