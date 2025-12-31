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

package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.business.sipgate.SipgateUserDevices
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

/**
 * Reads numbers, devices, users etc. from the remote Sipgate server.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
open class SipgateSyncService {
  @Autowired
  internal lateinit var sipgateService: SipgateService

  private var privateStorage: SipgateDataStorage? = null

  /**
   * Gets the storage, if exists. If not, the storage will be read from remote Sipgate server.
   * If the existing storage is existing but is outdated, the outdated storage is returned, but
   * an asyn job is started for getting new storage data.
   */
  open fun getStorage(): SipgateDataStorage {
    privateStorage?.let {
      thread(start = true) {
        readStorage() // Re-read if outdated.
      }
      return it
    }
    return readStorage()
  }

  /**
   * Get the data of the remote Sipgate server. Will be cached as file (work/sipgateStorage.json). The data will
   * be re-read after one day automatically.
   * @param If forceSync is set to true, the configuration will be get from remote Sipgate server. Default is false.
   */
  open fun readStorage(forceSync: Boolean = false): SipgateDataStorage {
    val ps = privateStorage
    if (forceSync || ps == null || !ps.uptodate) {
      if (forceSync) {
        log.info { "Reading Sipgate configuration is forced." }
      } else if (ps == null) {
        log.info { "Sipgate configuration doesn't yet exists.." }
      } else {
        log.info { "Sipgate configuration outdated." }
      }
      readFromFileOrGetFromRemote(forceSync)
    }
    return privateStorage!!
  }

  private fun readFromFileOrGetFromRemote(forceSync: Boolean = false) {
    synchronized(this) {
      try {
        if (!forceSync && storageFile.canRead()) {
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
  }

  private fun remoteRead() {
    log.info { "Reading Sipgate data (users, devices etc.)..." }
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
