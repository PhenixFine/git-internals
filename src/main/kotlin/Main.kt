import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.zip.InflaterInputStream

fun main() {
    try {
        val directory = getString("Enter .git directory location:")
        val gitHash = getString("Enter git object hash:")
        val gitFile = FileInputStream("$directory/${gitHash.substring(0, 2)}/${gitHash.substring(2)}")
        val header = InflaterInputStream(gitFile).reader().readLines()[0].split('\u0000')[0].split(" ")

        println("type:${header[0]} length:${header[1]}")
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