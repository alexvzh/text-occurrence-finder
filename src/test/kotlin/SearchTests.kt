@file:Suppress("ktlint:standard:no-wildcard-imports")

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.*

class SearchTests {
    @Test
    fun `finds all occurrences of a string in files`() =
        runTest {
            val tempDir = Files.createTempDirectory("search-basic")
            val file = tempDir.resolve("file.txt")
            file.writeText("Hello Kotlin\nKotlin rocks\n")

            val results = searchForTextOccurrences("Kotlin", tempDir).toList()

            assertEquals(2, results.size)
            assertEquals(file, results[0].file)
            assertEquals(1, results[0].line)
        }

    @Test
    fun `returns empty flow when no matches found`() =
        runTest {
            val tempDir = Files.createTempDirectory("search-empty")
            val file = tempDir.resolve("data.txt")
            file.writeText("Nothing to see here")

            val results = searchForTextOccurrences("Kotlin", tempDir).toList()

            assertEquals(0, results.size)
        }

    @Test
    fun `finds matches in nested directories`() =
        runTest {
            val root = Files.createTempDirectory("search-nested")
            val subdir = Files.createDirectories(root.resolve("sub"))
            val file1 = root.resolve("a.txt").apply { writeText("Find me") }
            val file2 = subdir.resolve("b.txt").apply { writeText("Find me too") }

            val results = searchForTextOccurrences("Find", root).toList()

            assertEquals(2, results.size)
            assertEquals(setOf(file1, file2), results.map { it.file }.toSet())
        }

    @Test
    fun `skips unreadable binary files without crashing`() =
        runTest {
            val tempDir = Files.createTempDirectory("search-binary")
            val textFile = tempDir.resolve("good.txt").apply { writeText("Target word here") }
            val binaryFile =
                tempDir.resolve("bad.bin").apply {
                    // Write invalid UTF-8 bytes
                    writeBytes(byteArrayOf(0xC3.toByte(), 0x28))
                }

            val results = searchForTextOccurrences("Target", tempDir).toList()

            // Should still find the text in good.txt, and skip bad.bin gracefully
            assertEquals(1, results.size)
            assertEquals(textFile, results.first().file)
        }

    @Test
    fun `throws error if path is not a directory`() =
        runTest {
            val tempFile = Files.createTempFile("not-a-dir", ".txt")

            val error =
                runCatching {
                    searchForTextOccurrences("Test", tempFile).toList()
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertTrue(error.message!!.contains("not a directory"))
        }

    @Test
    fun `handles multiple files concurrently`() =
        runTest {
            val tempDir = Files.createTempDirectory("search-concurrent")

            repeat(20) { i ->
                val file = tempDir.resolve("file$i.txt")
                file.writeText("Target word in file $i\n")
            }

            val results = searchForTextOccurrences("Target", tempDir).toList()

            assertEquals(20, results.size)
        }

    @Test
    fun `reads all files and skips only unreadable ones`() =
        runTest {
            val tempDir = Files.createTempDirectory("search-all-files")
            tempDir.resolve("good.txt").writeText("Find me")
            tempDir.resolve("fake.png").writeText("Find me too") // readable text file, should be read
            tempDir.resolve("broken.bin").writeBytes(byteArrayOf(0xC3.toByte(), 0x28)) // unreadable

            val results = searchForTextOccurrences("Find", tempDir).toList()

            // Both text-containing files should be read, broken.bin skipped
            assertEquals(2, results.size)
        }
}
