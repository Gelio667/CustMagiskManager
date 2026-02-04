import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator

class ConsoleBottomSheet : BottomSheetDialogFragment() {

    enum class Mode { TIMER, FACTS }
    private var mode: Mode = Mode.FACTS
    private var title: String = "Операция"
    private var estimatedSeconds: Int = 0

    private lateinit var timerContainer: View
    private lateinit var timerRing: CircularProgressIndicator
    private lateinit var timerText: TextView
    private lateinit var titleText: TextView
    private lateinit var factText: TextView
    private lateinit var logsList: RecyclerView

    private val logs = mutableListOf<LogLine>()
    private val adapter = LogsAdapter(logs)

    private var facts: List<String> = emptyList()
    private var factIndex = 0
    private var factsTicker: CountDownTimer? = null
    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mode = Mode.valueOf(it.getString("mode") ?: Mode.FACTS.name)
            title = it.getString("title") ?: "Операция"
            estimatedSeconds = it.getInt("seconds", 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bsu_console, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        timerContainer = view.findViewById(R.id.timerContainer)
        timerRing = view.findViewById(R.id.timerRing)
        timerText = view.findViewById(R.id.timerText)
        titleText = view.findViewById(R.id.titleText)
        factText = view.findViewById(R.id.factText)
        logsList = view.findViewById(R.id.logsList)

        titleText.text = title

        logsList.layoutManager = LinearLayoutManager(requireContext())
        logsList.adapter = adapter

        view.findViewById<View>(R.id.btnHide).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.btnCopy).setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("console_log", adapter.allText()))
        }
        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            addLog(LogLevel.WARN, "Отмена запрошена")
            stopTimers()
        }

        facts = loadFacts(requireContext())
        applyMode()
        demoLogs()
    }

    override fun onDestroyView() {
        stopTimers()
        super.onDestroyView()
    }

    private fun applyMode() {
        if (mode == Mode.TIMER && estimatedSeconds > 0) {
            timerContainer.visibility = View.VISIBLE
            startCountdown(estimatedSeconds)
            factText.text = ""
        } else {
            timerContainer.visibility = View.GONE
            startFactsTicker()
        }
    }

    private fun startCountdown(seconds: Int) {
        timerRing.max = seconds
        timerRing.progress = seconds
        timerText.text = "${seconds}s"

        countdown?.cancel()
        countdown = object : CountDownTimer((seconds * 1000L), 200L) {
            var lastShown = seconds
            override fun onTick(ms: Long) {
                val s = (ms / 1000L).toInt()
                if (s != lastShown) {
                    lastShown = s
                    if (s >= 0) {
                        timerRing.progress = s
                        timerText.text = "${s}s"
                    }
                }
            }

            override fun onFinish() {
                timerText.text = "● ● ●"
                timerRing.progress = 0
            }
        }.start()
    }

    private fun startFactsTicker() {
        if (facts.isEmpty()) {
            factText.text = "«…»"
            return
        }
        factIndex = 0
        factText.text = "«${facts[factIndex]}»"

        factsTicker?.cancel()
        factsTicker = object : CountDownTimer(Long.MAX_VALUE, 8000L) {
            override fun onTick(ms: Long) {
                factIndex = (factIndex + 1) % facts.size
                factText.text = "«${facts[factIndex]}»"
            }
            override fun onFinish() {}
        }.start()
    }

    private fun stopTimers() {
        countdown?.cancel()
        countdown = null
        factsTicker?.cancel()
        factsTicker = null
    }

    private fun addLog(level: LogLevel, text: String) {
        adapter.add(LogLine(level, text))
        logsList.post { logsList.scrollToPosition(adapter.itemCount - 1) }
    }

    private fun demoLogs() {
        addLog(LogLevel.INFO, "Инициализация операции")
        addLog(LogLevel.INFO, "Проверяю доступ")
        addLog(LogLevel.INFO, "Готовлю шаги")
        addLog(LogLevel.OK, "Ожидаю продолжение")
    }

    companion object {
        fun showTimer(title: String, seconds: Int): ConsoleBottomSheet {
            val b = ConsoleBottomSheet()
            b.arguments = Bundle().apply {
                putString("mode", Mode.TIMER.name)
                putString("title", title)
                putInt("seconds", seconds)
            }
            return b
        }

        fun showFacts(title: String): ConsoleBottomSheet {
            val b = ConsoleBottomSheet()
            b.arguments = Bundle().apply {
                putString("mode", Mode.FACTS.name)
                putString("title", title)
                putInt("seconds", 0)
            }
            return b
        }
    }

    fun runCore(core: java.io.File, asRoot: Boolean, args: List<String>) {
        addLog(LogLevel.INFO, "Запуск: ${args.joinToString(" ")}")
        Thread {
            val code = if (asRoot) {
                com.NovaStudios.custMagisk.core.CoreRunner.runAsRoot(core, args) { line ->
                    pushCoreLine(line)
                }
            } else {
                com.NovaStudios.custMagisk.core.CoreRunner.runNoRoot(core, args) { line ->
                    pushCoreLine(line)
                }
            }
            requireActivity().runOnUiThread {
                if (code == 0) addLog(LogLevel.OK, "Готово") else addLog(LogLevel.ERROR, "Код выхода: $code")
            }
        }.start()
    }

    private fun pushCoreLine(line: com.NovaStudios.custMagisk.core.CoreLine) {
        requireActivity().runOnUiThread {
            when (line.level) {
                "INFO" -> addLog(LogLevel.INFO, line.msg)
                "WARN" -> addLog(LogLevel.WARN, line.msg)
                "ERROR" -> addLog(LogLevel.ERROR, line.msg)
                "OK" -> addLog(LogLevel.OK, line.msg)
                "DATA" -> addLog(LogLevel.INFO, line.json ?: "")
                else -> addLog(LogLevel.INFO, "${line.level}: ${line.msg}")
            }
        }
    }

    fun addExternalLog(text: String) {
        addLog(LogLevel.INFO, text)
    }
}