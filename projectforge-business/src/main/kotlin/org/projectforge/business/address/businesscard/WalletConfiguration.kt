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


package org.projectforge.business.address.businesscard

import org.projectforge.business.configuration.ConfigurationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File

/**
 * Configuration of the Apple Wallet passes (digital business cards), see [PassKitService]. The Wallet button is only
 * offered if [isConfigured]; the certificate stays on the main instance, never on the gateway.
 */
@Configuration
open class WalletConfiguration {
    /** The Pass Type ID certificate with its private key (.p12). */
    @Value("\${wallet.passcertificate.path:}")
    open var passCertificatePath: String = ""

    @Value("\${wallet.passcertificate.password:}")
    open var passCertificatePassword: String = ""

    /** Apple Worldwide Developer Relations intermediate certificate (G4, .cer, DER or PEM). */
    @Value("\${wallet.wwdrcertificate.path:}")
    open var wwdrCertificatePath: String = ""

    @Value("\${wallet.teamIdentifier:}")
    open var teamIdentifier: String = ""

    @Value("\${wallet.passTypeIdentifier:}")
    open var passTypeIdentifier: String = ""

    /** Shown on the pass (next to the logo) and in the lock screen. */
    @Value("\${wallet.organizationName:}")
    open var organizationName: String = ""

    @Value("\${wallet.backgroundColor:rgb(255, 255, 255)}")
    open var backgroundColor: String = "rgb(255, 255, 255)"

    @Value("\${wallet.foregroundColor:rgb(33, 37, 41)}")
    open var foregroundColor: String = "rgb(33, 37, 41)"

    @Value("\${wallet.labelColor:rgb(108, 117, 125)}")
    open var labelColor: String = "rgb(108, 117, 125)"

    @Autowired(required = false)
    private var configurationService: ConfigurationService? = null

    /** Logo (png) of the pass; blank: the logo of the installation (`projectforge.logoFile`), else the bundled one. */
    @Value("\${wallet.logoFile:}")
    open var logoFile: String = ""

    /**
     * The customized logo: `wallet.logoFile`, else the logo of the installation (`projectforge.logoFile`). Null, if
     * there is none (the pass then shows the bundled ProjectForge logo).
     */
    open fun customLogoFile(): File? {
        logoFile.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.isFile }?.let { return it }
        return configurationService?.takeIf { it.isLogoFileValid }?.logoFileObject
    }

    open fun isConfigured(): Boolean {
        return listOf(passCertificatePath, passCertificatePassword, wwdrCertificatePath, teamIdentifier,
            passTypeIdentifier, organizationName).none { it.isBlank() } &&
                File(passCertificatePath).isFile && File(wwdrCertificatePath).isFile
    }
}
