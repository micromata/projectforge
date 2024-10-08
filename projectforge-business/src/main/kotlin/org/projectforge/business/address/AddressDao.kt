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

import jakarta.annotation.PostConstruct
import jakarta.persistence.criteria.CriteriaBuilder
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.user.UserRightId
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay.Companion.from
import org.projectforge.framework.utils.NumberHelper.extractPhonenumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.Writer
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AddressDao : BaseDao<AddressDO>(AddressDO::class.java) {
    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property)
    }

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var addressbookCache: AddressbookCache

    @Autowired
    private lateinit var userRights: UserRightService

    @Transient
    private var addressbookRight: AddressbookRight? = null

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    private lateinit var addressCache: AddressCache

    private lateinit var birthdayCache: BirthdayCache

    protected open val deletionListeners: MutableList<AddressDeletionListener> = ArrayList()

    init {
        isForceDeletionSupport = true
    }

    @PostConstruct
    private fun postConstruct() {
        addressCache = AddressCache(this)
        birthdayCache = BirthdayCache(this)
    }

    fun register(listener: AddressDeletionListener) {
        synchronized(deletionListeners) {
            deletionListeners.add(listener)
        }
    }

    val usedCommunicationLanguages: List<Locale>
        get() {
            return persistenceService.runReadOnly { context ->
                val em = context.em
                val cb: CriteriaBuilder = em.getCriteriaBuilder()
                val cr =
                    cb.createQuery(Locale::class.java)
                val root = cr.from(doClass)
                cr.select(root.get("communicationLanguage")).where(
                    cb.equal(root.get<Any>("deleted"), false),
                    cb.isNotNull(root.get<Any>("communicationLanguage"))
                )
                    .orderBy(cb.asc(root.get<Any>("communicationLanguage")))
                    .distinct(true)
                //    "select distinct a.communicationLanguage from AddressDO a where deleted=false and a.communicationLanguage is not null order by a.communicationLanguage");
                em.createQuery(cr).getResultList()
            }
        }

    /**
     * Get the newest address entries (by time of creation).
     */
    fun getNewest(filter: BaseSearchFilter, context: PfPersistenceContext): List<AddressDO> {
        val queryFilter = QueryFilter()
        queryFilter.addOrder(desc("created"))
        addAddressbookRestriction(queryFilter, null)
        if (filter.maxRows > 0) {
            filter.isSortAndLimitMaxRowsWhileSelect = true
        }
        return getList(queryFilter, context)
    }

    @Throws(AccessException::class)
    override fun getList(filter: QueryFilter, context: PfPersistenceContext): List<AddressDO> {
        val filters: MutableList<CustomResultFilter<AddressDO>> = ArrayList()
        if (filter.getExtendedBooleanValue("doublets")) {
            filters.add(DoubletsResultFilter())
        }
        if (filter.getExtendedBooleanValue("favorites")) {
            filters.add(FavoritesResultFilter(personalAddressDao))
        }
        return super.getList(filter, filters, context)
    }

    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<AddressDO> {
        val myFilter = if (filter is AddressFilter) {
            filter
        } else {
            AddressFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (StringUtils.isBlank(myFilter.searchString)) {
            if (!myFilter.isDeleted) {
                if (myFilter.isNewest) {
                    return getNewest(myFilter, context)
                }
                if (myFilter.isMyFavorites) {
                    // Show only favorites.
                    return personalAddressDao.myAddresses
                }
            }
        } else {
            if (StringUtils.isNumeric(filter.searchString)) {
                myFilter.setSearchString("*" + myFilter.searchString + "*")
            }
        }
        if (myFilter.isFilter) {
            // Proceed contact status:
            // Use filter only for non deleted entries:
            if (myFilter.isActive
                || myFilter.isNonActive
                || myFilter.isUninteresting
                || myFilter.isDeparted
                || myFilter.isPersonaIngrata
            ) {
                val col: MutableCollection<ContactStatus?> = ArrayList()
                if (myFilter.isActive) {
                    col.add(ContactStatus.ACTIVE)
                }
                if (myFilter.isNonActive) {
                    col.add(ContactStatus.NON_ACTIVE)
                }
                if (myFilter.isUninteresting) {
                    col.add(ContactStatus.UNINTERESTING)
                }
                if (myFilter.isDeparted) {
                    col.add(ContactStatus.DEPARTED)
                }
                if (myFilter.isPersonaIngrata) {
                    col.add(ContactStatus.PERSONA_INGRATA)
                }
                queryFilter.add(isIn<Any>("contactStatus", col))
            }

            // Proceed address status:
            // Use filter only for non deleted books:
            if (myFilter.isUptodate || myFilter.isOutdated || myFilter.isLeaved) {
                val col: MutableCollection<AddressStatus?> = ArrayList()
                if (myFilter.isUptodate) {
                    col.add(AddressStatus.UPTODATE)
                }
                if (myFilter.isOutdated) {
                    col.add(AddressStatus.OUTDATED)
                }
                if (myFilter.isLeaved) {
                    col.add(AddressStatus.LEAVED)
                }
                queryFilter.add(isIn<Any>("addressStatus", col))
            }

            //Add addressbook restriction
            addAddressbookRestriction(queryFilter, myFilter)
        }
        queryFilter.addOrder(asc("name"))
        val result = getList(queryFilter, context)
        if (myFilter.isDoublets) {
            return filterDoublets(result)
        }
        return result
    }

    private fun filterDoublets(result: List<AddressDO>): List<AddressDO> {
        val fullnames = HashSet<String>()
        val doubletFullnames = HashSet<String>()
        for (address in result) {
            val fullname = getNormalizedFullname(address)
            if (fullnames.contains(fullname)) {
                doubletFullnames.add(fullname)
            }
            fullnames.add(fullname)
        }
        val doublets: MutableList<AddressDO> = ArrayList()
        for (address in result) {
            if (doubletFullnames.contains(getNormalizedFullname(address))) {
                doublets.add(address)
            }
        }
        return doublets
    }

    private fun addAddressbookRestriction(queryFilter: QueryFilter, addressFilter: AddressFilter?) {
        //Addressbook rights check
        val abIdList = mutableSetOf<Long>()
        //First check wicket ui addressbook filter
        if (addressFilter != null && addressFilter.addressbooks != null && addressFilter.addressbooks.size > 0) {
            abIdList.addAll(addressFilter.addressbookIds)
        } else {
            //Global addressbook is selectable for every one
            abIdList.add(AddressbookDao.GLOBAL_ADDRESSBOOK_ID)
            //Get all addressbooks for user
            if (addressbookRight == null) {
                addressbookRight = userRights.getRight(UserRightId.MISC_ADDRESSBOOK) as AddressbookRight
            }
            for (ab in addressbookDao.internalLoadAll()) {
                if (!ab.deleted && addressbookRight!!.hasSelectAccess(loggedInUser, ab)) {
                    ab.id?.let {
                        abIdList.add(it)
                    }
                }
            }
        }
        //Has to be on id value, full entity doesn't work!!!
        queryFilter.createJoin("addressbookList")
        queryFilter.add(isIn<Any>("addressbookList.id", abIdList))
    }

    override fun hasAccess(
        user: PFUserDO, obj: AddressDO?, oldObj: AddressDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        if (addressbookRight == null) {
            addressbookRight = userRights.getRight(UserRightId.MISC_ADDRESSBOOK) as AddressbookRight
        }
        if (obj?.addressbookList == null) {
            //Nothing to check, should not happen, but does
            return true
        }
        when (operationType) {
            OperationType.SELECT -> {
                for (ab in addressCache.getAddressbooks(obj)!!) {
                    if (addressbookRight!!.checkGlobal(ab) || addressbookRight!!.getAccessType(ab, user.id)
                            .hasAnyAccess()
                    ) {
                        return true
                    }
                }
                if (throwException) {
                    throw AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType)
                }
                return false
            }

            OperationType.INSERT, OperationType.UPDATE, OperationType.DELETE -> {
                for (ab in obj.addressbookList!!) {
                    if (addressbookRight!!.checkGlobal(ab) || addressbookRight!!.hasFullAccess(
                            addressbookCache.getAddressbook(
                                ab
                            ), user.id
                        )
                    ) {
                        return true
                    }
                }
                if (throwException) {
                    throw AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType)
                }
                return false
            }

            else -> {
                if (throwException) {
                    throw AccessException(user, "access.exception.userHasNotRight", addressbookRight, operationType)
                }
                return false
            }
        }
    }

    override fun beforeSaveOrModify(obj: AddressDO, context: PfPersistenceContext) {
        if (obj.id == null) {
            if (obj.addressbookList == null) {
                val addressbookSet = mutableSetOf<AddressbookDO>()
                obj.addressbookList = addressbookSet
            }
            if (obj.addressbookList!!.isEmpty()) {
                obj.addressbookList!!.add(addressbookDao.globalAddressbook)
            }
        } else {
            //Check addressbook changes
            val dbAddress = internalGetById(obj.id, context)
            val addressbookRight = userRights.getRight(UserRightId.MISC_ADDRESSBOOK) as AddressbookRight
            for (dbAddressbook in dbAddress!!.addressbookList!!) {
                //If user has no right for assigned addressbook, it could not be removed
                if (!addressbookRight.hasSelectAccess(loggedInUser, dbAddressbook)
                    && !obj.addressbookList!!.contains(dbAddressbook)
                ) {
                    obj.addressbookList!!.add(dbAddressbook)
                }
            }
        }
    }

    override fun onSaveOrModify(obj: AddressDO, context: PfPersistenceContext) {
        beforeSaveOrModify(obj, context)
    }

    override fun onChange(obj: AddressDO, dbObj: AddressDO, context: PfPersistenceContext) {
        // Don't modify the following fields:
        if (obj.getTransientAttribute("Modify image modification data") !== "true") {
            obj.image = dbObj.image
            obj.imageLastUpdate = dbObj.imageLastUpdate
        }
        super.onChange(obj, dbObj, context)
    }

    /**
     * On force deletion all personal address references has to be deleted.
     * @param obj The deleted object.
     */
    override fun onDelete(obj: AddressDO, context: PfPersistenceContext) {
        personalAddressDao.internalDeleteAll(obj, context)
        teamEventDao.removeAttendeeByAddressIdFromAllEvents(obj, context)
        val counter = context.executeNamedUpdate(
            AddressImageDO.DELETE_ALL_IMAGES_BY_ADDRESS_ID,
            Pair("addressId", obj.id)
        )
        if (counter > 0) {
            log.info("Removed #$counter address images of deleted address: $obj")
        }
        synchronized(deletionListeners)
        {
            for (listener in deletionListeners) {
                listener.onDelete(obj)
            }
        }
    }

    /**
     * Mark the given address, so the image fields (image and imageLastUpdate) will be updated. imageLastUpdate will be
     * set to now.
     *
     * @param address
     * @param hasImage Is there an image or not?
     */
    fun internalModifyImageData(address: AddressDO, hasImage: Boolean) {
        address.setTransientAttribute("Modify image modification data", "true")
        address.image = hasImage
        address.imageLastUpdate = Date()
    }

    override fun onSave(obj: AddressDO, context: PfPersistenceContext) {
        // create uid if empty
        if (StringUtils.isBlank(obj.uid)) {
            obj.uid = UUID.randomUUID().toString()
        }
    }

    /**
     * Sets birthday cache as expired.
     *
     * @param obj
     */
    override fun afterSaveOrModify(obj: AddressDO, context: PfPersistenceContext) {
        birthdayCache.setExpired()
    }

    /**
     * Get the birthdays of address entries.
     *
     * @param fromDate Search for birthdays from given date (ignoring the year).
     * @param toDate   Search for birthdays until given date (ignoring the year).
     * @param all      If false, only the birthdays of favorites will be returned.
     * @return The entries are ordered by date of year and name.
     */
    fun getBirthdays(fromDate: Date, toDate: Date, all: Boolean): Set<BirthdayAddress> {
        return birthdayCache.getBirthdays(fromDate, toDate, all, personalAddressDao.favoriteAddressIdList)
    }

    val favoriteVCards: List<PersonalAddressDO>
        get() {
            val list = personalAddressDao.list
            val result: MutableList<PersonalAddressDO> = ArrayList()
            if (CollectionUtils.isNotEmpty(list)) {
                for (entry in list) {
                    if (entry.isFavoriteCard) {
                        result.add(entry)
                    }
                }
            }
            return result
        }

    fun exportFavoriteVCards(out: Writer, favorites: List<PersonalAddressDO>) {
        log.info("Exporting personal AddressBook.")
        val pw = PrintWriter(out)
        for (entry in favorites) {
            if (!entry.isFavoriteCard) {
                // Entry is not marks as vCard-Entry.
                continue
            }
            val addressDO = entry.address
            exportVCard(pw, addressDO)
        }
        pw.flush()
    }

    /**
     * Exports a single vcard for the given addressDO
     *
     * @param pw
     * @param addressDO
     * @return
     */
    fun exportVCard(pw: PrintWriter, addressDO: AddressDO?) {
        if (log.isDebugEnabled) {
            log.debug("Exporting vCard for addressDo : " + (addressDO?.id))
        }
        pw.println("BEGIN:VCARD")
        pw.println("VERSION:3.0")
        pw.print("N:")
        out(pw, addressDO!!.name)
        pw.print(';')
        out(pw, addressDO.firstName)
        pw.print(";;")
        out(pw, addressDO.title)
        pw.println(";")
        print(pw, "FN:", getFullName(addressDO))
        if (isGiven(addressDO.organization) || isGiven(addressDO.division)) {
            pw.print("ORG:")
            out(pw, addressDO.organization)
            pw.print(';')
            if (isGiven(addressDO.division)) {
                out(pw, addressDO.division)
            }
            pw.println()
        }
        print(pw, "TITLE:", addressDO.positionText)
        print(pw, "EMAIL;type=INTERNET;type=WORK;type=pref:", addressDO.email)
        print(pw, "EMAIL;type=INTERNET;type=HOME;type=pref:", addressDO.privateEmail)
        print(pw, "TEL;type=WORK;type=pref:", addressDO.businessPhone)
        print(pw, "TEL;TYPE=CELL:", addressDO.mobilePhone)
        print(pw, "TEL;type=WORK;type=FAX:", addressDO.fax)
        print(pw, "TEL;TYPE=HOME:", addressDO.privatePhone)
        print(pw, "TEL;TYPE=HOME;type=CELL:", addressDO.privateMobilePhone)

        if (isGiven(addressDO.addressText) || isGiven(addressDO.addressText2) || isGiven(addressDO.city)
            || isGiven(addressDO.zipCode)
        ) {
            pw.print("ADR;TYPE=WORK:;")
            out(pw, addressDO.addressText)
            pw.print(';')
            out(pw, addressDO.addressText2)
            pw.print(';')
            out(pw, addressDO.city)
            pw.print(";;")
            out(pw, addressDO.zipCode)
            pw.print(';')
            out(pw, addressDO.country)
            pw.println()
        }
        if (isGiven(addressDO.privateAddressText)
            || isGiven(addressDO.privateAddressText2)
            || isGiven(addressDO.privateCity)
            || isGiven(addressDO.privateZipCode)
        ) {
            pw.print("ADR;TYPE=HOME:;")
            out(pw, addressDO.privateAddressText)
            pw.print(';')
            out(pw, addressDO.privateAddressText2)
            pw.print(';')
            out(pw, addressDO.privateCity)
            pw.print(";;")
            out(pw, addressDO.privateZipCode)
            pw.print(";")
            pw.println()
        }
        print(pw, "URL;type=pref:", addressDO.website)
        if (addressDO.birthday != null) {
            print(pw, "BDAY;value=date:", V_CARD_DATE_FORMAT.format(from(addressDO.birthday!!).sqlDate))
        }
        if (isGiven(addressDO.comment)) {
            print(pw, "NOTE:", addressDO.comment + "\\nCLASS: WORK")
        } else {
            print(pw, "NOTE:", "CLASS: WORK")
        }
        // pw.println("TZ:+00:00");
        pw.println("CATEGORIES:ProjectForge")
        pw.print("UID:U")
        pw.println(addressDO.id)
        pw.println("END:VCARD")
        pw.println()
        // Unused: addressDO.getState();
    }

    /**
     * Used by vCard export for field 'FN' (full name). Concatenates first name, last name and title.
     *
     * @return
     */
    fun getFullName(a: AddressDO): String {
        val buf = StringBuilder()
        var space = false
        if (isGiven(a.name)) {
            buf.append(a.name)
            space = true
        }
        if (isGiven(a.firstName)) {
            if (space) {
                buf.append(' ')
            } else {
                space = true
            }
            buf.append(a.firstName)
        }
        if (isGiven(a.title)) {
            if (space) {
                buf.append(' ')
            }
            buf.append(a.title)
        }
        return buf.toString()
    }

    private fun print(pw: PrintWriter, key: String, value: String?) {
        if (!isGiven(value)) {
            return
        }
        pw.print(key)
        out(pw, value)
        pw.println()
    }

    /**
     * Simply calls StringUtils.defaultString(String) and replaces: "\r" -> "", "\n" -> "\\n", "," -> "\\,", ":" -> "\\:"
     * and print the resulted string into given PrintWriter (without newline).
     *
     * @param str
     * @see StringUtils.defaultString
     */
    private fun out(pw: PrintWriter, str: String?) {
        val s = StringUtils.defaultString(str)
        var cr = false
        for (i in 0 until s.length) {
            val ch = s[i]
            if (ch == ':') {
                pw.print("\\:")
            } else if (ch == ',') {
                pw.print("\\,")
            } else if (ch == ';') {
                pw.print("\\;")
            } else if (ch == '\r') {
                pw.print("\\n")
                cr = true
                continue
            } else if (ch == '\n') {
                if (!cr) {
                    // Print only \n if not already done by previous \r.
                    pw.print("\\n")
                }
            } else {
                pw.print(ch)
            }
            cr = false
        }
    }

    /**
     * Simply call StringUtils.isNotBlank(String)
     */
    private fun isGiven(str: String?): Boolean {
        return StringUtils.isNotBlank(str)
    }

    private fun appendPhoneEntry(pw: PrintWriter, address: AddressDO, suffix: String, number: String) {
        if (!isGiven(number)) {
            // Do nothing, number is empty.
            return
        }
        val no = extractPhonenumber(
            number,
            instance.getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX)
        )
        val name = address.name
        pw.print("\"")
        if (StringUtils.isNotEmpty(name)) {
            pw.print(name)
        }
        val firstName = address.firstName
        if (StringUtils.isNotBlank(firstName)) {
            if (StringUtils.isNotBlank(name)) {
                pw.print(", ")
            }
            pw.print(firstName)
        }
        if (StringUtils.isNotEmpty(suffix)) {
            pw.print(' ')
            pw.print(suffix)
        }
        pw.print("\",\"")
        pw.println(no + "\"")
    }

    override fun newInstance(): AddressDO {
        return AddressDO()
    }

    fun findAll(): List<AddressDO?> {
        return internalLoadAll()
    }

    fun findByUid(uid: String?): AddressDO? {
        return persistenceService.selectSingleResult(
            "SELECT a FROM AddressDO a WHERE a.uid = :uid",
            AddressDO::class.java,
            Pair("uid", uid),
        )
    }

    fun internalPhoneLookUp(phoneNumber: String): String? {
        val searchNumber = extractPhonenumber(phoneNumber)
        log.info("number=$phoneNumber, searchNumber=$searchNumber")
        val filter = BaseSearchFilter()
        filter.setSearchString("*$searchNumber*")
        val queryFilter = QueryFilter(filter)
        // Use internal get list method for avoiding access checking (no user is logged-in):
        val resultList = internalGetList(queryFilter)
        val buf = StringBuffer()
        if (resultList.isNotEmpty()) {
            var result = resultList[0]
            if (resultList.size > 1) {
                // More than one result, therefore find the newest one:
                buf.append("+") // Mark that more than one entry does exist.
                for (matchingUser in resultList) {
                    if (matchingUser.lastUpdate!!.after(result.lastUpdate)) {
                        result = matchingUser
                    }
                }
            }
            val fullname = result.fullName
            val organization = result.organization
            StringHelper.listToString(buf, "; ", fullname, organization)
            return buf.toString()
        }
        return null
    }

    companion object {
        private val V_CARD_DATE_FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf(
            "addressText",
            "addressText2",
            "postalAddressText",
            "postalAddressText2",
            "privateAddressText",
            "privateAddressText2",
            "organization"
        )

        private val log: Logger = LoggerFactory.getLogger(AddressDao::class.java)

        @JvmStatic
        fun getNormalizedFullname(address: AddressDO): String {
            val builder = StringBuilder()
            if (address.firstName != null) {
                builder.append(address.firstName!!.lowercase(Locale.getDefault()).trim { it <= ' ' })
            }
            if (address.name != null) {
                builder.append(address.name!!.lowercase(Locale.getDefault()).trim { it <= ' ' })
            }
            return builder.toString()
        }
    }
}
