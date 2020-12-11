package me.nutyworks.zipgagosipda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.forEach
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private var isDark = false
    private val date = Calendar.getInstance(TimeZone.getDefault())
    private var timer: Timer by Delegates.notNull()
    private var targetTimeTimerTask: TimerTask by Delegates.notNull()

    var targetMillis = 1606287600000

    enum class FormType {
        AS_EACH,
        FULL_DATE,
    }

    private var formType = FormType.AS_EACH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        my_toolbar.setTitle(R.string.app_name)
        setSupportActionBar(my_toolbar)

        targetMillis = getSharedPreferences("TIME_PREF", MODE_PRIVATE)
            .getLong("TARGET_PREF", targetMillis)

        layout_thing.setOnClickListener() {
            formType = when (formType) {
                FormType.AS_EACH -> FormType.FULL_DATE
                FormType.FULL_DATE -> FormType.AS_EACH
            }
            changeForm()
        }
    }

    fun changeForm() {
        val remaining = targetMillis - System.currentTimeMillis()
        fun setFullDate() {
            remaining_time.text = getString(R.string.full_date).format(
                remaining / 1000,
                remaining / 60000,
                remaining / 60000 / 60,
                remaining / 60000 / 60 / 24
            )
        }

        fun setAsEach() {
            remaining_time.text = getString(R.string.as_each).format(
                remaining / 60000 / 60 / 24,
                remaining / 60000 / 60 % 24,
                remaining / 60000 % 60,
                remaining / 1000 % 60
            )
        }
        when (formType) {
            FormType.FULL_DATE -> setFullDate()
            FormType.AS_EACH -> setAsEach()
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer().let {
            it.schedule(object : TimerTask() {
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
        menuInflater.inflate(R.menu.my_menu, menu)

        val hour = date.get(Calendar.HOUR_OF_DAY)

        if (hour > 20 || hour < 7) {
            setDark()
        } else {
            setLight()
        }
        return true
    }

    fun setLight() {
        remaining_time.setTextColor(getColor(R.color.black))
        layout_thing.setBackgroundColor(getColor(R.color.white))
        my_toolbar.setTitleTextColor(getColor(R.color.black))
        my_toolbar.menu.forEach {
            it.icon.colorFilter =
                PorterDuffColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_IN)
        }

        isDark = false
    }

    fun setDark() {
        remaining_time.setTextColor(getColor(R.color.white))
        layout_thing.setBackgroundColor(getColor(R.color.black))
        my_toolbar.setTitleTextColor(getColor(R.color.white))
        my_toolbar.menu.forEach {
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
                    { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                        TimePickerDialog(
                            this,
                            { _: TimePicker, hour: Int, minute: Int ->
                                val combinedCal: Calendar =
                                    Calendar.getInstance(TimeZone.getTimeZone("Seoul/Korea"))
                                        .clone() as Calendar
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}
