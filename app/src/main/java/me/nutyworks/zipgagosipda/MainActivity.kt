package me.nutyworks.zipgagosipda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.view.forEach
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private var isDark = false
    private lateinit var _layout: ConstraintLayout
    lateinit var rem: TextView
    private lateinit var toolbar: Toolbar

    private val date = Calendar.getInstance(TimeZone.getDefault())
    var targetMillis = 1606287600000

    var timer: Timer by Delegates.notNull()
    var targetTimeTimerTask: TimerTask by Delegates.notNull()

    private var isTouched = true


    enum class FormType {
        AS_EACH,
        FULL_DATE,
    }
    private var formType = FormType.AS_EACH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _layout = findViewById(R.id.layout_thing)
        rem = findViewById(R.id.remaining_time)
        toolbar = findViewById(R.id.my_toolbar)

        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        targetMillis = getSharedPreferences("TIME_PREF", MODE_PRIVATE)
                            .getLong("TARGET_PREF", targetMillis)

        layout_thing.setOnClickListener() {
            isTouched = !isTouched
            changeForm()
        }
    }

    fun changeForm() {
        val remaining = targetMillis - System.currentTimeMillis()
        fun fullDate() {
            rem.text = getString(R.string.full_date).format(
                remaining / 1000,
                remaining / 60000,
                remaining / 60000 / 60,
                remaining / 60000 / 60 / 24
            )
        }

        fun asEach() {
            rem.text = getString(R.string.as_each).format(
                remaining / 60000 / 60 / 24,
                remaining / 60000 / 60 % 24,
                remaining / 60000 % 60,
                remaining / 1000 % 60
            )
        }
        if (isTouched) {
            fullDate()
        } else {
            asEach()
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer().let {
            it.schedule(object: TimerTask() {
                override fun run() {
                    runOnUiThread {
                        changeForm()
                    }
                }
            }, 0, 1000)
            it
        }
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.my_menu , menu)

        val hour = date.get(Calendar.HOUR_OF_DAY)

        formType = when(formType) {
            FormType.AS_EACH -> FormType.FULL_DATE
            FormType.FULL_DATE -> FormType.AS_EACH
        }
        return true
    }

    fun setLight() {
        rem.setTextColor(getColor(R.color.black))
        _layout.setBackgroundColor(getColor(R.color.white))
        toolbar.setTitleTextColor(getColor(R.color.black))
        toolbar.menu.forEach {
            it.icon.colorFilter =
                PorterDuffColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_IN)
        }

        isDark = false
    }

    fun setDark() {
        rem.setTextColor(getColor(R.color.white))
        _layout.setBackgroundColor(getColor(R.color.black))
        toolbar.setTitleTextColor(getColor(R.color.white))
        toolbar.menu.forEach {
            it.icon.colorFilter =
                PorterDuffColorFilter(getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        }

        isDark = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.change_theme_btn -> {
                if (isDark) {
                    setLight()
                } else {
                    setDark()
                }
                true
            }
            R.id.change_date_btn -> {
                val datePicker = DatePickerDialog(
                    this,
//                    if (isDark) R.style.DARK else R.style.Theme_AppCompat_DayNight_Dialog_Alert,
                    { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                        val timePicker = TimePickerDialog(
                            this,
//                            if (isDark) R.style.Theme_AppCompat_Light_Dialog_Alert else R.style.Theme_AppCompat_DayNight_Dialog_Alert,
                            { _: TimePicker, hour: Int, minute: Int ->
                                val combinedCal: Calendar =
                                    Calendar.getInstance(TimeZone.getTimeZone("Seoul/Korea")).clone() as Calendar
                                combinedCal.set(year, monthOfYear, dayOfMonth)
                                combinedCal.set(Calendar.HOUR_OF_DAY, hour)
                                combinedCal.set(Calendar.MINUTE, minute)
                                combinedCal.set(Calendar.SECOND, 0)
                                combinedCal.set(Calendar.MILLISECOND, 0)
                                targetMillis = combinedCal.timeInMillis - 32400000
                                getSharedPreferences("TIME_PREF", MODE_PRIVATE).edit {
                                    putLong("TARGET_PREF", targetMillis)
                                    commit()
                                }
//                                Log.d("ADF", "onOptionsItemSelected: $targetMillis")
                            },
                            date.get(Calendar.HOUR_OF_DAY),
                            date.get(Calendar.MINUTE),
                           true
                        ).show()
                    },
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DATE)
                )
                datePicker.show()
                true
            }
//            R.id.settings_btn -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
