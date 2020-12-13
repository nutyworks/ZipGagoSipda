package me.nutyworks.zipgagosipda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
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
import kotlin.reflect.KProperty

const val DISPLAY_PREF = "DisplayPreference"
const val TARGET_MILLIS_PREF = "TargetMillis"
const val THEME_PREF = "Theme"
const val FORM_TYPE_PREF = "FormType"

class MainActivity : AppCompatActivity() {

    enum class FormType {
        AS_EACH,
        FULL_DATE,
    }

    private var targetMillis by Delegates.observable(
        System.currentTimeMillis() + 604_804_000,
        getSaveSharedPreferencesFunction<Long>(DISPLAY_PREF, TARGET_MILLIS_PREF)
                then this::updateWidget
    )

    private var isDark by Delegates.observable(
        true,
        getSaveSharedPreferencesFunction(DISPLAY_PREF, THEME_PREF)
    )
    private var formType by Delegates.observable(
        FormType.AS_EACH,
        getSaveSharedPreferencesFunction(DISPLAY_PREF, FORM_TYPE_PREF)
    )

    private val date = Calendar.getInstance(TimeZone.getDefault())
    private var timer: Timer by Delegates.notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        my_toolbar.setTitle(R.string.app_name)
        setSupportActionBar(my_toolbar)

        getSharedPreferences(DISPLAY_PREF, MODE_PRIVATE).run {
            targetMillis = getLong(TARGET_MILLIS_PREF, targetMillis)
            isDark = getBoolean(THEME_PREF, isDark)
            formType = getString(FORM_TYPE_PREF, formType.toString())?.let { FormType.valueOf(it) }
                ?: FormType.AS_EACH
        }

        layout_thing.setOnClickListener {
            formType = when (formType) {
                FormType.AS_EACH -> FormType.FULL_DATE
                FormType.FULL_DATE -> FormType.AS_EACH
            }
            changeForm()
        }
    }

    private fun <T> getSaveSharedPreferencesFunction(
        namespace: String,
        key: String
    ): (KProperty<*>, T, T) -> Unit {
        return { _, _, newValue ->
            getSharedPreferences(namespace, MODE_PRIVATE).edit {
                fun putAsToString(key: String, value: Any) {
                    putString(key, value.toString())
                }

                when (newValue) {
                    is Boolean -> ::putBoolean
                    is Float -> ::putFloat
                    is Int -> ::putInt
                    is Long -> ::putLong
                    is String -> ::putString
                    is Set<*> -> if (newValue.first() is String) ::putStringSet else ::putAsToString
                    else -> ::putAsToString
                }(key, newValue)
                commit()
            }
        }
    }

    private fun updateWidget() {
        Intent(this, ZipGagoSipdaWidget::class.java).run {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                AppWidgetManager.getInstance(application)
                    .getAppWidgetIds(ComponentName(application, ZipGagoSipdaWidget::class.java))
            )
        }.let {
            sendBroadcast(it)
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

        if (isDark) {
            setDark()
        } else {
            setLight()
        }

        return true
    }

    private fun setLight() {
        remaining_time.setTextColor(getColor(R.color.black))
        layout_thing.setBackgroundColor(getColor(R.color.white))
        my_toolbar.setTitleTextColor(getColor(R.color.black))
        my_toolbar.menu.forEach {
            it.icon.colorFilter =
                PorterDuffColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_IN)
        }

        isDark = false
    }

    private fun setDark() {
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

private infix fun <P1A, P1B, P1C, R1, R2> ((P1A, P1B, P1C) -> R1).then(other: () -> R2): (P1A, P1B, P1C) -> R2 =
    { p1A, p1B, p1C ->
        this(p1A, p1B, p1C)
        other()
    }
