/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class WebAuthnEntryDao {
    @Autowired
    private lateinit var persistencyService: PfPersistenceService

    fun upsert(entry: WebAuthnEntryDO) {
        val ownerId = entry.owner?.id
        val credentialId = entry.credentialId
        requireNotNull(ownerId) { "Owner must be given for upsert of entry." }
        requireNotNull(credentialId) { "Credential id must be given." }
        require(entry.owner?.id == ThreadLocalUserContext.loggedInUserId)
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

    fun getEntry(ownerId: Long, credentialId: String): WebAuthnEntryDO? {
        return persistencyService.selectNamedSingleResult(
            WebAuthnEntryDO.FIND_BY_OWNER_AND_CREDENTIAL_ID,
            WebAuthnEntryDO::class.java,
            Pair("ownerId", ownerId),
            Pair("credentialId", credentialId),
        )
    }

    /**
     * Checks if the found entry is owned by the logged-in-user and throws an AccessException if not.
     * If the entry isn't found, the same exception will be thrown to prevent any attacker to find valid ids of entries
     * (security paranoia).
     */
    fun getEntryById(id: Long): WebAuthnEntryDO {
        return persistencyService.runReadOnly { context ->
            getEntryById(persistenceContext = context, id = id)
        }
    }

    /**
     * Checks if the found entry is owned by the logged-in-user and throws an AccessException if not.
     * If the entry isn't found, the same exception will be thrown to prevent any attacker to find valid ids of entries
     * (security paranoia).
     */
    private fun getEntryById(
        persistenceContext: PfPersistenceContext,
        id: Long,
        attached: Boolean = false
    ): WebAuthnEntryDO {
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

    fun getEntries(ownerId: Long?): List<WebAuthnEntryDO> {
        if (ownerId == null) {
            return emptyList()
        }
        val loggedInUserId = ThreadLocalUserContext.loggedInUserId
        require(loggedInUserId == null || loggedInUserId == ownerId) { "Can only get WebAuthn entries for logged-in user #$loggedInUserId, not for other user #$ownerId" }
        return persistencyService.executeNamedQuery(
            WebAuthnEntryDO.FIND_BY_OWNER,
            WebAuthnEntryDO::class.java,
            Pair("ownerId", ownerId),
        )
    }

    fun delete(entryId: Long) {
        persistencyService.runInTransaction { context ->
            val entry = getEntryById(context, entryId, true)
            val ownerId = entry.owner?.id
            // Don't tell the user if the WebAuthn entry of a foreign user exists (security paranoia)
            requireNotNull(ownerId) { "Can't delete WebAuthn entry, no such entry found or logged-in-user isn't the owner." }
            require(entry.owner?.id == ThreadLocalUserContext.loggedInUserId) { "Owner is only allowed to delete own WebAuthn entries." }
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
