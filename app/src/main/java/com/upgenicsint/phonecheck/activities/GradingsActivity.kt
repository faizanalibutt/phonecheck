package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.GradeAdapter
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.models.Grade
import com.upgenicsint.phonecheck.models.GradeChild
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.misc.GradesTest
import kotlinx.android.synthetic.main.activity_gradings.*
import java.io.File
import java.util.*

class GradingsActivity : DeviceTestableActivity<GradesTest>() {

    private var grade: Grade? = null
    private lateinit var gradeList: MutableList<GradeChild>
    private lateinit var additionalList: MutableList<GradeChild>
    private var gradeAdapter1: GradeAdapter? = null
    private var gradeAdapter2: GradeAdapter? = null
    private var saveGrades: SharedPreferences.Editor? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gradings)

        Loader.TIME_VALUE = 0
        GRADES_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        onCreateNav()
        setNavTitle("Grading Screen")
        Logy.setEnable(BuildConfig.DEBUG)
        test = Loader.instance.getByClassType(GradesTest::class.java)
        /***
         * read json file
         */
        readGradeJsonFile()
    }

    @SuppressLint("CommitPrefEdits", "NewApi")
    private fun readGradeJsonFile() {
        val readTestJsonFile = ReadTestJsonFile()
        grade = readTestJsonFile.grades
        if (grade != null && grade!!.grade.size > 0) {
            grades.visibility = View.VISIBLE
            additional.visibility = View.VISIBLE
            noGrades.visibility = View.INVISIBLE
            /***
             * Grades List
             */
            gradeList = grade!!.grade
            gradeAdapter1 = GradeAdapter(gradeList, this, GradeAdapter.OnItemClickListener { item, viewHolder ->
                viewHolder.grades.setBackgroundColor(ContextCompat.getColor(this, R.color.gradebg))
                viewHolder.grades.setTextColor(ContextCompat.getColor(this, R.color.white_color))
                saveGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE).edit()
                saveGrades!!.putInt(getString(R.string.grade_position), item.gradePosition)
                saveGrades!!.putBoolean(getString(R.string.grade_selected), true)
                saveGrades!!.apply()
                test!!.status = Test.PASS
                finalizeTest()
            })
            /***
             * Grades Additional List
             */
            additionalList = grade!!.grade2
            gradeAdapter2 = GradeAdapter(additionalList, this, GradeAdapter.OnItemClickListener { item, viewHolder ->
                viewHolder.grades.setBackgroundColor(ContextCompat.getColor(this, R.color.gradebg))
                viewHolder.grades.setTextColor(ContextCompat.getColor(this, R.color.white_color))
                saveGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE).edit()
                saveGrades!!.putInt(getString(R.string.grade_position), item.gradePosition)
                saveGrades!!.putBoolean(getString(R.string.grade_selected), true)
                saveGrades!!.apply()
                test!!.status = Test.PASS
                finalizeTest()
            })
            /***
             * File to read for grade result
             */
            val fileContent = ReadTestJsonFile.getInstance().returnNewObject(gradingResults)
            if (fileContent != null && fileContent != "") {
                /***
                 * on restore handle selected text
                 */
                for (gradeItem in gradeList) {
                    if (fileContent == gradeItem.grade) {
                        val sharedGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE)
                        gradeList[sharedGrades.getInt(getString(R.string.grade_position), -1)] = GradeChild(
                                fileContent,
                                sharedGrades.getInt(getString(R.string.grade_position), -1),
                                sharedGrades.getBoolean(getString(R.string.grade_selected), false))
                        gradeAdapter1 = GradeAdapter(gradeList, this, GradeAdapter.OnItemClickListener { item, viewHolder ->
                            viewHolder.grades.setBackgroundColor(ContextCompat.getColor(this, R.color.gradebg))
                            viewHolder.grades.setTextColor(ContextCompat.getColor(this, R.color.white_color))
                            saveGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE).edit()
                            saveGrades!!.putInt(getString(R.string.grade_position), item.gradePosition)
                            saveGrades!!.putBoolean(getString(R.string.grade_selected), true)
                            saveGrades!!.apply()
                            test!!.status = Test.PASS
                            finalizeTest()

                        })

                        val mLayoutManager = LinearLayoutManager(applicationContext)
                        gradingsView.layoutManager = mLayoutManager
                        gradingsView.itemAnimator = DefaultItemAnimator()
                        gradingsView.adapter = gradeAdapter1
                    }
                }
                /***
                 * on restore handle selected text
                 */
                for (gradeItem in additionalList) {
                    if (fileContent == gradeItem.grade) {
                        val sharedGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE)
                        additionalList[sharedGrades.getInt(getString(R.string.grade_position), -1)] = GradeChild(
                                fileContent,
                                sharedGrades.getInt(getString(R.string.grade_position), -1),
                                sharedGrades.getBoolean(getString(R.string.grade_selected), false))
                        gradeAdapter2 = GradeAdapter(additionalList, this, GradeAdapter.OnItemClickListener { item, viewHolder ->
                            viewHolder.grades.setBackgroundColor(ContextCompat.getColor(this, R.color.gradebg))
                            viewHolder.grades.setTextColor(ContextCompat.getColor(this, R.color.white_color))
                            saveGrades = getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE).edit()
                            saveGrades!!.putInt(getString(R.string.grade_position), item.gradePosition)
                            saveGrades!!.putBoolean(getString(R.string.grade_selected), true)
                            saveGrades!!.apply()
                            test!!.status = Test.PASS
                            finalizeTest()
                        })
                        val mLayoutManager = LinearLayoutManager(applicationContext)
                        gradingsView.layoutManager = mLayoutManager
                        gradingsView.itemAnimator = DefaultItemAnimator()
                        gradingsView.adapter = gradeAdapter2
                    }
                }
            }
            val mLayoutManager1 = LinearLayoutManager(applicationContext)
            gradingsView.layoutManager = mLayoutManager1
            gradingsView.itemAnimator = DefaultItemAnimator()
            gradingsView.adapter = gradeAdapter1
            /***
             * making both list smooth scroll
             */
            val mLayoutManager2 = LinearLayoutManager(applicationContext)
            additionalView.layoutManager = mLayoutManager2
            additionalView.itemAnimator = DefaultItemAnimator()
            additionalView.adapter = gradeAdapter2
        } else {
            grades.visibility = View.INVISIBLE
            additional.visibility = View.INVISIBLE
            noGrades.visibility = View.VISIBLE
        }
    }

    override fun onNavDoneClick(v: View) {
        super.onNavDoneClick(v)
        if (test != null && test!!.status != Test.PASS) {
            test!!.status = Test.FAILED
        }
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                GRADES_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_grades), -1)] =
                            RecordTest(context.getString(R.string.report_grade_test), GRADES_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("Grades", "${GRADES_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    companion object {
        var GRADES_SCREEN_TIME = 0
        val gradingResults = File(Loader.baseFile.toString() + "/GradeResults.json")
        var REQ = 2520
    }
}
