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

import jakarta.persistence.Tuple
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.persistence.database.TupleUtils.getBigDecimal
import org.projectforge.framework.persistence.database.TupleUtils.getLong
import org.projectforge.framework.persistence.database.TupleUtils.getShort
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Uses tuple query for faster access to the database.
 */
@Service
class RechnungService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    fun fetchPositionen(invoices: List<AbstractRechnungDO>) {
        val invoiceIds = invoices.map { it.id!! }
        val entityClass = invoices.first()::class
        if (entityClass == RechnungDO::class) { // Re-assign positions for avoiding Hibernate lazy loading.
            invoices.forEach { (it as RechnungDO).positionen = mutableListOf() }
        } else {
            invoices.forEach { (it as EingangsrechnungDO).positionen = mutableListOf() }
        }
        persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            val sql =
                if (entityClass == RechnungDO::class) SELECT_RECHNUNG_POSITIONEN else SELECT_EINGANGSRECHNUNG_POSITIONEN
            val query = em.createQuery(sql, Tuple::class.java)
            query.setParameter("rechnungIds", invoiceIds)
            val tuples = query.resultList
            tuples.map { tuple ->
                val instance = if (entityClass == RechnungDO::class) {
                    RechnungsPositionDO()
                } else {
                    EingangsrechnungsPositionDO()
                }
                instance.also { pos ->
                    pos.id = getLong(tuple, "id")
                    pos.number = getShort(tuple, "number")!!
                    pos.menge = getBigDecimal(tuple, "menge")
                    pos.einzelNetto = getBigDecimal(tuple, "einzelNetto")
                    pos.vat = getBigDecimal(tuple, "vat")
                    val rechnungId = getLong(tuple, "rechnungId")
                    invoices.find { it.id == rechnungId }?.let { rechnung ->
                        if (rechnung is RechnungDO) {
                            pos as RechnungsPositionDO
                            pos.rechnung = rechnung
                            rechnung.positionen!!.add(pos)
                        } else {
                            rechnung as EingangsrechnungDO
                            pos as EingangsrechnungsPositionDO
                            pos.eingangsrechnung = rechnung
                            rechnung.positionen!!.add(pos)
                        }
                    }
                }
            }
        }
    }

    fun selectKostzuweisungen(
        invoices: List<AbstractRechnungDO>,
    ): List<KostZuweisungDO> {
        val positionen = invoices.flatMap { it.positionen ?: emptyList() }
        val entityClass = positionen.first()::class
        val posIds = positionen.map { it.id!! }
        return selectKostzuweisungen(entityClass, posIds)
    }

    fun selectKostzuweisungen(
        entityClass: KClass<*>,
        positionIds: Collection<Long>
    ): List<KostZuweisungDO> {
        return persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            val replacement = if (entityClass == RechnungDO::class) "rechnungsPosition" else "eingangsrechnungsPosition"
            val sql = SELECT_KOST_ZUWEISUNGEN.replace("{rechnungsPosition}", replacement)
            val query = em.createQuery(sql, Tuple::class.java)
            query.setParameter("positionIds", positionIds)
            val tuples = query.resultList
            tuples.map { tuple ->
                KostZuweisungDO().also { zuweisung ->
                    zuweisung.id = getLong(tuple, "id")
                    zuweisung.netto = getBigDecimal(tuple, "netto")
                    zuweisung.index = getShort(tuple, "index")!!
                    getLong(tuple, "kost1Id")?.let { kost1Id ->
                        zuweisung.kost1 = Kost1DO().also { it.id = kost1Id }
                    }
                    getLong(tuple, "kost2Id")?.let { kost2Id ->
                        zuweisung.kost2 = Kost2DO().also { it.id = kost2Id }
                    }
                    getLong(tuple, "rechnungsPositionId")?.let { rechnungsPositionId ->
                        val instance = entityClass.createInstance()
                        if (instance is RechnungsPositionDO) {
                            zuweisung.rechnungsPosition = instance.also { it.id = rechnungsPositionId }
                        } else {
                            instance as EingangsrechnungsPositionDO
                            zuweisung.eingangsrechnungsPosition = instance.also { it.id = rechnungsPositionId }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val SELECT_RECHNUNG_POSITIONEN = """
            SELECT p.id as id,p.number as number,p.menge as menge,p.einzelNetto as einzelNetto,p.vat as vat,
                   p.rechnung.id as rechnungId
            FROM ${RechnungsPositionDO::class.simpleName} p
            WHERE p.rechnung.id IN :rechnungIds
        """.trimIndent()
        private val SELECT_EINGANGSRECHNUNG_POSITIONEN = """
            SELECT p.id as id,p.number as number,p.menge as menge,p.einzelNetto as einzelNetto,p.vat as vat,
                   p.eingangsrechnung.id as rechnungId
            FROM ${EingangsrechnungsPositionDO::class.simpleName} p
            WHERE p.eingangsrechnung.id IN :rechnungIds
        """.trimIndent()
        private val SELECT_KOST_ZUWEISUNGEN = """
            SELECT k.id as id,k.netto as netto,k.index as index,k.kost1.id as kost1Id,k.kost2.id as kost2Id,
                   k.{rechnungsPosition}.id as rechnungsPositionId
            FROM ${KostZuweisungDO::class.simpleName} k
            WHERE k.{rechnungsPosition}.id IN :positionIds
        """.trimIndent()
    }
}
