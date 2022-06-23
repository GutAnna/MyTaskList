package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import java.io.File

enum class ColorText(val color: String) {
    C("\u001B[101m \u001B[0m"),
    H("\u001B[103m \u001B[0m"),
    N("\u001B[102m \u001B[0m"),
    L("\u001B[104m \u001B[0m"),
    I("\u001B[102m \u001B[0m"),
    T("\u001B[103m \u001B[0m"),
    O("\u001B[101m \u001B[0m");

    companion object {
        fun getStr(char: Char): String {
            return when (char) {
                'C' -> C.color
                'H' -> H.color
                'N' -> N.color
                'L' -> L.color
                'I' -> I.color
                'T' -> T.color
                'O' -> O.color
                else -> " "
            }
        }
    }
}


class Task(
    var text: MutableList<String>,
    var priority: Char, var date: String, var time: String
)

object DataBase {
    var tasks = mutableListOf<Task>()

    private fun inputPriority(): Char {
        while (true) {
            println("Input the task priority (C, H, N, L):")
            val priority = readLine()!!.uppercase()
            if (priority.length > 1 || priority.isEmpty()) continue
            if (priority.first() in "CHNL".toList()) return priority.first()
        }
    }

    private fun inputDate(): String {
        while (true) {
            try {
                println("Input the date (yyyy-mm-dd):")
                val (y, m, d) = readLine()!!.split('-').map { it.toInt() }
                val date = LocalDateTime(y, m, d, 0, 0)
                return "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${
                    date.dayOfMonth.toString().padStart(2, '0')
                }"
            } catch (e: Exception) {
                println("The input date is invalid")
            }
        }
    }

    private fun inputTime(): String {
        while (true) {
            try {
                println("Input the time (hh:mm):")
                val (h, m) = readLine()!!.split(':').map { it.padStart(2, '0') }
                val time = "1990-01-01T$h:$m".toLocalDateTime()
                return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
            } catch (e: Exception) {
                println("The input time is invalid")
            }
        }
    }

    private fun inputText(): MutableList<String> {
        println("Input a new task (enter a blank line to end):")
        val task = mutableListOf<String>()
        while (true) {
            val line = readLine()!!.trim()
            if (line.isNotEmpty()) task.add(line) else break
        }
        if (task.isEmpty()) println("The task is blank")
        return task
    }

    fun addTasks() {
        val priority = inputPriority()
        val date = inputDate()
        val time = inputTime()
        val task = inputText()
        if (task.isNotEmpty()) tasks.add(Task(task, priority, date, time))
    }

    fun cutString(str: String): List<String> {
        val list = mutableListOf<String>()
        if (str.length > 44)
            while (true) {
                list.add(str.substring(0,44))
                var newStr = str.substring(44)
                if (newStr.length<=44) {
                    list.add(newStr)
                    return list
                }
            }
        else {list.add(str); return list}
    }

    fun printTasks(): Boolean {
        if (tasks.isEmpty()) {
            println("No tasks have been input"); return false
        }
        println("+----+${"-".repeat(12)}+-------+---+---+${"-".repeat(44)}+")
        println("| N  |    Date    | Time  | P | D |${" ".repeat(19)}Task${" ".repeat(21)}|")
        println("+----+${"-".repeat(12)}+-------+---+---+${"-".repeat(44)}+")
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date

        for (i in tasks.indices) {
            val index = (i + 1).toString().padEnd(2, ' ')
            val numberOfDays = currentDate.daysUntil(tasks[i].date.toLocalDate())
            val tag = if (numberOfDays > 0) 'I' else if (numberOfDays == 0) 'T' else 'O'

            print("| $index | ${tasks[i].date} | ${tasks[i].time} | ${ColorText.getStr(tasks[i].priority)} | ${ColorText.getStr(tag)} |")
            for (j in tasks[i].text.indices) {
                val taskLine = cutString(tasks[i].text[j])
                for (line in taskLine.indices) {
                    if (j>0 || line>0)  print("|    |${" ".repeat(12)}|       |   |   |")
                    println("${taskLine[line]}${" ".repeat(44-taskLine[line].length)}|")
                }
            }
            println("+----+${"-".repeat(12)}+-------+---+---+${"-".repeat(44)}+")
        }
        return true
    }

    fun deleteTask() {
        if (!printTasks()) return
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            try {
                tasks.removeAt(readLine()!!.toInt() - 1)
                println("The task is deleted")
                return
            } catch (e: Exception) {
                println("Invalid task number")
            }
        }
    }

    fun editTask() {
        if (!printTasks()) return
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            try {
                val number = readLine()!!.toInt() - 1
                if (number !in tasks.indices) throw Exception()
                while (true) {
                    println("Input a field to edit (priority, date, time, task):")
                    when (readLine()!!) {
                        "priority" -> tasks[number].priority = inputPriority()
                        "date" -> tasks[number].date = inputDate()
                        "time" -> tasks[number].time = inputTime()
                        "task" -> tasks[number].text = inputText()
                        else -> { println("Invalid field"); continue }
                    }
                    println("The task is changed"); return
                }
            } catch (e: Exception) {
                println("Invalid task number")
            }
        }
    }
}

fun main() {
    val jsonFile = File("tasklist.json")
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java,Task::class.java)
    val taskListAdapter = moshi.adapter<List<Task>>(type)
    if (jsonFile.exists()) {
        DataBase.tasks = taskListAdapter.fromJson(jsonFile.readText())!!.toMutableList()
    }

    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readLine()!!.lowercase()) {
            "add" -> DataBase.addTasks()
            "print" -> DataBase.printTasks()
            "edit" -> DataBase.editTask()
            "delete" -> DataBase.deleteTask()
            "end" -> {
                jsonFile.writeText(taskListAdapter.toJson(DataBase.tasks))
                println("Tasklist exiting!"); return
            }
            else -> println("The input action is invalid")
        }
    }
}