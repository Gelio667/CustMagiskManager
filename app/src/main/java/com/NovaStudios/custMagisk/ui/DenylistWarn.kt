package com.NovaStudios.custMagisk.ui

import android.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.TextView
import com.NovaStudios.custMagisk.R
import com.NovaStudios.custMagisk.core.RootProbe
import com.NovaStudios.custMagisk.core.MagiskConfig

object DenylistWarn {

    fun show(
        context: Context,
        modeTitle: String,
        onProceed: () -> Unit,
        onOpenMagisk: () -> Unit
    ) {
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_denylist_warning, null, false)
        val title = v.findViewById<TextView>(R.id.txtWarnTitle)
        val body = v.findViewById<TextView>(R.id.txtWarnBody)
        val timer = v.findViewById<TextView>(R.id.txtTimer)
        val chk = v.findViewById<CheckBox>(R.id.chkConfirm)

        title.text = modeTitle
        body.text = "Эта функция влияет на поведение Magisk.\nУбедитесь, что понимаете последствия.\n\nЕсли сомневаетесь — откройте настройки Magisk и настройте DenyList вручную."

        var canProceed = false

        val d = AlertDialog.Builder(context)
            .setView(v)
            .setCancelable(false)
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Перейти в настройки Magisk") { dialog, _ ->
                dialog.dismiss()
                onOpenMagisk()
            }
            .setPositiveButton("Включить") { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .create()

        d.setOnShowListener {
            val pos = d.getButton(AlertDialog.BUTTON_POSITIVE)
            pos.isEnabled = false

            chk.setOnCheckedChangeListener { _, isChecked ->
                pos.isEnabled = canProceed && isChecked
            }

            object : CountDownTimer(10_000, 200) {
                override fun onTick(ms: Long) {
                    val sec = ((ms + 999) / 1000).toInt()
                    timer.text = "Подождите: ${sec}с"
                }

                override fun onFinish() {
                    timer.text = "Можно продолжить"
                    canProceed = true
                    chk.isEnabled = true
                    pos.isEnabled = chk.isChecked
                }
            }.start()
        }

        d.show()
    }

    fun denylistOnlyGate(context: Context, onOk: () -> Unit, onOpenMagisk: () -> Unit) {
        val st = com.NovaStudios.custMagisk.core.RootProbe.probe(context)

        if (st.isMagiskSu && st.rootGranted) {
            show(context, "DenyList", onOk, onOpenMagisk)
            return
        }

        if (st.isMagiskSu && !st.rootGranted) {
            android.app.AlertDialog.Builder(context)
                .setTitle("Root не выдан")
                .setMessage("Найден MagiskSU, но root-доступ не был выдан.\n\nОткройте Magisk и выдайте разрешение этому приложению.")
                .setPositiveButton("Ок", null)
                .show()
            return
        }

        if (st.suPath != null && !st.isMagiskSu) {
            android.app.AlertDialog.Builder(context)
                .setTitle("Найден другой su")
                .setMessage("Найден su, но он не похож на MagiskSU.\n\nDenyList управляется через Magisk. Откройте Magisk и настройте вручную.")
                .setPositiveButton("Ок", null)
                .show()
            return
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Magisk/Root не найдены")
            .setMessage("su не найден. Установите Magisk и повторите.")
            .setPositiveButton("Ок", null)
            .show()
    }
}