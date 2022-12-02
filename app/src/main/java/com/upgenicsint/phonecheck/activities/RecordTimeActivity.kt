package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.RecordTestAdapter
import com.upgenicsint.phonecheck.adapter.TestAdapter
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.misc.WriteObjectFile
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.models.TestModel
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.sensor.AccelerometerTest
import kotlinx.android.synthetic.main.activity_record_time.*
import org.json.JSONObject

@SuppressLint("SetTextI18n")
class RecordTimeActivity : AppCompatActivity() {

    private var testResults: RecyclerView? = null
    private var testModelList: MutableList<RecordTest>? = null
    private var testListAdapter: RecordTestAdapter? = null
    private var showErrorText: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_time)

        val totalTimeSpent = JSONObject()
        totalTimeSpent.put("Total Time", "${Loader.TOTAL_SCREEN_TIME}s")
        Loader.RESULT_TIME.put("Total Time Spent", totalTimeSpent)
        Loader.RESULT_TIME.put("Tests", Loader.RECORD_TESTS_TIME)
        WriteObjectFile.getInstance().writeObject(Loader.RESULT_TIME.toString(), "/time.json")

        initViews()
    }

    private fun initViews() {
        totalTime!!.text = "Total Time: ${Loader.TOTAL_SCREEN_TIME}s"
        testResults = findViewById(R.id.testResults)
        testResults!!.layoutManager = LinearLayoutManager(this@RecordTimeActivity)
        //val check = ReadTestJsonFile(errorText, testResults)
        /*val test = Loader.instance.getByClassType(AccelerometerTest::class.java)
        if (!AccelerometerActivity.recordUserTestCameToScreen && test != null && test.status != Test.INIT) {
            Loader.instance.recordList.removeAt(getSharedPreferences(getString(R.string.record_tests), Context.MODE_PRIVATE).getInt(getString(R.string.record_accel), -1))
        }*/
        testModelList = Loader.instance.recordList
        testListAdapter = RecordTestAdapter(testModelList, 0)
        testResults!!.adapter = testListAdapter
    }
}
