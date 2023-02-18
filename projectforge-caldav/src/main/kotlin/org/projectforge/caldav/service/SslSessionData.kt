package org.projectforge.caldav.service

import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * User data of ssl session cache.
 */
class SslSessionData(val httpSessionId: String, val user: PFUserDO)
