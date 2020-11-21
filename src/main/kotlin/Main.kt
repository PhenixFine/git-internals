import java.io.FileInputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.zip.InflaterInputStream
import java.util.TimeZone

fun main() {
    try {
        val directory = getString("Enter .git directory location:")
        val gitHash = getString("Enter git object hash:")
        val gitFile = FileInputStream("$directory/objects/${gitHash.substring(0, 2)}/${gitHash.substring(2)}")
        val inflatedFile = InflaterInputStream(gitFile).reader().readLines()
        val headerFirst = inflatedFile[0].split(0.toChar())
        val header = headerFirst[0].split(" ")[0].toUpperCase()
        var commitMessage = false

        println("*$header*")
        for (i in inflatedFile.indices) {
            if (header == "BLOB" || commitMessage) {
                println(if (i == 0) headerFirst[1] else inflatedFile[i])
            } else {
                if (i != 0 && inflatedFile[i] == "") {
                    commitMessage = true
                    if (i != inflatedFile.lastIndex) println("commit message:")
                } else {
                    var line = (if (i == 0) headerFirst[1] else inflatedFile[i]).replaceFirst(" ", ": ")

                    when (val type = line.substring(0, 6)) {
                        "parent" -> line = line.replace("parent", "parents")
                        "author", "commit" -> line = formatLine(line, type)
                    }
                    println(line)
                }
            }
        }
        gitFile.close()
    } catch (e: FileNotFoundException) {
        println("File not found.\n")
    } catch (e: Exception) {
        println("There was an error in loading your file. Please ensure it is not open in another program.\n")
    }
}

fun formatLine(lineStr: String, type: String): String {
    var line = ""
    val hold = lineStr.split(" ").toMutableList()
    val gmtString = hold[hold.lastIndex].substring(0, 3) + ":" + hold[hold.lastIndex].substring(3)
    val addWords = (if (type == "author") " original" else " commit") + " timestamp:"
    val formatTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val gmtNum = (gmtString.substring(0, 3).toInt() * 3600) +
            ((gmtString.substring(0, 1) + gmtString.substring(4)).toInt() * 60)
    val timeNum = (hold[hold.lastIndex - 1].toLong() + gmtNum) * 1000L

    formatTime.timeZone = TimeZone.getTimeZone("GMT")
    hold[hold.lastIndex] = gmtString
    hold[hold.lastIndex - 1] = formatTime.format(timeNum)
    hold.forEach { line += "$it " }
    line = line.replace("<", "").replace(">", addWords)

    return line.trim()
}

fun getString(text: String): String {
    println(text)
    return readLine()!!
}