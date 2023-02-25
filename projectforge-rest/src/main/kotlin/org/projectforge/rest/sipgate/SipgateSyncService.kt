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

package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.business.sipgate.SipgateUserDevices
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Reads numbers, devices, users etc. from the remote Sipgate server.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class SipgateSyncService {
  @Autowired
  internal lateinit var sipgateService: SipgateService

  private var privateStorage: SipgateDataStorage? = null


  /**
   * Get the data of the remote Sipgate server. Will be cached as file (work/sipgateStorage.json). The data will
   * be re-read after one day automatically.
   */
  open fun readStorage(): SipgateDataStorage {
    val ps = privateStorage
    if (ps == null || !ps.uptodate) {
      readFromFileOrGetFromRemote()
    }
    return privateStorage!!
  }

  private fun readFromFileOrGetFromRemote() {
    try {
      if (storageFile.canRead()) {
        val json = storageFile.readText()
        log.info { "Reading Sipgate data from '${storageFile.absolutePath}'..." }
        val newStorage = JsonUtils.fromJson(json, SipgateDataStorage::class.java)
        if (newStorage?.uptodate == true) {
          privateStorage = newStorage
          return
        }
        log.info { "Sipgate storage is outdated (older than one day), re-reading..." }
      }
    } catch (ex: Exception) {
      log.error("Error while parsing sipgate storage from '${storageFile.absolutePath}': ${ex.message}", ex)
    }
    remoteRead()
  }

  private fun remoteRead() {
    val newStorage = SipgateDataStorage()
    newStorage.users = sipgateService.getUsers()
    try {
      newStorage.numbers = sipgateService.getNumbers()
    } catch (ex: Exception) {
      log.error("Can't read numbers (may-be no access): ${ex.message}", ex)
    }
    val userDevices = mutableListOf<SipgateUserDevices>()
    newStorage.users?.forEach { user ->
      val devices = sipgateService.getDevices(user)
      if (devices.isNotEmpty()) {
        devices.forEach { it.deleteSecrets() }
        userDevices.add(SipgateUserDevices(user.id, devices))
      }
    }
    newStorage.userDevices = userDevices
    newStorage.addresses = sipgateService.getAddresses()
    privateStorage = newStorage
    log.info { "Writing Sipgate data to '${storageFile.absolutePath}'..." }
    storageFile.writeText(privateStorage.toString())
  }

  companion object {
    private val storageFile by lazy {
      File(ConfigXml.getInstance().workingDirectory, "sipgateStorage.json")
    }
  }
}
