/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.business.excel.ExportWorkbook
import org.projectforge.export.ExportJFreeChart
import org.projectforge.export.ExportZipFile
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * For exporting multiple objects by one script you may collect all objects within this zip archive.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class ExportZipArchive {
  private val zipFiles: MutableCollection<ExportZipFile> = LinkedList()

  /**
   * @return the filename
   */
  val filename: String

  constructor() {
    filename = "archive"
  }

  /**
   * @param filename The filename of the zip archive (without extension), default is "archive".
   */
  constructor(filename: String) {
    this.filename = filename
  }

  fun asByteArray(): ByteArray {
    return ByteArrayOutputStream().use { out ->
      write(out)
      out.toByteArray()
    }
  }

  fun write(out: OutputStream) {
    ZipOutputStream(out).use { zipOut ->
      try {
        zipOut.putNextEntry(ZipEntry("$filename/"))
        for (file in zipFiles) {
          val zipEntry = ZipEntry(filename + "/" + file.filename)
          zipOut.putNextEntry(zipEntry)
          file.exportObject?.let { exportObject ->
            if (exportObject is ExportWorkbook) {
              // Older ProjectForge Excel library.
              val xls = exportObject.asByteArray
              if (xls == null || xls.size == 0) {
                log.error("Oups, xls has zero size. Filename: $filename")
                return@let
              }
              zipOut.write(xls)
            } else if (exportObject is ExcelWorkbook) {
              // Newer Merlin Excel library.
              exportObject.use { workbook ->
                val xls: ByteArray = workbook.asByteArrayOutputStream.toByteArray()
                if (xls.isEmpty()) {
                  log.error("Oups, xls has zero size. Filename: $filename")
                  return@let
                }
                zipOut.write(xls)
              }
            } else if (exportObject is ExportJFreeChart) {
              exportObject.write(zipOut)
            } else if (exportObject is String) {
              zipOut.write(exportObject.toByteArray(Charsets.UTF_8))
            } else if (exportObject is ByteArray) {
              zipOut.write(exportObject)
            }
            zipOut.closeEntry()
          }
        }
      } catch (ex: IOException) {
        log.error(ex.message, ex)
        throw RuntimeException(ex)
      }
    }
  }

  fun add(filename: String, exportWorkbook: ExportWorkbook?): ExportZipArchive {
    zipFiles.add(ExportZipFile(filename, exportWorkbook))
    return this
  }

  fun add(exportWorkbook: ExportWorkbook): ExportZipArchive {
    zipFiles.add(ExportZipFile(exportWorkbook.filename, exportWorkbook))
    return this
  }

  fun add(filename: String, excelWorkbook: ExcelWorkbook?): ExportZipArchive {
    zipFiles.add(ExportZipFile(filename, excelWorkbook))
    return this
  }

  fun add(excelWorkbook: ExcelWorkbook): ExportZipArchive {
    zipFiles.add(ExportZipFile(excelWorkbook.filename, excelWorkbook))
    return this
  }

  fun add(filename: String, exportJFreeChart: ExportJFreeChart?): ExportZipArchive {
    zipFiles.add(ExportZipFile(filename, exportJFreeChart))
    return this
  }

  fun add(filename: String, content: String): ExportZipArchive {
    zipFiles.add(ExportZipFile(filename, content))
    return this
  }

  fun add(filename: String, content: ByteArray): ExportZipArchive {
    zipFiles.add(ExportZipFile(filename, content))
    return this
  }

  val files: Collection<ExportZipFile>
    get() = zipFiles

  companion object {
    private val log = LoggerFactory.getLogger(ExportZipArchive::class.java)
  }
}
