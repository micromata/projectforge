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

package org.projectforge.rest.fibu.importer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free, JSON returning employee-salary (Gehaltsimport) xlsx import, reachable as a button in the
 * migrated salary list rather than as its own menu entry. It is a thin concrete subclass of
 * [AbstractImportRest]: the xlsx parsing, the access right and the job enqueueing are lifted from the legacy
 * Wicket/Merlin import, everything else — the endpoints and the [org.projectforge.rest.importer.ImportView]
 * wire shape — lives in the base. Vorbild: [IncomingInvoiceImportRest].
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/employeeSalaryImport")
class EmployeeSalaryImportRest :
    AbstractImportRest<EmployeeSalaryImportDTO, EmployeeSalaryImportStorage>() {

    @Autowired
    private lateinit var employeeSalaryDao: EmployeeSalaryDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val fileExtensions = arrayOf("xlsx", "xls")

    override val maxFileUploadSizeMB = 10L // in MB

    override fun checkRight() {
        // The real DAO right is HR_EMPLOYEE_SALARY (READWRITE); the legacy Wicket page gated on the wrong one.
        employeeSalaryDao.checkLoggedInUserInsertAccess(EmployeeSalaryDO())
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): EmployeeSalaryImportStorage {
        val storage = EmployeeSalaryImportStorage()
        storage.filename = filename
        EmployeeSalaryExcelImporter(employeeCache, employeeService).parse(inputStream, storage)
        return storage
    }

    override fun import(
        storage: EmployeeSalaryImportStorage,
        selectedEntries: List<ImportPairEntry<EmployeeSalaryImportDTO>>,
    ): Int {
        log.info { "Enqueueing import of #${selectedEntries.size} employee-salary entries." }
        return jobHandler.addJob(
            EmployeeSalaryImportJob(
                employeeSalaryDao,
                selectedEntries,
                importStorage = storage,
            )
        ).id
    }
}
