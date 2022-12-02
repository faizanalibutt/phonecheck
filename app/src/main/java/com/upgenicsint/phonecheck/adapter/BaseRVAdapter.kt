package com.upgenicsint.phonecheck.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView

/**
 * Created by Farhan on 10/15/2016.
 */

abstract class BaseRVAdapter<VH : RecyclerView.ViewHolder>(val context: Context) : RecyclerView.Adapter<VH>() {
}
