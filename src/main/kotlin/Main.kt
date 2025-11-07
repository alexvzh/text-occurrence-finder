@file:Suppress("ktlint:standard:no-wildcard-imports")

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

interface Occurrence {
    val file: Path
    val line: Int
    val offset: Int
}

data class TextOccurrence(
    override val file: Path,
    override val line: Int,
    override val offset: Int,
) : Occurrence

fun searchForTextOccurrences(
    stringToSearch: String,
    directory: Path,
): Flow<Occurrence> =
    channelFlow {
        require(Files.isDirectory(directory)) {
            "Provided path is not a directory: $directory"
        }

        val files =
            Files
                .walk(directory)
                .filter { Files.isRegularFile(it) }
                .toList()

        files.forEach { file ->
            launch(Dispatchers.IO) {
                try {
                    Files.newBufferedReader(file).useLines { lines ->
                        lines.forEachIndexed { index, line ->
                            var pos = line.indexOf(stringToSearch)
                            while (pos >= 0) {
                                send(TextOccurrence(file, index + 1, pos))
                                pos = line.indexOf(stringToSearch, pos + 1)
                            }
                        }
                    }
                } catch (e: MalformedInputException) {
                    println("Skipping unreadable file: $file")
                } catch (e: Exception) {
                    println("Could not read $file: ${e.message}")
                }
            }
        }
    }.flowOn(Dispatchers.IO)

fun main(args: Array<String>) =
    runBlocking {
        if (args.size < 2) {
            println("Usage: search <directory> <string>")
            return@runBlocking
        }

        val directory = Paths.get(args[0])
        val stringToSearch = args[1]

        println("Searching for \"$stringToSearch\" in $directory...")

        searchForTextOccurrences(stringToSearch, directory)
            .collect { occurrence ->
                println("Found in ${occurrence.file} (line ${occurrence.line}, offset ${occurrence.offset})")
            }

        println("Search completed.")
    }
