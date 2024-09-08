import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class EmojiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("generateEmoji")
            .doLast(Action {
                val httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()

                val req = HttpRequest
                    .newBuilder(URI.create("https://unicode.org/Public/emoji/15.1/emoji-test.txt"))
                    .GET()
                    .setHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.3"
                    )
                    .build()


                val bodyAsText = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body()

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

                var emojiCount = 0
                var groupCount = 0

                data class Emoji(val group: String, val value: String)

                val enumList = mutableMapOf<String, Emoji>()
                //TODO グループ分けしたことで重複がなくなるはずなので修正
                for (s in split) {
                    if (emojiCount >= 99) {
                        emojiCount = 0
                        groupCount++
                    }

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
                            val status = s.substringAfter(";").substringBefore("#").trim()

                            val code =
                                s.substringBefore(";").replace(Regex(" +"), " ").trim()
                            val char = s.substringAfter("# ").substringBefore(" ").trim()

                            val statusString = when (status) {
                                "fully-qualified" -> "Status.FULLY_QUALIFIED"
                                "unqualified" -> "Status.UNAUALIFIED"
                                "minimally-qualified" -> "Status.MINIMALLY_QUALIFIED"

                                else -> {
                                    println(status)
                                    continue
                                }
                            }

                            enumList.putIfAbsent(
                                (description + "_" + status),
                                Emoji(
                                    group + groupCount,
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
                                    }(\"$group\",\"$subgroup\",\"$code\",\"$char\",\"$description\",$statusString)"
                                )
                            )

                            emojiCount++
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
                    "enum class ${it.key}(override val group:String,override val subgroup:String,override val code:String,override val char:String,override val description:String,val status:Status):UnicodeEmoji{\n" +
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
interface UnicodeEmoji {
    val group: String
    val subgroup: String
    val code: String
    val char: String
    val description: String
}

enum class Status{
FULLY_QUALIFIED,
UNAUALIFIED,
MINIMALLY_QUALIFIED
}

object Emojis {
val allEmojis:MutableList<UnicodeEmoji> = mutableListOf<UnicodeEmoji>()
init {
    ${emojis.keys.map { "allEmojis.addAll($it.values())" }.joinToString("\n")}
}
${joinToString}
}"""
                asFile.writeText(trimIndent)


            })
    }
}

//  { task: Task? -> println("Hello Gradle!") }
