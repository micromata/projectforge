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

package org.projectforge.business.address

import jakarta.persistence.LockModeType
import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class PersonalAddressDao {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var userRights: UserRightService

    private var personalAddressCache: PersonalAddressCache? = null
        get() {
            if (field == null) {
                ApplicationContextProvider.getApplicationContext()?.let {
                    // ApplicationContext might be null on startup phase.
                    field = it.getBean(PersonalAddressCache::class.java)
                }
            }
            return field
        }

    @Transient
    private var addressbookRight: AddressbookRight? = null

    /**
     * @param personalAddress
     * @param ownerId         If null, then task will be set to null;

     */
    fun setOwner(personalAddress: PersonalAddressDO, ownerId: Long) {
        val user = userDao.getOrLoad(ownerId)
        personalAddress.owner = user
    }

    /**
     * @param obj
     * @return the generated identifier.
     */
    fun saveOrUpdate(obj: PersonalAddressDO): Serializable? {
        if (internalUpdateNewTrans(obj)) {
            return obj.id
        }
        return internalSave(obj)
    }

    private fun checkAccess(obj: PersonalAddressDO, throwException: Boolean = true): Boolean {
        requireNotNull(obj.ownerId)
        requireNotNull(obj.addressId)
        val owner = user
        if (owner == null || owner.id != obj.ownerId) {
            if (throwException) {
                throw AccessException("address.accessException.userIsNotOwnerOfPersonalAddress")
            }
            return false
        }
        val addressbookIDListForUser = getAddressbookIdsForUser(owner)
        //     Set<Integer> addressbookIDListFromAddress = obj.getAddress().getAddressbookList().stream().mapToInt(AddressbookDO::getId).boxed()
        //        .collect(Collectors.toSet());
        val addressbookIDListFromAddress = obj.address?.addressbookList?.map { it.id }?.toSet() ?: return false
        if (Collections.disjoint(addressbookIDListForUser, addressbookIDListFromAddress)) {
            if (throwException) {
                throw AccessException("address.accessException.userHasNoRightForAddressbook")
            }
            return false
        }
        accessChecker.checkRestrictedOrDemoUser()
        return true
    }

    private fun getAddressbookIdsForUser(user: PFUserDO): Set<Long?> {
        val abIdSet: MutableSet<Long?> = HashSet()
        //Get all addressbooks for user
        if (addressbookRight == null) {
            addressbookRight = userRights.getRight(UserRightId.MISC_ADDRESSBOOK) as AddressbookRight
        }
        abIdSet.add(AddressbookDao.GLOBAL_ADDRESSBOOK_ID)
        for (ab in addressbookDao.internalLoadAll()) {
            if (!ab.deleted && addressbookRight!!.hasSelectAccess(user, ab)) {
                abIdSet.add(ab.id)
            }
        }
        return abIdSet
    }

    private fun internalSave(obj: PersonalAddressDO): Serializable? {
        if (isEmpty(obj)) {
            // No entry, so we do not need to save this entry.
            return null
        }
        checkAccess(obj)
        obj.setCreated()
        obj.setLastUpdate()
        persistenceService.insert(obj)
        personalAddressCache!!.setAsExpired(obj.ownerId!!)
        log.info("New object added (${obj.id}): $obj")
        return obj.id
    }

    /**
     * Will be called, before an address is (forced) deleted. All references in personal address books have to be deleted first.
     *
     * @param addressDO
     */
    fun internalDeleteAll(addressDO: AddressDO, context: PfPersistenceContext) {
        val counter = context.executeNamedUpdate(
            PersonalAddressDO.DELETE_ALL_BY_ADDRESS_ID,
            Pair("addressId", addressDO.id),
        )
        if (counter > 0) {
            log.info("Removed #$counter personal address book entries of deleted address: $addressDO")
        }
    }

    private fun isEmpty(obj: PersonalAddressDO): Boolean {
        return !obj.isFavoriteCard
    }

    /**
     * @param obj
     * @return true, if already existing entry was updated, otherwise false (e. g. if no entry exists for update).
     */
    private fun internalUpdateNewTrans(obj: PersonalAddressDO): Boolean {
        return persistenceService.runInTransaction { context ->
            var dbObj: PersonalAddressDO? = null
            if (obj.id != null) {
                dbObj = context.selectById(
                    PersonalAddressDO::class.java,
                    obj.id,
                    attached = true,
                    lockModeType = LockModeType.PESSIMISTIC_WRITE,
                )
            }
            if (dbObj == null) {
                dbObj = getByAddressId(obj.addressId, requiredLoggedInUser, attached = true)
            }
            if (dbObj == null) {
                return@runInTransaction false
            }
            checkAccess(dbObj)
            Validate.isTrue(dbObj.addressId == obj.addressId)
            obj.id = dbObj.id
            // Copy all values of modified user to database object.
            val modified = dbObj.copyValuesFrom(obj, "owner", "address", "id")
            if (modified == EntityCopyStatus.MAJOR) {
                dbObj.setLastUpdate()
                context.update(dbObj)
                personalAddressCache!!.setAsExpired(dbObj.ownerId!!)
                log.info("Object updated: $dbObj")
            }
            true
        }
    }


    /**
     * @return the PersonalAddressDO entry assigned to the given address for the context user or null, if not exist.
     */
    fun getByAddressId(addressId: Long?): PersonalAddressDO? {
        val owner = user
        return getByAddressId(addressId, owner)
    }

    /**
     * @return the PersonalAddressDO entry assigned to the given address for the context user or null, if not exist.
     */
    fun getByAddressId(addressId: Long?, owner: PFUserDO?): PersonalAddressDO? {
        return getByAddressId(addressId, owner, false)
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     * @return the PersonalAddressDO entry assigned to the given address for the context user or null, if not exist.
     */
    private fun getByAddressId(addressId: Long?, owner: PFUserDO?, attached: Boolean): PersonalAddressDO? {
        requireNotNull(owner?.id)
        return persistenceService.selectNamedSingleResult(
            PersonalAddressDO.FIND_BY_OWNER_AND_ADDRESS_ID,
            PersonalAddressDO::class.java,
            Pair("ownerId", owner!!.id),
            Pair("addressId", addressId),
            attached = attached,
            errorMessage = "Multiple personal address book entries for same user (${owner.id} and same address ($addressId). Should not occur?!",
        )
    }

    /**
     * @return the PersonalAddressDO entry assigned to the given address for the context user or null, if not exist.
     */
    fun getByAddressUid(addressUid: String): PersonalAddressDO? {
        val owner = requiredLoggedInUser
        return persistenceService.selectNamedSingleResult(
            PersonalAddressDO.FIND_BY_OWNER_AND_ADDRESS_UID,
            PersonalAddressDO::class.java,
            Pair("ownerId", owner.id),
            Pair("addressUid", addressUid),
            errorMessage = "Multiple personal address book entries for same user (${owner.id} and same address ($addressUid). Should not occur?!",
        )
    }

    val list: List<PersonalAddressDO>
        /**
         * @return the list of all PersonalAddressDO entries for the context user.
         */
        get() {
            val owner = requiredLoggedInUser
            val start = System.currentTimeMillis()
            val list = persistenceService.namedQuery(
                PersonalAddressDO.FIND_JOINED_BY_OWNER,
                PersonalAddressDO::class.java,
                Pair("ownerId", owner.id),
            )
            log.info("PersonalDao.getList took " + (System.currentTimeMillis() - start) + "ms for user " + owner.id + ".")
            return list.filter { pa -> checkAccess(pa, false) }
        }

    val favoriteAddressIdList: List<Long>
        /**
         * @return the list of all PersonalAddressDO entries for the context user without any check access (addresses might be also deleted).
         */
        get() {
            val owner = requiredLoggedInUser
            return persistenceService.namedQuery(
                PersonalAddressDO.FIND_FAVORITE_ADDRESS_IDS_BY_OWNER,
                Long::class.java,
                Pair("ownerId", owner.id)
            )
        }

    val personalAddressByAddressId: Map<Long, PersonalAddressDO>
        /**
         * @return the list of all address ids of personal address book for the context user (isFavorite() must be true).
         * @see PersonalAddressDO.isFavorite
         */
        get() {
            val owner = requiredLoggedInUser
            val list = persistenceService.namedQuery(
                PersonalAddressDO.FIND_BY_OWNER,
                PersonalAddressDO::class.java,
                Pair("ownerId", owner.id),
            )
            val result = mutableMapOf<Long, PersonalAddressDO>()
            for (entry in list) {
                if (entry.isFavorite && checkAccess(entry, false)) {
                    entry.addressId?.let { addressId ->
                        result[addressId] = entry
                    }
                }
            }
            return result
        }

    val myAddresses: List<AddressDO>
        /**
         * @return the list of all address entries for the context user (isFavorite() must be true).
         * @see PersonalAddressDO.isFavorite
         */
        get() {
            requiredLoggedInUser
            val list = list
            val result = mutableListOf<AddressDO>()
            for (entry in list) {
                if (entry.isFavorite && checkAccess(entry, false)) {
                    entry.address?.let { address -> result.add(address) }
                }
            }
            return result
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PersonalAddressDao::class.java)
    }
}
