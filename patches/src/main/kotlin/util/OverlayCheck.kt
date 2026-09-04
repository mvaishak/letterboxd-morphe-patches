package util

import app.template.patches.letterboxd.theme.buildColorOverlay
import com.reandroid.arsc.chunk.TableBlock
import java.io.File

fun main() {
    val decoded = "/Users/mvaishak/Downloads/letterboxd-decoded"
    val out = File("/Users/mvaishak/.claude/jobs/199c96a8/tmp/ovcheck/oled.arsc")
    val colors = mapOf(
        "gray0D1012" to "#FF000000", "gray14181C" to "#FF000000", "gray181C20" to "#FF000000",
        "windowBackground" to "#FF000000", "gray1C242C" to "#FF121212", "gray202830" to "#FF121212",
        "gray283038" to "#FF121212", "gray223344" to "#FF1C1C1C", "gray2C3440" to "#FF1C1C1C",
        "gray303840" to "#FF1C1C1C", "gray334455" to "#FF2E2E2E", "gray445566" to "#FF2E2E2E",
    )
    buildColorOverlay(
        sourceManifest = File("$decoded/AndroidManifest.xml"),
        sourcePublic = File("$decoded/res/values/public.xml"),
        packageName = "com.letterboxd.letterboxd",
        outputFile = out,
        colors = colors,
    )
    println("wrote ${out.length()} bytes")

    val table = TableBlock.load(out)
    println("re-parsed OK; packages=${table.packageArray.size()}")
    val txt = table.toString()
    println(txt.take(600))
    for (id in intArrayOf(0x7f0600bf, 0x7f060504, 0x7f0600c8)) {
        val res = table.getResource(id)
        println("  0x%08x -> name=%s".format(id, res?.name))
    }
}
