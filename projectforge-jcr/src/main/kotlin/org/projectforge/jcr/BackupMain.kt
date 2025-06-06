/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jcr

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

class BackupMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty() || args.size > 2) {
                printHelp()
                return
            }
            val repositoryLocation = checkRepoDir(args[0]) ?: return
            val backupFile =
                    if (args.size == 2) {
                        checkBackupFileWriteable(File(args[1], RepoBackupService.backupFilename).absolutePath)
                    } else {
                        checkBackupFileWriteable(RepoBackupService.backupFilename)
                    }
            backupFile ?: return
            val repoBackupService = prepare(repositoryLocation)
            ZipOutputStream(FileOutputStream(backupFile)).use {
                repoBackupService.backupAsZipArchive(backupFile.name, it)
            }
            shutdown(repoBackupService)
        }

        internal fun prepare(repositoryLocation: File): RepoBackupService {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            loggerContext.getLogger("org.apache").level = Level.INFO
            loggerContext.getLogger("org.projectforge").level = Level.INFO

            val repoService = RepoService()
            val repoBackupService = RepoBackupService()
            repoService.init(repositoryLocation)
            repoBackupService.repoService = repoService
            val jcrCheckSanityJob = JCRCheckSanityCheckJob()
            jcrCheckSanityJob.repoService = repoService
            repoBackupService.jcrCheckSanityJob = jcrCheckSanityJob
            return repoBackupService
        }

        internal fun shutdown(repoBackupService: RepoBackupService) {
            repoBackupService.repoService.shutdown()
        }

        internal fun printHelp() {
            val readme = RepoBackupService::class.java.getResource(RepoBackupService.BACKUP_README).readText()
            println(readme)
        }

        internal fun checkRepoDir(repoPath: String): File? {
            val dir = File(repoPath)
            if (!dir.exists() || !dir.isDirectory()) {
                println("****** Repository dir doesn't exist or isn't a directory: ${dir.absolutePath}")
                printHelp()
                return null
            }
            println("Using repo directory: ${dir.absolutePath}")
            return dir
        }

        internal fun checkBackupFileReadable(filePath: String): File? {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                println("****** Can't read from backup file: ${file.absolutePath}")
                printHelp()
                return null
            }
            println("Using backup archive: ${file.absolutePath}")
            return file
        }

        internal fun checkBackupFileWriteable(filePath: String): File? {
            val file = File(filePath)
            if (file.exists() || file.canWrite()) {
                println("****** Can't write backup file: ${file.absolutePath}")
                printHelp()
                return null
            }
            println("Using backup archive: ${file.absolutePath}")
            return file
        }
    }
}
