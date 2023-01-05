/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.dvelop

import org.projectforge.framework.configuration.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.*
import java.util.*
import javax.annotation.PostConstruct
import javax.net.ssl.*

/**
 * ProjectForge supports the synchronization from and to a D-velop installation.
 */
@Configuration
open class DvelopConfiguration {
  @Value("\${projectforge.dvelop.baseUri}")
  open lateinit var baseUri: String

  /**
   * The api key for authentication.
   */
  @Value("\${projectforge.dvelop.apiKey}")
  open lateinit var apiKey: String

  /**
   * If given, all entities (TradingPartner) will be assigned to this organistion referred by ID.
   */
  @Value("\${projectforge.dvelop.organizationId}")
  open lateinit var organizationId: String

  /**
   * The id of the TradingPartners' customized field datevKonto.
   */
  @Value("\${projectforge.dvelop.datevKontoFieldId}")
  open lateinit var datevKontoFieldId: String

  @PostConstruct
  private fun postConstruct() {
    TradingPartner.datevKontoFieldId = datevKontoFieldId
    ExtractPFTradingPartners.organizationId = organizationId
  }

  fun isConfigured(): Boolean {
    return baseUri.isNotBlank() && apiKey.isNotBlank()
  }
}
