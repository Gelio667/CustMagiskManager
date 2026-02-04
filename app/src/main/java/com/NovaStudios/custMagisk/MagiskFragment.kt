package com.NovaStudios.custMagisk

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.NovaStudios.custMagisk.console.ConsoleBottomSheet
import com.NovaStudios.custMagisk.core.CoreInstaller
import com.NovaStudios.custMagisk.core.MagiskConfig
import com.NovaStudios.custMagisk.core.MagiskGate
import com.NovaStudios.custMagisk.ui.DenylistWarn
import com.NovaStudios.custMagisk.ui.ModuleDownloader
import com.NovaStudios.custMagisk.ui.ZipPicker
import java.io.File

class MagiskFragment : Fragment() {

    private var pickedZip: File? = null

    private val pickZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedZip = ZipPicker.copyToCache(requireContext(), uri)
            AlertDialog.Builder(requireContext())
                .setTitle("ZIP выбран")
                .setMessage(pickedZip!!.absolutePath)
                .setPositiveButton("Ок", null)
                .show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_magisk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val core = CoreInstaller.ensureCore(requireContext())
        val moduleId = MagiskConfig.MODULE_ID

        fun openConsole(title: String): ConsoleBottomSheet {
            val sheet = ConsoleBottomSheet.showFacts(title)
            sheet.show(parentFragmentManager, "console")
            return sheet
        }

        view.findViewById<View>(R.id.tileInstall).setOnClickListener {
            MagiskGate.runMagiskOnly(requireContext(), core) {
                val items = arrayOf(
                    "Скачать модуль",
                    "Выбрать ZIP из файлов",
                    "Установить выбранный ZIP",
                    "Проверить состояние модуля",
                    "Включить модуль",
                    "Выключить модуль",
                    "Удалить модуль"
                )

                AlertDialog.Builder(requireContext())
                    .setTitle("CustMagisk")
                    .setItems(items) { _, which ->
                        when (which) {
                            0 -> {
                                val url = MagiskConfig.MODULE_URL
                                if (url.isBlank()) {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Ссылка не задана")
                                        .setMessage("Укажи ссылку на ZIP модуля в MagiskConfig.MODULE_URL")
                                        .setPositiveButton("Ок", null)
                                        .show()
                                } else {
                                    ModuleDownloader.enqueue(requireContext(), url)
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Скачивание начато")
                                        .setMessage("Файл будет в Загрузках как CustMagisk.zip")
                                        .setPositiveButton("Ок", null)
                                        .show()
                                }
                            }
                            1 -> pickZip.launch(arrayOf("application/zip"))
                            2 -> {
                                val f = pickedZip
                                if (f == null) {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("ZIP не выбран")
                                        .setMessage("Сначала выбери файл ZIP.")
                                        .setPositiveButton("Ок", null)
                                        .show()
                                } else {
                                    val sheet = ConsoleBottomSheet.showTimer("Установка модуля", 25)
                                    sheet.show(parentFragmentManager, "console")
                                    sheet.runCore(core, true, listOf("module_install", f.absolutePath))
                                }
                            }
                            3 -> {
                                val sheet = openConsole("Состояние модуля")
                                sheet.runCore(core, true, listOf("module_state", moduleId))
                            }
                            4 -> {
                                val sheet = openConsole("Включить модуль")
                                sheet.runCore(core, true, listOf("module_enable", moduleId))
                            }
                            5 -> {
                                val sheet = openConsole("Выключить модуль")
                                sheet.runCore(core, true, listOf("module_disable", moduleId))
                            }
                            6 -> {
                                val sheet = ConsoleBottomSheet.showTimer("Удалить модуль", 12)
                                sheet.show(parentFragmentManager, "console")
                                sheet.runCore(core, true, listOf("module_remove", moduleId))
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }

        view.findViewById<View>(R.id.tilePatch).setOnClickListener {
            MagiskGate.runMagiskOnly(requireContext(), core) {
                val sheet = openConsole("Патч boot/init_boot")
                sheet.addExternalLog("Эта функция будет реализована позже в отдельном шаге.")
            }
        }

        view.findViewById<View>(R.id.tileFlash).setOnClickListener {
            MagiskGate.runMagiskOnly(requireContext(), core) {
                val sheet = openConsole("Флеш boot/init_boot")
                sheet.addExternalLog("Эта функция будет реализована позже в отдельном шаге.")
            }
        }

        view.findViewById<View>(R.id.tileDenyList).setOnClickListener {
            DenylistWarn.denylistOnlyGate(
                requireContext(),
                onOk = {
                    val sheet = openConsole("DenyList")
                    sheet.addExternalLog("Откройте Magisk и настройте DenyList вручную.")
                },
                onOpenMagisk = {
                    openMagisk()
                }
            )
        }

        view.findViewById<View>(R.id.tileLogs).setOnClickListener {
            val sheet = openConsole("Диагностика / env")
            sheet.runCore(core, false, listOf("env"))
        }

        view.findViewById<View>(R.id.btnOpenMagisk).setOnClickListener {
            openMagisk()
        }
    }

    private fun openMagisk() {
        val pm = requireContext().packageManager
        val i = pm.getLaunchIntentForPackage("com.topjohnwu.magisk")
        if (i != null) {
            requireContext().startActivity(i)
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Magisk скрыт или не найден")
            .setMessage("Не удалось открыть Magisk по стандартному пакету.\n\nЕсли Magisk скрыт/переименован — откройте его вручную из списка приложений.")
            .setPositiveButton("Ок", null)
            .show()
    }
}