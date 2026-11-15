/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////


package org.projectforge.framework.renderer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

/**
 * Converts Word documents to PDF/A-3 with a locally installed LibreOffice (`soffice --headless`).
 *
 * Its layout is much closer to Word's than xdocreport's (line spacing, positioned frames in headers), so it is
 * preferred where installed. Callers fall back to xdocreport if [convertDocxToPdf] returns null.
 *
 * Fonts: on Linux, LibreOffice gets the ProjectForge fonts directory via a fontconfig file (`FONTCONFIG_FILE`),
 * besides the fonts of the OS. On macOS LibreOffice ignores fontconfig, there the fonts have to be installed in
 * the OS (e.g. `~/Library/Fonts`).
 *
 * All conversions share one LibreOffice profile (its first creation takes a few seconds), so they are serialized.
 */
@Service
open class LibreOfficeService(
    private val configurationService: ConfigurationService,
    @param:Value("\${projectforge.libreoffice.path:}") private val configuredPath: String?,
) {
    private val lock = ReentrantLock()

    private val executable: File? by lazy { findExecutable() }

    private val workDir: File by lazy { createWorkDir() }

    open val isAvailable: Boolean
        get() = executable != null

    /**
     * @return The PDF/A-3, or null if LibreOffice isn't available or the conversion failed (logged).
     */
    open fun convertDocxToPdf(docx: ByteArray): ByteArray? = convertDocxToPdf(listOf(docx))?.first()

    /**
     * Converts several Word documents in one LibreOffice run, which saves its start for each document (serial
     * documents of Merlin).
     * @return The PDF/A-3 documents in the order of the given ones (null for a document LibreOffice didn't convert),
     * or null if LibreOffice isn't available or the run failed (logged).
     */
    open fun convertDocxToPdf(docxList: List<ByteArray>): List<ByteArray?>? {
        if (docxList.isEmpty()) {
            return emptyList()
        }
        val soffice = executable ?: return null
        return lock.withLock {
            val jobDir = Files.createTempDirectory(workDir.toPath(), "job").toFile()
            try {
                convert(soffice, jobDir, docxList)
            } catch (ex: Exception) {
                log.error(ex) { "Converting Word document(s) with LibreOffice failed: ${ex.message}" }
                null
            } finally {
                jobDir.deleteRecursively()
            }
        }
    }

    private fun convert(soffice: File, jobDir: File, docxList: List<ByteArray>): List<ByteArray?>? {
        val baseNames = docxList.indices.map { "document-${it + 1}" }
        val inputs = docxList.mapIndexed { index, docx ->
            File(jobDir, "${baseNames[index]}.docx").also { it.writeBytes(docx) }
        }
        val outDir = File(jobDir, "out")
        val logFile = File(jobDir, "soffice.log")
        val started = System.currentTimeMillis()
        val process = ProcessBuilder(
            listOf(
                soffice.absolutePath,
                "-env:UserInstallation=${File(workDir, "profile").toPath().toUri()}",
                "--headless", "--norestore", "--nolockcheck", "--nodefault",
                "--convert-to", PDF_A3_FILTER,
                "--outdir", outDir.absolutePath,
            ) + inputs.map { it.absolutePath }
        ).directory(jobDir).redirectErrorStream(true).redirectOutput(logFile).also {
            it.environment()["FONTCONFIG_FILE"] = File(workDir, "fonts.conf").absolutePath
        }.start()
        val timeout = TIMEOUT_SECONDS + (docxList.size - 1) * TIMEOUT_SECONDS_PER_FURTHER_DOCUMENT
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            log.error { "LibreOffice didn't finish within $timeout s, conversion aborted." }
            return null
        }
        val pdfs = baseNames.map { File(outDir, "$it.pdf").takeIf { file -> file.isFile }?.readBytes() }
        if (process.exitValue() != 0 || pdfs.all { it == null }) {
            log.error { "LibreOffice failed (exit code ${process.exitValue()}): ${logFile.readText().trim()}" }
            return null
        }
        val missing = pdfs.count { it == null }
        if (missing > 0) {
            log.error { "LibreOffice didn't convert $missing of ${docxList.size} Word documents: ${logFile.readText().trim()}" }
        }
        log.info { "Converted ${docxList.size - missing} Word document(s) to PDF/A-3 with LibreOffice in ${System.currentTimeMillis() - started} ms." }
        return pdfs
    }

    private fun createWorkDir(): File {
        val dir = Files.createTempDirectory("projectforge-libreoffice").toFile()
        dir.deleteOnExit()
        // Fonts of the OS plus the ProjectForge fonts directory, the cache is kept out of the user's home.
        File(dir, "fonts.conf").writeText(
            """
            |<?xml version="1.0"?>
            |<!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd">
            |<fontconfig>
            |  <include ignore_missing="yes">/etc/fonts/fonts.conf</include>
            |  <dir>${xmlEscape(configurationService.fontsDir ?: "")}</dir>
            |  <cachedir>${xmlEscape(File(dir, "fontconfig-cache").absolutePath)}</cachedir>
            |</fontconfig>
            |""".trimMargin()
        )
        return dir
    }

    private fun findExecutable(): File? {
        val path = configuredPath?.trim()
        if (path.equals("disabled", ignoreCase = true)) {
            log.info { "LibreOffice disabled (projectforge.libreoffice.path=disabled), using the built-in Word-to-PDF converter." }
            return null
        }
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.canExecute()) {
                log.info { "Using LibreOffice '${file.absolutePath}' for Word-to-PDF conversion." }
                return file
            }
            log.error { "LibreOffice '$path' (projectforge.libreoffice.path) isn't executable, using the built-in Word-to-PDF converter." }
            return null
        }
        val pathDirs = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
        val candidates = pathDirs.flatMap { listOf(File(it, "soffice"), File(it, "libreoffice")) } +
                CANDIDATES.map { File(it) } +
                (File("/opt").listFiles { file -> file.name.startsWith("libreoffice") }?.sortedDescending()
                    ?.map { File(it, "program/soffice") }.orEmpty())
        val file = candidates.firstOrNull { it.isFile && it.canExecute() }
        if (file == null) {
            log.info { "No LibreOffice found, using the built-in Word-to-PDF converter. Install LibreOffice or set projectforge.libreoffice.path for a layout closer to Word." }
        } else {
            log.info { "Using LibreOffice '${file.absolutePath}' for Word-to-PDF conversion." }
        }
        return file
    }

    private fun xmlEscape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    companion object {
        private const val TIMEOUT_SECONDS = 120L

        private const val TIMEOUT_SECONDS_PER_FURTHER_DOCUMENT = 30L

        private const val PDF_A3_FILTER = """pdf:writer_pdf_Export:{"SelectPdfVersion":{"type":"long","value":"3"}}"""

        private val CANDIDATES = listOf(
            "/usr/bin/soffice",
            "/usr/lib/libreoffice/program/soffice",
            "/usr/local/bin/soffice",
            "/snap/bin/libreoffice",
            "/Applications/LibreOffice.app/Contents/MacOS/soffice",
        )
    }
}
