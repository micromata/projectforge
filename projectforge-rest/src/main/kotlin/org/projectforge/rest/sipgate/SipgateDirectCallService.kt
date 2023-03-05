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
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.sipgate.SipgateDevice
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
open class SipgateDirectCallService {
  internal class CallData(val callerId: String, val caller: String, val deviceId: String, val callee: String)

  @Autowired
  private lateinit var sipgateConfiguration: SipgateConfiguration

  @Autowired
  internal lateinit var sipgateSyncService: SipgateSyncService

  @Autowired
  private lateinit var sipgateService: SipgateService

  open fun isAvailable(): Boolean {
    return sipgateConfiguration.isConfigured()
  }

  /**
   * Gets the caller id's of the user (by user devices and configured phone id under MyAccount).
   */
  open fun getCallerNumbers(user: PFUserDO): List<String> {
    val devices = sipgateSyncService.getStorage().getUserDevices(user)?.devices
    return getCallerDevices(user, devices)
  }

  /**
   * Gets the caller id's of the user (by user devices and configured phone id under MyAccount.
   * If the basePhoneNumber is given in the configuration, "${basePhoneNumber}0" is prepended (for suppressing phone number).
   */
  open fun getCallerIds(user: PFUserDO): List<String> {
    val list = mutableListOf<String>()
    sipgateConfiguration.basePhoneNumber.let {
      if (it.isNotBlank()) {
        list.add("${it}0")
      }
    }
    val storage = sipgateSyncService.getStorage()
    storage.getUserDevices(user)?.devices?.forEach { device ->
      // Add the caller number
      getCallerId(storage, device)?.let { callerId ->
        if (!list.contains(callerId)) {
          list.add(callerId)
        }
      }
    }
    getPersonalPhoneIdentifiers(user)?.forEach { number ->
      val fullNumber = getFullNumber(number)!!
      if (!list.contains(fullNumber)) {
        list.add(fullNumber)
      }
    }
    return list
  }

  /**
   * @param user user needed for getting user devices.
   * @param callerString Caller string is needed for getting the caller's device.
   * @param callerIdString If given, this id is displayed at the callees device.
   * @param callee Whom to call?
   */
  private fun getCallerData(user: PFUserDO, callerString: String, callerIdString: String?, callee: String): CallData? {
    val storage = sipgateSyncService.getStorage()
    val userDevices = storage.getUserDevices(user)?.devices
    return getCallerData(
      userDevices,
      storage.numbers,
      defaultDevice = sipgateConfiguration.defaultDevice,
      basePhoneNumber = sipgateConfiguration.basePhoneNumber,
      user,
      callerString = callerString,
      callerIdString = callerIdString,
      callee = callee,
    )
  }

  /**
   *
   */
  open fun initCall(user: PFUserDO, callerString: String, callerId: String?, callee: String): Boolean {
    val data =
      getCallerData(user, callerString = callerString, callerIdString = callerId, callee = callee) ?: return false
    log.info { "deviceId=${data.deviceId}, caller=${data.caller}, callerId=${data.callerId}, callee=${data.callee}" }
    return sipgateService.initCall(
      deviceId = data.deviceId,
      caller = data.caller,
      callee = data.callee,
      callerId = data.callerId,
    )
  }

  private fun getCallerId(storage: SipgateDataStorage, device: SipgateDevice): String? {
    val phoneLineId =
      device.activePhonelines?.firstOrNull { it.alias?.contains("routing", ignoreCase = true) != true }?.id
    val number = if (phoneLineId != null) {
      storage.numbers?.firstOrNull { it.endpointId == phoneLineId }
    } else {
      null
    }
    return getFullNumber(number?.localized)
  }

  private fun getFullNumber(number: String?): String? {
    return getFullNumber(sipgateConfiguration.basePhoneNumber, number)
  }

  companion object {
    internal fun getCallerDevices(user: PFUserDO, devices: List<SipgateDevice>?): List<String> {
      val list = mutableListOf<String>()
      devices?.forEach { device ->
        list.add("${device.alias ?: ""} (${device.id})")
      }
      getPersonalPhoneIdentifiers(user)?.let { list.addAll(it) }
      return list
    }

    @JvmStatic
    fun getNormalizedPersonalPhoneIdentifiers(user: PFUserDO): String? {
      return getNormalizedPersonalPhoneIdentifiers(user.personalPhoneIdentifiers)
    }

    @JvmStatic
    fun getNormalizedPersonalPhoneIdentifiers(personalPhoneIdentifiers: String?): String? {
      return getPersonalPhoneIdentifiers(personalPhoneIdentifiers)?.joinToString(",")
    }

    private fun getPersonalPhoneIdentifiers(user: PFUserDO): Array<String>? {
      return getPersonalPhoneIdentifiers(user.personalPhoneIdentifiers)
    }

    private fun getPersonalPhoneIdentifiers(personalPhoneIdentifiers: String?): Array<String>? {
      if (personalPhoneIdentifiers.isNullOrBlank()) {
        return null
      }
      val list = mutableListOf<String>()
      personalPhoneIdentifiers.split(",", ";", "|").forEach { el ->
        if (el.isNotBlank()) {
          list.add(el.trim())
        }
      }
      return list.distinct().toTypedArray()
    }

    /**
     * For testing.
     * @param callerString My phone (number or device string)
     * @param callerIdString My current caller id to display at the callee's phone.
     */
    internal fun getCallerData(
      userDevices: List<SipgateDevice>?,
      numbers: List<SipgateNumber>?,
      defaultDevice: String,
      basePhoneNumber: String?,
      user: PFUserDO,
      callerString: String,
      callerIdString: String?,
      callee: String
    ): CallData? {
      val device = userDevices?.firstOrNull {
        callerString.contains(
          it.alias ?: "###NO_MATCH###"
        ) || callerString.contains("(${it.id})")
      }
      val callerDeviceId = device?.id ?: defaultDevice
      if (callerDeviceId.isBlank()) {
        log.error { "No matching device found (neither user device by '$callee' nor defaultDevice in projectforge.properties." }
        return null
      }
      val phoneLineId =
        device?.activePhonelines?.firstOrNull { it.alias?.contains("routing", ignoreCase = true) != true }?.id
      val number = if (phoneLineId != null) {
        numbers?.firstOrNull { it.endpointId == phoneLineId }
      } else {
        null
      }
      // Number to display to the recipient
      var callerId = callerIdString
      if (callerId.isNullOrBlank()) {
        number?.localized?.trim { it <= ' ' }?.let { localized ->
          if (localized.length < 4) {
            callerId = "${basePhoneNumber}$localized"
          } else {
            callerId = localized
          }
        }
        if (callerId == null) {
          callerId = NumberHelper.formatPhonenumber(callerString)
        }
      }
      val caller = if (number?.endpointId != null) {
        number.endpointId!!
      } else {
        getFullNumber(basePhoneNumber = basePhoneNumber, number = callerString) ?: defaultDevice
      }
      if (callerId == null) {
        log.error { "No caller-id found for user '${user.username}' and callerId='$callerId'." }
        return null
      }
      return CallData(callerId = callerId!!, caller = caller, deviceId = callerDeviceId, callee = callee)
    }

    private fun getFullNumber(basePhoneNumber: String?, number: String?): String? {
      number ?: return null
      val trimmed = number.trim { it <= ' ' }
      return if (trimmed.length < 4) {
        "${basePhoneNumber}$trimmed"
      } else {
        trimmed
      }
    }

  }
}
