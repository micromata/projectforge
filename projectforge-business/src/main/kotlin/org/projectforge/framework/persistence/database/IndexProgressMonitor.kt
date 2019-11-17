/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.framework.persistence.database

import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class IndexProgressMonitor(private val logPrefix: String, private val totalNumber: Long) : SimpleIndexingProgressMonitor() {
    @Volatile
    private var done: Long = 0
    private var blockCounter: Long = 0
    private var progressSteps: Long = 0
    override fun documentsAdded(increment: Long) {
        synchronized(this) {
            blockCounter += increment
            done += increment
            if (blockCounter > progressSteps) {
                printStatusMessage(totalNumber, done)
                blockCounter -= progressSteps
            }
        }
    }

    fun printStatusMessage(totalTodoCount: Long, doneCount: Long) {
        val format = NumberFormat.getInstance(Locale.US)
        val percentage = BigDecimal(doneCount).multiply(NumberHelper.HUNDRED).divide(BigDecimal(totalTodoCount), 0, RoundingMode.HALF_UP)
        log.info("$logPrefix: Progress: ${percentage}% (${format.format(doneCount)}/${format.format(totalTodoCount)})")
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexProgressMonitor::class.java)
    }

    init {
        progressSteps = if (totalNumber > 5000000) // 1.000.000
            totalNumber / 100 // Log message every 1%
        else if (totalNumber > 1000000) // 1.000.000
            totalNumber / 10 // Log message every 10%
        else if (totalNumber > 100000) // 100.000
            totalNumber / 5 // Log message every 20%
        else if (totalNumber > 10000) // 10.000
            totalNumber / 2 // Log message every 50%
        else 2 * totalNumber // Do not log.
    }
}
