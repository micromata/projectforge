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

import org.projectforge.plugins.cveradar.sync.OsvPackages
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * The technical settings of the CVE-Radar (`projectforge.plugin.cveradar.*`), read on start. Everything specific to
 * an installation (hosts, credentials, queries, field names) is given here only; without it the connectors are
 * inactive (EXT-06).
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.plugin.cveradar")
open class CveRadarConfig {
    /** Prefix of the ids of incidents without CVE (IMP-14): `<prefix>-<year>-<n>`, letters and digits only. */
    var caseIdPrefix: String = "INC"

    /** A device not reported for more days is inactive (EXT-07): not counted in the closure overview. */
    var clientActiveDays: Int = 30

    /** Devices not reported for more days are deleted (EXT-07). */
    var deviceRetentionDays: Int = 365

    /**
     * A team check asking every member waits for the members logged in within so many days only (CLS-03): technical
     * and old accounts would keep it open.
     */
    var teamCheckActiveDays: Int = 90

    /** Name of the PF group of the readers (role [CvrRole.READER]), e.g. `cve-read`; empty: nobody. */
    var readGroup: String = ""

    /** Name of the PF group of the security team (role [CvrRole.SECURITY_TEAM]), e.g. `cve-read-write`; empty: nobody. */
    var writeGroup: String = ""

    /** Name of the PF group of the security officers (CLS-05), empty: nobody closes a case with open items. */
    var securityOfficerGroup: String = ""

    var graylog = Graylog()

    var nvd = Nvd()

    var osv = Osv()

    var euvd = Euvd()

    var deptrack = DependencyTrack()

    /** Log server Graylog with the messages of the client agents (EXT-04, EXT-05). */
    class Graylog {
        /** Base url, e.g. `https://graylog.example.org`; empty: inactive. */
        var url: String = ""

        /** Access token of a user with read access to the stream. */
        var token: String = ""

        /** Id of the stream with the messages of the client agents; empty: all streams the user may read. */
        var streamId: String = ""

        /** Search query selecting the messages with the agent version or check results; empty: inactive. */
        var query: String = ""

        /** Period searched, back from now. */
        var rangeSeconds: Long = 24 * 3600

        /** Max number of messages read per sync. */
        var limit: Int = 10000

        /** Field with the user name of the device (mandatory). */
        var userField: String = ""

        /** Field with the serial number of the device, if logged; otherwise devices are matched by user and host. */
        var serialField: String = ""

        /** Field with the host name of the device. */
        var hostField: String = ""

        /** Field with the version of the client agent. */
        var versionField: String = ""

        /** Field with the id of a check of the client agent (EXT-05). */
        var checkIdField: String = ""

        /** Field with the result of the check: `passed` (or `true`, `ok`) means passed, anything else failed. */
        var checkResultField: String = ""

        fun isConfigured(): Boolean = url.isNotBlank() && token.isNotBlank() && query.isNotBlank() &&
                userField.isNotBlank() && (versionField.isNotBlank() || checkIdField.isNotBlank())
    }

    /** Import of the vulnerabilities from the NVD API 2.0 (PF-CVR-IMP). */
    class Nvd {
        /** Imports from the NVD (needs internet access); off: inactive. */
        var enabled: Boolean = false

        var url: String = "https://services.nvd.nist.gov/rest/json/cves/2.0"

        /**
         * The API key of the NVD (raises the rate limit from 5 to 50 requests in 30 s), optional. Never logged and never
         * sent to the frontend.
         */
        var apiKey: String = ""

        /** The first run imports the vulnerabilities modified in these last days (IMP-09). */
        var initialDays: Int = 30

        /** Vulnerabilities with a lower CVSS score aren't imported (IMP-10). */
        var minCvss: Double = 7.0

        override fun toString(): String =
            "Nvd(enabled=$enabled, url=$url, apiKey=${if (apiKey.isBlank()) "<none>" else "***"}, initialDays=$initialDays, minCvss=$minCvss)"
    }

    /**
     * Import of the advisories of OSV.dev for the libraries (IMP-13) from its public bucket: all advisories of an
     * ecosystem once (`all.zip`), afterwards the modified ones (`modified_id.csv`). Nothing of the inventory is sent.
     */
    class Osv {
        /** Imports from OSV.dev (needs internet access); off: inactive. */
        var enabled: Boolean = false

        var url: String = "https://osv-vulnerabilities.storage.googleapis.com"

        /** The OSV keys of the ecosystems imported, comma separated; see [OsvPackages.ECOSYSTEMS]. */
        var ecosystems: String = "Maven,npm,PyPI,NuGet,Packagist,Go,crates.io"

        /**
         * Also the malicious packages (OSV ids `MAL-…`): some hundred thousand advisories, almost all of npm and PyPI;
         * off by default.
         */
        var malicious: Boolean = false

        /** The known ecosystems of [ecosystems]. */
        val ecosystemKeys: List<String>
            get() = ecosystems.split(',').map { it.trim() }.filter { OsvPackages.ecosystem(it) != null }.distinct()

        override fun toString(): String = "Osv(enabled=$enabled, url=$url, ecosystems=$ecosystems, malicious=$malicious)"
    }

    /**
     * Dependency-Track (PF-CVR-DT-MIG), optional: a second detection of the findings in parallel to the own matching
     * (decision D2). Without url and API key inactive, everything else works as without it.
     */
    class DependencyTrack {
        /** Base url of the API server, e.g. `https://dtrack.example.org`; empty: inactive. */
        var url: String = ""

        /** API key of the team of ProjectForge in Dependency-Track. Never logged and never sent to the frontend. */
        var apiKey: String = ""

        /** Secret of the webhook of Dependency-Track (phase 3), optional. Never logged. */
        var webhookSecret: String = ""

        fun isConfigured(): Boolean = url.isNotBlank() && apiKey.isNotBlank()

        override fun toString(): String =
            "DependencyTrack(url=$url, apiKey=${if (apiKey.isBlank()) "<none>" else "***"}, webhookSecret=${if (webhookSecret.isBlank()) "<none>" else "***"})"
    }

    /**
     * Import of the vulnerabilities from the EUVD of the ENISA (IMP-16), public API without key. It adds the EUVD id, the
     * EPSS and the day of the first known exploitation to the vulnerabilities of the NVD, and brings the ones the NVD
     * doesn't have (yet). It has no CPE: a match by the product names of the EUVD is only possible, never sure.
     */
    class Euvd {
        /** Imports from the EUVD (needs internet access); off: inactive. */
        var enabled: Boolean = false

        var url: String = "https://euvdservices.enisa.europa.eu/api"

        /** The first run imports the vulnerabilities published in these last days. */
        var initialDays: Int = 30

        /**
         * Every run reads the publications of these last days again, as the filter of the EUVD is the publication
         * day: a rating or a CVE id comes later.
         */
        var rescanDays: Int = 7

        /** New vulnerabilities with a lower CVSS score (or not rated yet) aren't imported; stored ones are always updated. */
        var minCvss: Double = 7.0

        /** Pause between two requests (the EUVD doesn't document a rate limit). */
        var pauseMillis: Long = 1000

        override fun toString(): String =
            "Euvd(enabled=$enabled, url=$url, initialDays=$initialDays, rescanDays=$rescanDays, minCvss=$minCvss)"
    }
}
