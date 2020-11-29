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
            "commit-tree" -> commitTree(directory)
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

fun catFile(
    directory: String,
    hash: String = "",
    log: Boolean = false,
    tree: Boolean = false,
    head: Boolean = false
): String {
    val gitHash = if (hash != "") hash else getString("Enter git object hash:")
    val inflatedFile = InflaterInputStream(gitFile(directory, gitHash)).reader().readLines()
    val headerFirst = inflatedFile[0].split(0.toChar())
    val header = headerFirst[0].split(" ")[0].toUpperCase()
    var commitMessage = false
    var returnString = ""

    if (head) return header
    if (!log && !tree) println("*$header*")
    if (header == "TREE") handleTree(gitFile(directory, gitHash)) else {
        for (i in inflatedFile.indices) {
            if (header == "BLOB" || commitMessage) {
                if (!tree) println(if (i == 0) headerFirst[1] else inflatedFile[i])
            } else {
                if (i != 0 && inflatedFile[i] == "") {
                    commitMessage = true
                    if (i != inflatedFile.lastIndex && !log && !tree) println("commit message:")
                } else {
                    var line = (if (i == 0) headerFirst[1] else inflatedFile[i]).replaceFirst(" ", ": ")

                    when (val type = line.substring(0, 6)) {
                        "parent" -> {
                            if (!log) line = line.replace("parent", "parents") else {
                                if (returnString == "") returnString = line.split(" ")[1]
                            }
                        }
                        "author", "commit" -> if (!log && !tree) line = formatLine(line, type) else if (log) {
                            if (type == "commit") println(formatLine(line.substringAfter(' '), type))
                        }
                        "tree: " -> if (tree) return line.substringAfter(' ')
                    }
                    if (!log && !tree) println(line)
                }
            }
        }
    }
    return returnString
}

fun log(directory: String) {
    val branch = getString("Enter branch name:")
    var hash = File("$directory/refs/heads/$branch").readText().trim()
    var stop = false

    while (!stop) {
        println("Commit: $hash")
        hash = catFile(directory, hash, true)
        if (hash == "") stop = true else println()
    }
}

fun commitTree(directory: String, hash: String = "", line: String = "") {
    val gitHash = if (hash == "") catFile(directory, getString("Enter commit hash"), tree = true) else hash
    val fileMap = handleTree(gitFile(directory, gitHash), true)

    for ((name, hash2) in fileMap) {
        val fileName = if (line == "") name else "$line/$name"

        if (catFile(directory, hash2, head = true) == "TREE") commitTree(directory, hash2, fileName)
        else println(fileName)
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

fun handleTree(gitFile: FileInputStream, returnInfo: Boolean = false): Map<String, String> {
    val inflatedBytes = InflaterInputStream(gitFile).readAllBytes()
    var combined = ""
    var lastWord = ""
    var correctHash = ""
    val fileHash = mutableMapOf<String, String>()
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
                    if (returnInfo) correctHash += String.format("%02X", byte).toLowerCase()
                    if (byteCount == 20) {
                        combined += " $lastWord\n"
                        if (returnInfo) fileHash[lastWord] = correctHash
                        if (returnInfo) correctHash = ""
                        lastWord = ""
                        count = 0
                        byteCount = 0
                    }
                }
            }
        }
        if (!passedHeader && byte.toChar() == 0.toChar()) passedHeader = true
    }
    if (!returnInfo) print(combined)
    gitFile.close()
    return fileHash
}

fun gitFile(directory: String, hash: String) =
    FileInputStream("$directory/objects/${hash.substring(0, 2)}/${hash.substring(2)}")

fun getString(text: String): String {
    println(text)
    return readLine()!!
}