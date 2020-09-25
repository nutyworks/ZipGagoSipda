package me.nutyworks.zipgagosipda

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker


class TimePreference(
    ctxt: Context?,
    attrs: AttributeSet?
) :
    DialogPreference(ctxt, attrs) {
    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePicker? = null
    override fun onCreateDialogView(): View {
        picker = TimePicker(context)
        return picker as TimePicker
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        picker!!.currentHour = lastHour
        picker!!.currentMinute = lastMinute
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (positiveResult) {
            lastHour = picker!!.currentHour
            lastMinute = picker!!.currentMinute
            val time = "$lastHour:$lastMinute"
            if (callChangeListener(time)) {
                persistString(time)
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index)!!
    }

    override fun onSetInitialValue(
        restoreValue: Boolean,
        defaultValue: Any
    ) {
        var time: String? = null
        time = if (restoreValue) {
            if (defaultValue == null) {
                getPersistedString("00:00")
            } else {
                getPersistedString(defaultValue.toString())
            }
        } else {
            defaultValue.toString()
        }
        lastHour = getHour(time)
        lastMinute = getMinute(time)
    }

    companion object {
        fun getHour(time: String?): Int {
            val pieces = time!!.split(":".toRegex()).toTypedArray()
            return pieces[0].toInt()
        }

        fun getMinute(time: String?): Int {
            val pieces = time!!.split(":".toRegex()).toTypedArray()
            return pieces[1].toInt()
        }
    }

    init {
        positiveButtonText = "Set"
        negativeButtonText = "Cancel"
    }
}