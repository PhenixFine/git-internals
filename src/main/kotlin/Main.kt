import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.zip.InflaterInputStream
import java.util.TimeZone

fun main() {
    try {
        val directory = getString("Enter .git directory location:")

        when (getString("Enter command:").toLowerCase()) {
            "list-branches" -> listBranches(directory)
            "cat-file" -> catFile(directory)
            "log" -> log(directory)
        }

    } catch (e: FileNotFoundException) {
        println("File not found.")
    } catch (e: Exception) {
        println("There was an error in loading your file. Please ensure it is not open in another program.")
    }
}

fun listBranches(directory: String) {
    val head = File("$directory/HEAD").readText().split("/").last().trim()
    val directoryList = (File("$directory/refs/heads").list()?.sorted())

    if (directoryList == null) println("Branches are missing.") else {
        for (name in directoryList) println((if (name == head) "* " else "  ") + name)
    }
}

fun catFile(directory: String) {
    val gitHash = getString("Enter git object hash:")
    val gitFile = { FileInputStream("$directory/objects/${gitHash.substring(0, 2)}/${gitHash.substring(2)}") }
    val inflatedFile = InflaterInputStream(gitFile()).reader().readLines()
    val headerFirst = inflatedFile[0].split(0.toChar())
    val header = headerFirst[0].split(" ")[0].toUpperCase()
    var commitMessage = false

    println("*$header*")
    if (header == "TREE") handleTree(gitFile()) else {
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
    }
}

fun log(directory: String) {
    val branch = getString("Enter branch name:")
    var hash = File("$directory/refs/heads/$branch").readText().trim()
    val gitFile = { FileInputStream("$directory/objects/${hash.substring(0, 2)}/${hash.substring(2)}") }
    val inflatedFile = { InflaterInputStream(gitFile()).reader().readLines() }
    var currentFile = inflatedFile()
    var stop = false
    var commitMessage = false

    while (!stop) {
        var parentFound = false

        stop = true
        println("Commit: $hash")
        for (i in currentFile.indices) {
            val line = if (i == 0) currentFile[0].split(0.toChar())[1] else currentFile[i]

            when {
                commitMessage -> println(line)
                line == "" -> commitMessage = true
                else -> {
                    when (val type = line.substring(0, 6)) {
                        "parent" -> if (!parentFound) {
                            hash = line.split(" ")[1]
                            stop = false
                            parentFound = true
                        }
                        "commit" -> println(formatLine(line.substringAfter(' '), type))
                    }
                }
            }
        }
        if (!stop) {
            currentFile = inflatedFile()
            commitMessage = false
            println()
        }
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

fun handleTree(gitFile: FileInputStream) {
    val inflatedBytes = InflaterInputStream(gitFile).readAllBytes()
    var combined = ""
    var lastWord = ""
    var count = 0
    var byteCount = 0
    var passedHeader = false

    for (byte in inflatedBytes) {
        if (passedHeader) {
            when (count) {
                0 -> {
                    if (byte.toChar() == ' ') count++
                    combined += "${byte.toChar()}"
                }
                1 -> if (byte.toChar() == 0.toChar()) count++ else lastWord += "${byte.toChar()}"
                2 -> {
                    byteCount++
                    combined += String.format("%X", byte).toLowerCase()
                    if (byteCount == 20) {
                        combined += " $lastWord\n"
                        lastWord = ""
                        count = 0
                        byteCount = 0
                    }
                }
            }
        }
        if (!passedHeader && byte.toChar() == 0.toChar()) passedHeader = true
    }
    print(combined)
    gitFile.close()
}

fun getString(text: String): String {
    println(text)
    return readLine()!!
}