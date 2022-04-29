/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.security.webauthn

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.utils.SQLHelper.ensureUniqueResult
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

private val log = KotlinLogging.logger {}

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class WebAuthnEntryDao {

  @PersistenceContext
  private lateinit var em: EntityManager

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun upsert(entry: WebAuthnEntryDO) {
    val ownerId = entry.owner?.id
    val credentialId = entry.credentialId
    requireNotNull(ownerId) { "Owner must be given for upsert of entry." }
    requireNotNull(credentialId) { "Credential id must be given." }
    require(entry.owner?.id == ThreadLocalUserContext.getUserId())
    val dbObj = findExistingEntry(entry)
    if (dbObj != null) {
      dbObj.copyDataFrom(entry)
      dbObj.lastUpdate = Date()
      log.info { "Updating webauthn entry for user '${entry.owner?.username}' with credential id '${entry.credentialId}' with displayName='${entry.displayName}'." }
      em.merge(dbObj)
    } else {
      entry.created = Date()
      entry.lastUpdate = entry.created
      log.info { "Storing new webauthn entry for user '${entry.owner?.username}' with credential id '${entry.credentialId}' with displayName='${entry.displayName}'." }
      em.persist(entry)
    }
    em.flush()
  }

  open fun getEntry(ownerId: Int, credentialId: String): WebAuthnEntryDO? {
    return ensureUniqueResult<WebAuthnEntryDO>(
      em.createNamedQuery(WebAuthnEntryDO.FIND_BY_OWNER_AND_CREDENTIAL_ID, WebAuthnEntryDO::class.java)
        .setParameter("ownerId", ownerId)
        .setParameter("credentialId", credentialId)
    )
  }

  open fun getEntries(ownerId: Int?): List<WebAuthnEntryDO> {
    if (ownerId == null) {
      return emptyList()
    }
    val loggedInUserId = ThreadLocalUserContext.getUserId()
    require(loggedInUserId == null || loggedInUserId == ownerId) { "Can only get WebAuthn entries for logged-in user #$loggedInUserId, not for other user #$ownerId" }
    return em.createNamedQuery(WebAuthnEntryDO.FIND_BY_OWNER, WebAuthnEntryDO::class.java)
      .setParameter("ownerId", ownerId)
      .resultList
  }

  private fun findExistingEntry(entry: WebAuthnEntryDO): WebAuthnEntryDO? {
    if (entry.id != null) {
      val dbObj = em.find(WebAuthnEntryDO::class.java, entry.id)
      requireNotNull(dbObj) { "Entry with id given, but doesn't exist in the data base." }
      return dbObj
    }
    return getEntry(entry.owner!!.id, entry.credentialId!!)
  }
}
