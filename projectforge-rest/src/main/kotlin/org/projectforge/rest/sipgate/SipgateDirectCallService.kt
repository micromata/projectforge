package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
open class SipgateDirectCallService {
  @Autowired
  internal lateinit var sipgateService: SipgateService

  @Autowired
  internal lateinit var sipgateSyncService: SipgateSyncService

  fun getCallerNumbers(user: PFUserDO): Array<String>? {
    sipgateSyncService.getStorage().getUserDevices(user)
    return null
  }
  
  companion object {
    @JvmStatic
    fun getNormalizedPersonalPhoneIdentifiers(user: PFUserDO): String? {
      return getNormalizedPersonalPhoneIdentifiers(user.personalPhoneIdentifiers)
    }

    @JvmStatic
    fun getNormalizedPersonalPhoneIdentifiers(personalPhoneIdentifiers: String?): String? {
      return getPersonalPhoneIdentifiers(personalPhoneIdentifiers)?.joinToString(",")
    }

    @JvmStatic
    fun getPersonalPhoneIdentifiers(user: PFUserDO): Array<String>? {
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
  }
}
