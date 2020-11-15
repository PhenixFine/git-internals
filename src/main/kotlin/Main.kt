import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.zip.InflaterInputStream

fun main() {
    try {
        val gitFile = FileInputStream(getString("Enter git object location:"))
        val inflate = InflaterInputStream(gitFile).reader()
        var firstLine = true

        inflate.forEachLine {
            if (firstLine) {
                for (char in it) if (char.toInt() == 0) println() else print(char)
                firstLine = false
                println()
            } else println(it)
        }
        inflate.close()
        gitFile.close()
    } catch (e: FileNotFoundException) {
        println("File not found.\n")
    } catch (e: Exception) {
        println("There was an error in loading your file. Please ensure it is not open in another program.\n")
    }
}

fun getString(text: String): String {
    println(text)
    return readLine()!!
}