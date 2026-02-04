package com.NovaStudios.custMagisk.core

import android.app.AlertDialog
import android.content.Context
import java.io.File

object MagiskGate {

    fun runMagiskOnly(
        context: Context,
        core: File,
        onAllowed: () -> Unit
    ) {
        val st = RootProbe.probe(context)

        if (st.isMagiskSu && st.rootGranted) {
            val checkCode = CoreRunner.runAsRoot(core, listOf("check_module", MagiskConfig.MODULE_ID)) { }
            if (checkCode != 0) {
                AlertDialog.Builder(context)
                    .setTitle("Нет модуля")
                    .setMessage("Модуль не найден или выключен.\n\nНекоторые функции могут не работать.\nСкачайте модуль из Telegram-канала или из репозитория GitHub.")
                    .setPositiveButton("Ок", null)
                    .show()
                return
            }
            onAllowed()
            return
        }

        if (st.isMagiskSu && !st.rootGranted) {
            AlertDialog.Builder(context)
                .setTitle("Root не выдан")
                .setMessage("Найден MagiskSU, но root-доступ не был выдан.\n\nОткройте Magisk и выдайте разрешение этому приложению.")
                .setPositiveButton("Ок", null)
                .show()
            return
        }

        if (st.suPath != null && !st.isMagiskSu) {
            AlertDialog.Builder(context)
                .setTitle("Найден другой su")
                .setMessage("Найден su, но он не похож на MagiskSU.\n\nMagisk-функции могут не работать с другим su.")
                .setPositiveButton("Ок", null)
                .show()
            return
        }

        AlertDialog.Builder(context)
            .setTitle("Magisk/Root не найдены")
            .setMessage("su не найден. Установите Magisk или другой root-менеджер и повторите.")
            .setPositiveButton("Ок", null)
            .show()
    }
}