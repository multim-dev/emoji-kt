import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.Paths

class EmojiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("generateEmoji")
            .doLast(Action {
                runBlocking {

                    val httpClient = HttpClient(CIO)
                    val bodyAsText =
                        httpClient.get("https://unicode.org/Public/emoji/15.0/emoji-test.txt")
                            .bodyAsText()
                    println(project.buildDir.path)
//                    println(bodyAsText)

                    val file =
                        project.layout.buildDirectory.file("generated/source/emoji/main/kotlin/Emojis.kt")
                    val asFile = file.get().asFile
                    asFile.parentFile.mkdirs()
                    val split = bodyAsText.replace("\r", "").split("\n");

                    println(split.size)

                    var group: String = ""
                    var subgroup: String = ""

                    data class Emoji(val group: String, val value: String)

                    val enumList = mutableMapOf<String, Emoji>()
                    //TODO グループ分けしたことで重複がなくなるはずなので修正
                    for (s in split) {
                        when {
                            s.startsWith("# group:") -> {
                                group = s.substringAfter(": ")
                            }

                            s.startsWith("# subgroup:") -> {
                                subgroup = s.substringAfter(": ")
                            }

                            s.isNullOrBlank() -> {}
                            s.startsWith("#") -> {}
                            else -> {
                                val description = s.substringAfterLast("E").substringAfter(" ")
                                if (!description.contains("skin tone")) {

                                    val code =
                                        s.substringBefore(";").replace(Regex(" +"), " ").trim()
                                    val char = s.substringAfter("# ").substringBefore(" ").trim()
                                    val status = s.substringAfter(";").substringBefore("#").trim()
                                    enumList.put(
                                        description,
                                        Emoji(
                                            group,
                                            "${
                                                (description + "_" + status).toUpperCase()
                                                    .replace(" ", "_")
                                                    .replace("-", "_")
                                                    .replace(":", "_")
                                                    .replace(",", "_")
                                                    .replace(".", "_")
                                                    .replace("’", "_")
                                                    .replace("1ST", "FIRST")
                                                    .replace("2ND", "SECOND")
                                                    .replace("3RD", "THIRD")
                                                    .replace("!", "_EXCLAMATION_MARK_")
                                                    .replace("#", "SHARP")
                                                    .replace("*", "ASTRISC")
                                                    .replace("0", "ZERO")
                                                    .replace("1", "ONE")
                                                    .replace("2", "TWO")
                                                    .replace("3", "THREE")
                                                    .replace("4", "FOUR")
                                                    .replace("5", "FIVE")
                                                    .replace("6", "SIX")
                                                    .replace("7", "SEVEN")
                                                    .replace("8", "EIGHT")
                                                    .replace("9", "NINE")
                                                    .replace("“", "_")
                                                    .replace("”", "_")
                                                    .replace("(", "_")
                                                    .replace(")", "_")
                                                    .replace("&", "_AND_")
                                                    .replace("Ã", "A")
                                                    .replace("É", "E")
                                                    .replace("Í", "I")
                                                    .replace("Ñ", "N")
                                                    .replace("Å", "A")
                                                    .replace("Ô", "O")
                                                    .replace("Ç", "C")
                                                    .replace(Regex("_+"), "_")
                                            }(\"$group\",\"$subgroup\",\"$code\",\"$char\",\"$description\")"
                                        )
                                    )
                                }
                            }
                        }
                    }
                    val emojis = mutableMapOf<String, MutableList<Emoji>>()
                    enumList.values.forEach {
                        emojis.getOrPut(
                            it.group.replace(" ", "").replace("&", "And")
                        ) { mutableListOf() }.add(it)
                    }

                    val map = emojis.map {
                        "enum class ${it.key}(val group:String,val subGroup:String,val code:String,val char:String,val description:String){\n" +
                                "${
                                    it.value.map { it.value }.joinToString(
                                        ",\n"
                                    )
                                }}"
                    }

                    val joinToString = map.joinToString("\n")
                    //language=kotlin
                    val trimIndent =
                        """@Suppress("unused")
class Emojis {
${joinToString}
}"""
                    asFile.writeText(trimIndent)

                }
            })
    }
}

//  { task: Task? -> println("Hello Gradle!") }
