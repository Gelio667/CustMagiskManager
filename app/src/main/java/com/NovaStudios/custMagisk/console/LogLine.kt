enum class LogLevel { INFO, WARN, ERROR, OK }

data class LogLine(val level: LogLevel, val text: String)