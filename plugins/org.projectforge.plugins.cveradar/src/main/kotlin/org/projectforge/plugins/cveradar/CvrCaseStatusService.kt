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

package org.projectforge.plugins.cveradar

import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.projectforge.plugins.cveradar.model.CvrScanFindingDO
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * The closure overview of a case (CLS-02): which devices are fixed by the fix criteria (CLS-01) or confirmed by
 * their user (CLS-04), which scan findings and team checks (CLS-03) are open, and whether the case can be closed.
 */
@Service
class CvrCaseStatusService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    enum class DeviceState {
        /** Proved fixed: agent version or check, and no open scan finding of the case. */
        FIXED,

        /** Proved open: an open scan finding, a failed check or an agent version below the minimum. */
        OPEN,

        /** No evidence, but its user confirmed the measures. */
        CONFIRMED,

        /** No evidence and no confirmation. */
        NO_DATA,
    }

    /** A device with its state for the case. */
    class DeviceStatus(
        val device: CvrDeviceDO,
        val state: DeviceState,
        val openFindings: List<CvrScanFindingDO>,
        val checkPassed: Boolean?,
        val confirmation: CvrConfirmationDO?,
    )

    /** An open item preventing the closing, the i18n key `plugins.cveradar.closure.open.<key>` with the count. */
    class OpenItem(val key: String, val count: Int)

    class CaseStatus(
        val case: CvrCaseDO,
        /** Whether the clients are part of the case: fix criteria for devices or released to the clients. */
        val clientsRelevant: Boolean,
        val devices: List<DeviceStatus>,
        /** Devices not reported within [CveRadarConfig.clientActiveDays], not counted. */
        val inactiveDevices: Int,
        val teamChecks: List<CvrTeamCheckDO>,
        val confirmations: List<CvrConfirmationDO>,
        val openItems: List<OpenItem>,
    ) {
        val closable: Boolean
            get() = openItems.isEmpty()

        fun count(state: DeviceState): Int = devices.count { it.state == state }
    }

    fun status(case: CvrCaseDO, now: Date = Date()): CaseStatus {
        val activeSince = Date(now.time - config.clientActiveDays * MILLIS_PER_DAY)
        val allDevices = deviceService.selectDevices()
        val active = allDevices.filter { it.lastSeen?.before(activeSince) == false }
        val findings = deviceService.selectFindings(active.mapNotNull { it.id }).groupBy { it.deviceId }
        val confirmations = case.id?.let { deviceService.selectConfirmations(it) } ?: emptyList()
        val teamChecks = case.id?.let { teamCheckDao.selectByCase(it) } ?: emptyList()
        return status(case, active, allDevices.size - active.size, findings, confirmations, teamChecks)
    }

    /** The states of the given (active) devices only, e.g. the own ones of a user, without the case's other items. */
    fun deviceStates(
        case: CvrCaseDO,
        activeDevices: List<CvrDeviceDO>,
        openFindingsByDevice: Map<Long?, List<CvrScanFindingDO>>,
    ): List<DeviceStatus> {
        return status(case, activeDevices, 0, openFindingsByDevice, emptyList(), emptyList()).devices
    }

    internal fun status(
        case: CvrCaseDO,
        activeDevices: List<CvrDeviceDO>,
        inactiveDevices: Int,
        openFindingsByDevice: Map<Long?, List<CvrScanFindingDO>>,
        confirmations: List<CvrConfirmationDO>,
        teamChecks: List<CvrTeamCheckDO>,
    ): CaseStatus {
        val pluginIds = CvrCaseDO.splitList(case.scanPluginIds).toSet()
        val cveIds = CvrCaseDO.splitList(case.cveIds).map { it.uppercase() }.toSet()
        val hasScanCriteria = pluginIds.isNotEmpty() || cveIds.isNotEmpty()
        val hasAgentCriteria = !case.minClientAgentVersion.isNullOrBlank() || !case.clientAgentCheckId.isNullOrBlank()
        val clientsRelevant = hasScanCriteria || hasAgentCriteria || case.clientAdvisory
        val confirmationByUser = confirmations.associateBy { it.userId }
        val devices = if (!clientsRelevant) emptyList() else activeDevices.map { device ->
            val open = (openFindingsByDevice[device.id] ?: emptyList()).filter { finding ->
                pluginIds.contains(finding.pluginId) ||
                        CvrCaseDO.splitList(finding.cveIds).any { cveIds.contains(it.uppercase()) }
            }
            val checkPassed = case.clientAgentCheckId?.takeIf { it.isNotBlank() }?.let {
                deviceService.agentChecks(device)[it]?.passed
            }
            val minVersion = case.minClientAgentVersion?.takeIf { it.isNotBlank() }
            val versionFixed = minVersion != null && VersionComparator.isAtLeast(device.agentVersion, minVersion)
            val versionTooLow = minVersion != null && !device.agentVersion.isNullOrBlank() && !versionFixed
            val confirmation = device.userId?.let { confirmationByUser[it] }
            val state = when {
                open.isNotEmpty() || checkPassed == false -> DeviceState.OPEN
                versionFixed || checkPassed == true -> DeviceState.FIXED
                versionTooLow -> DeviceState.OPEN
                // Only scan criteria: no open finding in a scan of the device is the evidence.
                !hasAgentCriteria && hasScanCriteria && device.lastScanSync != null -> DeviceState.FIXED
                confirmation?.answer?.isConfirmed == true -> DeviceState.CONFIRMED
                else -> DeviceState.NO_DATA
            }
            DeviceStatus(device, state, open, checkPassed, confirmation)
        }
        val openItems = mutableListOf<OpenItem>()
        fun add(key: String, count: Int) {
            if (count > 0) openItems.add(OpenItem(key, count))
        }
        add("devicesOpen", devices.count { it.state == DeviceState.OPEN })
        add("devicesNoData", devices.count { it.state == DeviceState.NO_DATA })
        add("teamChecks", teamChecks.count { it.answer == null || it.answer == CvrTeamAnswer.IN_PROGRESS })
        add("riskAccepted", teamChecks.count { it.answer == CvrTeamAnswer.RISK_ACCEPTED })
        add("questions", confirmations.count { it.answer?.isConfirmed == false })
        return CaseStatus(case, clientsRelevant, devices, inactiveDevices, teamChecks, confirmations, openItems)
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 3600 * 1000
    }
}
