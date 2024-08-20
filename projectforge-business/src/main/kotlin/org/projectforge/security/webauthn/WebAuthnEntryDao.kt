/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.impl.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val log = KotlinLogging.logger {}

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class WebAuthnEntryDao {
    @Autowired
    private lateinit var persistencyService: PfPersistenceService

    open fun upsert(entry: WebAuthnEntryDO) {
        val ownerId = entry.owner?.id
        val credentialId = entry.credentialId
        requireNotNull(ownerId) { "Owner must be given for upsert of entry." }
        requireNotNull(credentialId) { "Credential id must be given." }
        require(entry.owner?.id == ThreadLocalUserContext.userId)
        persistencyService.runInTransaction { context ->
            val em = context.em
            val dbObj = findExistingEntry(entry, em)
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
    }

    open fun getEntry(ownerId: Int, credentialId: String): WebAuthnEntryDO? {
        return persistencyService.selectSingleResult(
            WebAuthnEntryDO.FIND_BY_OWNER_AND_CREDENTIAL_ID,
            WebAuthnEntryDO::class.java,
            Pair("ownerId", ownerId),
            Pair("credentialId", credentialId),
            namedQuery = true,
        )
    }

    /**
     * Checks if the found entry is owned by the logged-in-user and throws an AccessException if not.
     * If the entry isn't found, the same exception will be thrown to prevent any attacker to find valid ids of entries
     * (security paranoia).
     */
    open fun getEntryById(id: Int): WebAuthnEntryDO {
        return persistencyService.runReadOnly { context ->
            getEntryById(persistenceContext = context, id = id)
        }
    }

    /**
     * Checks if the found entry is owned by the logged-in-user and throws an AccessException if not.
     * If the entry isn't found, the same exception will be thrown to prevent any attacker to find valid ids of entries
     * (security paranoia).
     */
    private fun getEntryById(persistenceContext: PfPersistenceContext, id: Int, attached: Boolean = false): WebAuthnEntryDO {
        val loggedInUser = ThreadLocalUserContext.requiredLoggedInUser
        val result = persistenceContext.selectSingleResult(
            WebAuthnEntryDO.FIND_BY_ID,
            WebAuthnEntryDO::class.java,
            Pair("id", id),
            attached = attached,
            namedQuery = true,
        )
        if (result == null || result.owner?.id != loggedInUser.id) {
            throw AccessException(loggedInUser, "webauthn.error.userNotOwnerOrEntryDoesnotExist")
        }
        return result
    }

    open fun getEntries(ownerId: Int?): List<WebAuthnEntryDO> {
        if (ownerId == null) {
            return emptyList()
        }
        val loggedInUserId = ThreadLocalUserContext.userId
        require(loggedInUserId == null || loggedInUserId == ownerId) { "Can only get WebAuthn entries for logged-in user #$loggedInUserId, not for other user #$ownerId" }
        return persistencyService.namedQuery(
            WebAuthnEntryDO.FIND_BY_OWNER,
            WebAuthnEntryDO::class.java,
            Pair("ownerId", ownerId),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun delete(entryId: Int) {
        persistencyService.runInTransaction { context ->
            val entry = getEntryById(context, entryId, true)
            val ownerId = entry.owner?.id
            // Don't tell the user if the WebAuthn entry of a foreign user exists (security paranoia)
            requireNotNull(ownerId) { "Can't delete WebAuthn entry, no such entry found or logged-in-user isn't the owner." }
            require(entry.owner?.id == ThreadLocalUserContext.userId) { "Owner is only allowed to delete own WebAuthn entries." }
            val em = context.em
            em.remove(entry)
            em.flush()
        }
    }


    private fun findExistingEntry(entry: WebAuthnEntryDO, em: EntityManager): WebAuthnEntryDO? {
        if (entry.id != null) {
            val dbObj = em.find(WebAuthnEntryDO::class.java, entry.id)
            requireNotNull(dbObj) { "Entry with id given, but doesn't exist in the data base." }
            return dbObj
        }
        return getEntry(entry.owner!!.id!!, entry.credentialId!!)
    }
}
