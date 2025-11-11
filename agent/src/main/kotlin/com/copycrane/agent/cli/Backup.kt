package com.copycrane.agent.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import org.intellij.lang.annotations.Language
import java.net.HttpURLConnection
import java.net.URI

class Backup : CliktCommand() {
    private val source by option("-i")
        .file(mustExist = true, canBeDir = true, canBeFile = false, canBeSymlink = true)
        .required()
        .help("Source directory to back up")
    private val destination by option("-o")
        .required()
        .help("Destination directory to back up, can be remote directory as per what rsync supports")
    private val mountPoint by option("-m")
        .file(canBeDir = true, canBeFile = false, canBeSymlink = true)
        .help("Mount point for remote destination")
    private val webhook by option("-w")
        .required()
        .help("Webhook URL to send HTTP POST request to on backup start and end")

    override fun run() {
        try {
            with(Runtime.getRuntime()) {
                val mountPoint = mountPoint
                if (mountPoint != null) {
                    if (exists("findmnt")) {
                        val p = exec(arrayOf("findmnt", "-M", "${mountPoint.canonicalFile}"))
                        p.inputReader().forEachLine {
                            println(it)
                        }
                        check(p.waitFor() == 0) {
                            "Mount point was specified and is not mounted"
                        }
                    } else {
                        System.err.println("findmnt not found, skipping mount point check(!)")
                    }
                }
                check(exists("rsync")) {
                    "rsync not found"
                }

                val p = exec(
                    arrayOf(
                        "rsync",
                        "-avh",
                        "--delete",
                        "--progress",
                        "${source.canonicalFile}",
                        destination
                    )
                )
                p.inputReader().forEachLine {
                    println(it)
                }
                check(p.waitFor() == 0) {
                    "Rsync failed, see output"
                }
            }
            send("✅ Backup done", true)
        } catch (e: Throwable) {
            send("⚠️ Backup failed", false)
            throw e
        }
    }

    fun exists(program: String) = Runtime.getRuntime().exec(arrayOf("which", program)).waitFor() == 0

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    fun send(message: String, success: Boolean) {
        URI(webhook).toURL().openConnection().apply {
            this as HttpURLConnection
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            outputStream.writer(Charsets.UTF_8).use {
                val input = escapeJson(source.path)
                val output = escapeJson(destination)
                val color = if (success) "good" else "danger"
                @Language("JSON")
                val text = """{
                        "attachments": [{
                            "title": "$message",
                            "color": "$color",
                            "fields": [
                                {"title": "Input", "value": "$input", "short": false},
                                {"title": "Output", "value": "$output", "short": false}
                            ]
                        }]
                    }""".trimIndent()
                it.write(text)
            }
            if (responseCode >= 300) {
                val error = errorStream?.reader()?.readText()
                System.err.println("Webhook failed: $responseCode $responseMessage\n$error")
            }
        }
    }
}