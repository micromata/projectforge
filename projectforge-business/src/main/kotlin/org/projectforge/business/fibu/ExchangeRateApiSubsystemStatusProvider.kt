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

package org.projectforge.business.fibu

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * The exchange rate API (Frankfurter), tested daily by the [ExchangeRateApiProbeJob]: its state is the one of the
 * last test (unknown until the first one). Not shown, if the test is switched off (cron `-`).
 */
@Component
class ExchangeRateApiSubsystemStatusProvider : SubsystemStatusProvider {
    @Value("\${projectforge.cron.exchangeRateApiProbe:-}")
    private var cron: String? = null

    override val id = "exchangeRateApi"

    override val titleKey = "system.admin.adminErrors.subsystem.exchangeRateApi"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("fibu.exchangeRate."),
        locationPrefixes = listOf("ExchangeRateApi"),
        syncTypes = listOf(ExchangeRateApiProbeJob.SYNC_TYPE),
    )

    override fun status(): SubsystemStatus? {
        if (cron.isNullOrBlank() || cron?.trim() == "-") {
            return null
        }
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail = SubsystemStatus.hostOf(ExchangeRateApiService.BASE_URL))
    }
}
