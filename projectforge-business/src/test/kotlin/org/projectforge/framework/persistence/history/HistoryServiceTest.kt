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

package org.projectforge.framework.persistence.history

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.net.URI
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

class HistoryServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyService: HistoryService

    @Test
    fun testOldInvoiceHistory() {
        ensureSetup()
        val invoice = RechnungDO()
        invoice.id = 40770225
        historyService.loadHistory(invoice).let { historyEntries ->
            Assertions.assertEquals(2, historyEntries.size)
            var entry = historyEntries[0]
            Assertions.assertEquals(4, entry.attributes!!.size)
            val attrs = entry.attributes!!
            Assertions.assertEquals(4, attrs.size)
            assert(attrs, "bemerkung", "PoBa", null, PropertyOpType.Insert)
            assert(attrs, "bezahlDatum", "2023-12-29", null, PropertyOpType.Insert)
            assert(attrs, "status", "BEZAHLT", "GESTELLT", PropertyOpType.Update)
            assert(attrs, "zahlBetrag", "4765.95", null, PropertyOpType.Insert)
            entry = historyEntries[1]
            Assertions.assertEquals(17, entry.attributes!!.size)
        }
        invoice.id = 351958
        historyService.loadHistory(invoice).let { historyEntries ->
            historyEntries.filter { it.entityId == 351958L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice 351958")

            }
            historyEntries.filter { it.entityId == 351959L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice position 351959")
            }
            historyEntries.filter { it.entityId == 351960L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice position 3519560")
            }
            historyEntries.filter { it.entityId == 382506L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382506")
            }
            historyEntries.filter { it.entityId == 382507L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382507")
            }
            historyEntries.filter { it.entityId == 382508L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382508")
            }
            historyEntries.filter { it.entityId == 382509L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382509")
            }
            Assertions.assertEquals(20, historyEntries.size)
        }

        // 20.04.15 17:19 Update Konto              -> 12202
        // 08.03.10 13:26 Update #1:Position        -> 674.1
        // 08.03.10 13:26 Update Betreff            DM 2010 #674 -> DM 2010
        // 08.03.10 13:26 Update #2:Position        -> 674.2
        // 22.02.10 08:55 Update Rechnungsstatus    gestellt -> bezahlt
        // 22.02.10 08:55 Update Bezahldatum        -> 2010-02-22
        // 22.02.10 08:55 Update Zahlbetrag         -> 4455.00
        // 08.02.10 11:33 Update #1.kost#0:Kost     5.620.08.02 -> 5.620.07.02
        // 08.02.10 11:33 Update #1.kost#1:Kost     5.620.08.02 -> 5.620.07.02
        // 08.02.10 11:33 Update #1.kost#2:Kost     5.620.08.02 -> 5.620.07.02
        // 08.02.10 11:33 Update #2.kost#0:Kost     5.620.08.04 -> 5.620.07.04
        // 08.02.10 11:24 Update #1:kostZuweisungen	-> 0, 1, 2
        // 08.02.10 11:24 Update #2:kostZuweisungen	-> 0
        // 08.02.10 11:24 Insert #1.kost#0:
        // 08.02.10 11:24 Insert #1.kost#1:
        // 08.02.10 11:24 Insert #1.kost#2:
        // 08.02.10 11:24 Insert #2.kost#0:
        // 02.02.10 16:16 Update #2:Mwst-Satz       0.19000 -> 0.19
        // 02.02.10 16:16 Update #1:Mwst-Satz       0.19000 -> 0.19
        // 26.01.10 08:31 Insert #2:
        // 26.01.10 08:31 Insert
        // 26.01.10 08:31 Insert #1:
    }

    @Test
    fun testOldUserHistory() {
        ensureSetup()
        val user = PFUserDO()
        user.id = 34961222
        historyService.loadHistory(user).let { historyEntries ->
            Assertions.assertEquals(27, historyEntries.size)
            var entry = historyEntries.find { it.id == getNewHistoryEntryId(34961266) }!! // was 34961266
            Assertions.assertEquals(1, entry.attributes!!.size)
            var attributes = entry.attributes!!
            Assertions.assertEquals(1, attributes.size)
            assert(attributes, "assignedGroups", "1100452,1100063,1826459,33", null, PropertyOpType.Update)
            entry = historyEntries.find { it.id == getNewHistoryEntryId(38057999) }!! // was 38057999
            Assertions.assertEquals(1, entry.attributes!!.size)
            attributes = entry.attributes!!
            Assertions.assertEquals(1, attributes.size)
            assert(
                attributes,
                "lastPasswordChange",
                "2023-02-10 13:34:25:184",
                "2022-10-04 09:55:19:329",
                PropertyOpType.Update
            )
            entry = historyEntries.find { it.id == getNewHistoryEntryId(37229748) }!! // was 37229748
            Assertions.assertEquals(2, entry.attributes!!.size)
            attributes = entry.attributes!!
            Assertions.assertEquals(2, attributes.size)
            assert(attributes, "locale", "de_DE", "", PropertyOpType.Update)
            assert(attributes, "timeZoneString", "Europe/Berlin", null, PropertyOpType.Insert)
        }
    }

    @Test
    fun testOldAuftragHistory() {
        ensureSetup()
        val order = AuftragDO()
        order.id = 36901223
        historyService.loadHistory(order).let { historyEntries ->
            // 9 history entries, 36 attr entries in old format -> 18 entries in new format.
            historyEntries.filter { it.entityId == 36901223L }.let { entries ->
                Assertions.assertEquals(9, entries.size, "9 entries for Auftrag 36901229")
            }
            historyEntries.filter { it.entityId == 36901224L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901224")
            }
            historyEntries.filter { it.entityId == 36901225L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901225")
            }
            historyEntries.filter { it.entityId == 36901226L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901226")
            }
            historyEntries.filter { it.entityId == 36901227L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901227")
            }
            historyEntries.filter { it.entityId == 36901228L }.let { entries ->
                Assertions.assertEquals(5, entries.size, "5 for Auftragsposition 36901228")
            }
            Assertions.assertEquals(26, historyEntries.size, "26 entries in total")
            val entry = historyEntries.find { it.id == getNewHistoryEntryId(36901229) }!! // was 36901229
            Assertions.assertEquals(18, entry.attributes!!.size)
            val diffEntries = entry.attributes!!
            Assertions.assertEquals(18, diffEntries.size)

            val orderPos = AuftragsPositionDO()
            orderPos.id = 36901228
        }
    }

    private fun assert(
        attrs: Collection<HistoryEntryAttr>, propertyName: String, value: String?, oldValue: String?, operationType: PropertyOpType
    ) {
        attrs.find { it.propertyName == propertyName }.let { attr ->
            Assertions.assertNotNull(attr, "Property $propertyName not found")
            Assertions.assertEquals(propertyName, attr!!.propertyName)
            Assertions.assertEquals(value, attr.value)
            Assertions.assertEquals(oldValue, attr.oldValue)
            Assertions.assertEquals(operationType, attr.opType)
        }
    }

    /**
     * Create entries in new format:
     */
    private fun add(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val entry = HistoryCreateUtils.createHistoryEntry(entity, operationType)

        val attr1 = HistoryCreateUtils.createAttr(
            GroupDO::class,
            propertyName = propertyName,
            value = value,
            oldValue = oldValue,
        )
        val attrs = mutableListOf(attr1)

        historyService.save(entry, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", entry.entityName)
        Assertions.assertEquals(entity.id, entry.entityId)
        Assertions.assertEquals("anon", entry.modifiedBy)
        val createdAt = entry.modifiedAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(entry.id, attr1.parent!!.id)
    }

    private fun ensureSetup() {
        ensureSetup(persistenceService, historyService)
    }

    companion object {
        private val oldPropertyClass = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"

        /**
         * Key is the parsed history entry id and value the database entry saved by this test case.
         */
        private val historyEntryMap = mutableMapOf<Long, HistoryEntryDO>()

        /**
         * Key is the parsed history master_fk id and value the generated HistoryEntryAttrDO.
         */
        private val historyAttrMap = mutableMapOf<Long, MutableList<HistoryEntryAttrDO>>()

        internal fun getNewHistoryEntryId(origHistoryEntryId: Long): Long {
            val newEntry = historyEntryMap[origHistoryEntryId]
            Assertions.assertNotNull(newEntry, "HistoryEntryDO with id $origHistoryEntryId not found!")
            return newEntry!!.id!!
        }

        internal fun ensureSetup(persistenceService: PfPersistenceService, historyService: HistoryService) {
            if (historyEntryMap.isNotEmpty()) {
                return // Already done.
            }
            parseFile(getUri("/history/pf_history-testentries.csv")).forEach { map ->
                // pk, modifiedby, entity_id, entity_name, entity_optype
                val pk = map["pk"]!!.toLong()
                val historyEntry = HistoryEntryDO()
                historyEntry.modifiedAt = PFDateTimeUtils.parse(map["modifiedat"])?.utilDate
                historyEntry.modifiedBy = map["modifiedby"]
                historyEntry.entityId = map["entity_id"]!!.toLong()
                historyEntry.entityName = map["entity_name"]!!
                historyEntry.entityOpType = EntityOpType.valueOf(map["entity_optype"]!!)
                historyEntryMap[pk] = historyEntry
            }
            parseFile(getUri("/history/pf_history_attr-testentries.csv")).forEach { map ->
                try {
                    // value, propertyname, type, property_type_class, old_value, optype, master_fk
                    val attr = HistoryEntryAttrDO()
                    attr.value = map["value"]
                    attr.oldValue = map["old_alue"]
                    attr.propertyName = map["propertyname"]
                    attr.propertyTypeClass = map["property_type_class"]
                    map["optype"]?.let { optype ->
                        if (optype.isNotEmpty()) {
                            attr.opType = PropertyOpType.valueOf(optype)
                        }
                    }
                    val masterFk = map["master_fk"]!!.toLong()
                    historyAttrMap.getOrPut(masterFk) { mutableListOf() }.add(attr)
                } catch (ex: Exception) {
                    log.error {
                        "Error while parsing map ${
                            map.entries.joinToString(
                                prefix = "{", postfix = "}"
                            ) { (key, value) -> "\"$key\": \"$value\"" }
                        }"
                    }
                    throw IllegalStateException("Error parsing map $map", ex)
                }
            }
            persistenceService.runInTransaction { context ->
                val em = context.em
                historyEntryMap.entries.forEach { entry ->
                    val historyEntry = entry.value
                    val historyEntryId = entry.key
                    val attrs = historyAttrMap[historyEntryId]
                    if (em.find(HistoryEntryDO::class.java, historyEntryId) == null) {
                        // Entry not yet saved:
                        historyService.save(em, historyEntry, attrs)
                    }
                }
            }

            val user = PFUserDO()
            user.id = 42
            // One group assigned:
            addOldFormat(
                historyService,
                user,
                value = "1052256",
                propertyName = "assignedGroups",
                operationType = EntityOpType.Insert
            )
            // Assigned: 34,101478,33 unassigned: 17,16,11,31
            addOldFormat(
                historyService,
                user,
                value = "34,101478,33",
                oldValue = "17,16,11,31",
                propertyName = "assignedGroups",
                operationType = EntityOpType.Insert
            )
            addOldFormat(
                historyService,
                user,
                value = "Project manager",
                oldValue = "Project assistant",
                propertyName = "description",
                operationType = EntityOpType.Update,
            )
            // Test entries generated with:
            // \pset null 'NULL'
            // select pk,entity_id,entity_name,entity_optype from t_pf_history where entity_id = <ENTITY_ID>
            // select value,propertyname,type,property_type_class,old_value,optype,master_fk from t_pf_history_attr where master_fk in (PK1,PK2,PK3);
            persistenceService.runInTransaction { context ->
                context.executeNativeUpdate("insert into t_fibu_rechnung (pk,deleted,datum) values (351958,false,'2023-12-29')")
                context.executeNativeUpdate("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (351959,false,351958,1)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (382507,false,351959,0,10.10)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (382508,false,351959,1),20.20")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (382509,false,351959,2),30.30")
                context.executeNativeUpdate("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (351960,false,351958,2)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (382506,false,351960,0)")
                context.executeNativeUpdate("insert into t_fibu_konto (pk,deleted,bezeichnung,nummer) values (167040,false,'ACME Int.',12202)")
            }
        }

        private fun parseFile(uri: URI): List<Map<String, String?>> {
            val result = mutableListOf<Map<String, String?>>()
            val columnNames = mutableListOf<String>()
            var lineNo = 0
            File(uri).readLines().forEach { line ->
                ++lineNo
                if (columnNames.isEmpty()) {
                    // First row:
                    columnNames.addAll(parseLine(line).map { it ?: "<unknown>" })
                } else if (!line.contains('|') || line.startsWith("--")) {
                    // Ignore empty lines and second row.
                } else {
                    val values = parseLine(line)
                    val map = mutableMapOf<String, String?>()
                    if (values.size != columnNames.size) {
                        throw IllegalStateException("Values size ${values.size} != columnNames size ${columnNames.size} in line $lineNo: $line")
                    }
                    columnNames.forEachIndexed { index, name ->
                        map[name] = values[index]
                    }
                    result.add(map)
                }
            }
            return result
        }

        private fun parseLine(line: String): List<String?> {
            val list = mutableListOf<String?>()
            line.split("|").forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed == "NULL") {
                    list.add(null)
                } else {
                    list.add(trimmed)
                }
            }
            return list
        }

        private fun getUri(path: String): URI {
            return object {}.javaClass.getResource(path)!!.toURI()
        }

        /**
         * Create entries in old mgc format:
         */
        private fun addOldFormat(
            historyService: HistoryService,
            entity: BaseDO<Long>,
            value: String?,
            oldValue: String? = null,
            propertyName: String,
            operationType: EntityOpType
        ) {
            val entry = HistoryCreateUtils.createHistoryEntry(entity, operationType)

            val attr1 = HistoryCreateUtils.createAttr(
                GroupDO::class, propertyName = "$propertyName${HistoryEntryDOUtils.NEWVAL_SUFFIX}", value = value
            )
            val attr2 = HistoryCreateUtils.createAttr(
                oldPropertyClass, "$propertyName${HistoryEntryDOUtils.OP_SUFFIX}", value = operationType.name
            )
            val attr3 = HistoryCreateUtils.createAttr(
                GroupDO::class, "$propertyName${HistoryEntryDOUtils.OLDVAL_SUFFIX}", value = oldValue
            )
            val attrs = mutableListOf(attr1, attr2, attr3)

            historyService.save(entry, attrs)!!

            Assertions.assertEquals(
                "org.projectforge.framework.persistence.user.entities.PFUserDO", entry.entityName
            )
            Assertions.assertEquals(entity.id, entry.entityId)
            Assertions.assertEquals("anon", entry.modifiedBy)
            val createdAt = entry.modifiedAt!!.time
            Assertions.assertTrue(
                Math.abs(System.currentTimeMillis() - createdAt) < 10000,
                "createdAt should be near to now (10s)",
            )

            Assertions.assertEquals(entry.id, attr1.parent!!.id)
        }

        /**
         * Asserts the history entry.
         * @return The attributes of the history entry (migth be null).
         */
        fun assertHistoryEntry(
            entityClass: KClass<*>,
            id: Long?,
            opType: EntityOpType,
            modUser: PFUserDO,
            entry: HistoryEntry,
            numberOfAttributes: Int = 0,
        ): Set<HistoryEntryAttr>? {
            Assertions.assertEquals(entityClass.java.name, entry.entityName)
            if (id != null) {
                Assertions.assertEquals(id, entry.entityId)
            }
            Assertions.assertEquals(opType, entry.entityOpType)
            Assertions.assertEquals(modUser.id?.toString(), entry.modifiedBy)
            Assertions.assertTrue(
                System.currentTimeMillis() - entry.modifiedAt!!.time < 10000,
                "Time difference is too big",
            )
            entry as HistoryEntryDO
            Assertions.assertEquals(numberOfAttributes, entry.attributes?.size ?: 0)
            return entry.attributes
        }

        fun assertAttrEntry(
            propertyClass: String?,
            value: String?,
            oldValue: String?,
            propertyName: String?,
            optype: PropertyOpType,
            attributes: Set<HistoryEntryAttr>?,
        ) {
            Assertions.assertFalse(attributes.isNullOrEmpty())
            val attr = attributes?.firstOrNull { it.propertyName == propertyName }
            Assertions.assertNotNull(attr, "Property $propertyName not found")
            Assertions.assertEquals(propertyClass, attr!!.propertyTypeClass, "propertyTypeClass")
            Assertions.assertEquals(value, attr.value, "$propertyName.value")
            Assertions.assertEquals(oldValue, attr.oldValue, "$propertyName.oldValue")
            Assertions.assertEquals(propertyName, attr.propertyName, "propertyName")
            Assertions.assertEquals(optype, attr.opType, "opType")

        }
    }
}
