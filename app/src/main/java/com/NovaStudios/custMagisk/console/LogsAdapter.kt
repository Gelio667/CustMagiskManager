import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogsAdapter(private val items: MutableList<LogLine>) : RecyclerView.Adapter<LogsAdapter.VH>() {

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log_line, parent, false) as ViewGroup
        return VH(v.findViewById(R.id.txtLine))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val line = items[position]
        holder.tv.text = "${line.level.name}: ${line.text}"
    }

    fun add(line: LogLine) {
        items.add(line)
        notifyItemInserted(items.size - 1)
    }

    fun allText(): String {
        return items.joinToString("\n") { "${it.level.name}: ${it.text}" }
    }
}