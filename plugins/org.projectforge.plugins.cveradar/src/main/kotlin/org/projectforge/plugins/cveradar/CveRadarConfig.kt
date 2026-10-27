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

    /** Devices not reported for more days are deleted with their scan findings (EXT-07). */
    var deviceRetentionDays: Int = 365

    /** Name of the PF group of the security officers (CLS-05), empty: nobody closes a case with open items. */
    var securityOfficerGroup: String = ""

    var tenable = Tenable()

    var graylog = Graylog()

    var nvd = Nvd()

    /** Vulnerability scanner Tenable.sc (EXT-01 – EXT-03). */
    class Tenable {
        /** Base url, e.g. `https://tenable.example.org`; empty: inactive. */
        var url: String = ""

        /** API key of a user with read access (Tenable.sc: My Account → API keys). */
        var accessKey: String = ""

        var secretKey: String = ""

        /** Severities imported (Tenable: 0 info, 1 low, 2 medium, 3 high, 4 critical). */
        var severities: String = "2,3,4"

        /**
         * Regular expression with the named groups `serial` and `user`, applied to the name of every asset (asset
         * list) of Tenable.sc. Only matching assets are clients and imported; empty: inactive.
         */
        var assetPattern: String = ""

        fun isConfigured(): Boolean = url.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank() &&
                assetPattern.isNotBlank()
    }

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
}
