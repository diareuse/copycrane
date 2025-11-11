package com.copycrane.agent.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

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

    override fun run() {
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
    }

    fun exists(program: String) = Runtime.getRuntime().exec(arrayOf("which", program)).waitFor() == 0
}