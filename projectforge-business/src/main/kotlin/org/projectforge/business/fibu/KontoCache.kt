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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.apache.commons.collections4.MapUtils
import org.hibernate.Hibernate
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Caches the DATEV accounts.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class KontoCache : AbstractCache() {
    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var projektCache: ProjektCache

    /**
     * The key is the database id.
     */
    private var accountMapById: Map<Long?, KontoDO?>? = null

    val isEmpty: Boolean
        get() {
            checkRefresh()
            return MapUtils.isEmpty(accountMapById)
        }

    fun getKonto(id: Long?): KontoDO? {
        if (id == null) {
            return null
        }
        checkRefresh()
        return accountMapById!![id]
    }

    /**
     * Gets account of given project if given, otherwise the account assigned to the customer assigned to this project. If
     * no account is given at all, null is returned.<br></br>
     * Please note: The object of project must be initialized including the assigned customer, if not a
     * [LazyInitializationException] could be thrown.
     *
     * @param project
     * @return The assigned account if given, otherwise null.
     */
    fun getKonto(project: ProjektDO?): KontoDO? {
        if (project == null) {
            return null
        }
        checkRefresh()
        var konto = getKontoIfNotInitialized(project.konto)
        if (konto != null) {
            return konto
        }
        val customer = project.kunde
        if (customer != null) {
            konto = getKontoIfNotInitialized(customer.konto)
        }
        return konto
    }

    /**
     * Returns the KontoDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getKontoIfNotInitialized(konto: KontoDO?): KontoDO? {
        konto ?: return null
        if (Hibernate.isInitialized(konto)) {
            return konto
        }
        return getKonto(konto.id)
    }

    /**
     * Gets account:
     *
     *  1. Returns the account of given invoice if given.
     *  1. Returns the account of the assigned project if given.
     *  1. Returns the account assigned to the customer of this invoice if given.
     *  1. Returns the account of the customer assigned to the project if given.<br></br>
     * Please note: The object of project must be initialized including the assigned customer, if not a
     * [LazyInitializationException] could be thrown.
     *
     * @param invoice
     * @return The assigned account if given, otherwise null.
     */
    fun getKonto(invoice: RechnungDO?): KontoDO? {
        if (invoice == null) {
            return null
        }
        checkRefresh()
        var konto = getKontoIfNotInitialized(invoice.konto)
        if (konto != null) {
            return konto
        }
        val project = projektCache.getProjektIfNotInitialized(invoice.projekt)
        if (project != null) {
            konto = getKontoIfNotInitialized(project.konto)
            if (konto != null) {
                return konto
            }
        }
        var kunde = kundeCache.getKundeIfNotInitialized(invoice.kunde)
        if (kunde != null) {
            konto = getKontoIfNotInitialized(kunde.konto)
        }
        if (konto != null) {
            return konto
        }
        if (project != null) {
            kunde = kundeCache.getKundeIfNotInitialized(project.kunde)
            if (kunde != null) {
                konto = getKontoIfNotInitialized(kunde.konto)
            }
        }
        return konto
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    public override fun refresh() {
        log.info("Initializing KontoCache ...")
        persistenceService.runIsolatedReadOnly { context ->
            // This method must not be synchronized because it works with a new copy of maps.
            val map: MutableMap<Long?, KontoDO?> = HashMap()
            val list = persistenceService.executeQuery(
                "from KontoDO t where deleted=false",
                KontoDO::class.java,
            )
            for (konto in list) {
                map[konto.id] = konto
            }
            this.accountMapById = map
            log.info { "Initializing of KontoCache done. ${context.formatStats()}" }
        }
    }
}
