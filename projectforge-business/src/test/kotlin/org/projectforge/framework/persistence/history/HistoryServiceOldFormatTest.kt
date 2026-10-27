/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryTester
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTimeUtils
import java.io.File
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Test of old history format (until .
 */
class HistoryServiceOldFormatTest : AbstractTestBase() {
    @Test
    fun testOldInvoiceHistory() {
        ensureSetup()
        val invoice = RechnungDO()
        invoice.id = 72130000
        val hist = createHistoryTester()
        val msg = "Invoice 72130000, entry 0"
        hist.loadHistory(invoice, 2, 21, msg)
        hist.getEntry(0, 4, msg).let { entry ->
            HistoryTester.assertHistoryAttr(entry, "bemerkung", "Bank", null, PropertyOpType.Insert, msg = msg)
            HistoryTester.assertHistoryAttr(entry, "bemerkung", "Bank", null, PropertyOpType.Insert, msg = msg)
            HistoryTester.assertHistoryAttr(
                entry,
                "bezahlDatum",
                "2023-12-29",
                null,
                PropertyOpType.Insert,
                LocalDate::class,
                msg = msg,
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "status",
                "BEZAHLT",
                "GESTELLT",
                PropertyOpType.Update,
                RechnungStatus::class,
                msg = msg,
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "zahlBetrag",
                "1234.56",
                null,
                PropertyOpType.Insert,
                BigDecimal::class,
                msg = msg,
            )
        }
        hist.getEntry(1, 17, "Invoice 72130000, entry 0")
        invoice.id = 703300
        hist.loadHistory(invoice, 20, 22, "Invoice 703300")
        hist.getEntriesByEntityId(703300L, 4, 6)
        hist.getEntriesByEntityId(703600L, 4, 4)
        hist.getEntriesByEntityId(703900L, 4, 4)
        hist.getEntriesByEntityId(704200L, 2, 2)
        hist.getEntriesByEntityId(704500L, 2, 2)
        hist.getEntriesByEntityId(704800L, 2, 2)
        hist.getEntriesByEntityId(705100L, 2, 2)
        // 20.04.15 17:19 Update Konto              -> 19001
        // 08.03.10 13:26 Update #1:Position        -> 123.1
        // 08.03.10 13:26 Update Betreff            XY 2010 #123 -> XY 2010
        // 08.03.10 13:26 Update #2:Position        -> 123.2
        // 22.02.10 08:55 Update Rechnungsstatus    gestellt -> bezahlt
        // 22.02.10 08:55 Update Bezahldatum        -> 2010-02-22
        // 22.02.10 08:55 Update Zahlbetrag         -> 1500.00
        // 08.02.10 11:33 Update #1.kost#0:Kost     5.900.08.02 -> 5.900.07.02
        // 08.02.10 11:33 Update #1.kost#1:Kost     5.900.08.02 -> 5.900.07.02
        // 08.02.10 11:33 Update #1.kost#2:Kost     5.900.08.02 -> 5.900.07.02
        // 08.02.10 11:33 Update #2.kost#0:Kost     5.900.08.04 -> 5.900.07.04
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
        user.id = 70360000
        val hist = createHistoryTester()
        hist.loadHistory(user, 27, 4)
        hist.getEntriesByPk(getNewHistoryEntryId(70420000), 1, 1).first().let { entry ->
            HistoryTester.assertHistoryAttr(
                entry,
                "assignedGroups",
                "7006000,7003000,7009000,33",
                null,
                PropertyOpType.Update,
                GroupDO::class
            )
        }
        hist.getEntriesByPk(getNewHistoryEntryId(71260000), 1, 1).first().let { entry ->
            HistoryTester.assertHistoryAttr(
                entry,
                "lastPasswordChange",
                "2023-02-10 13:34:25:184",
                "2022-10-04 09:55:19:329",
                PropertyOpType.Update,
                Date::class,
            )
        }
        hist.getEntriesByPk(getNewHistoryEntryId(71200000), 1, 2).first().let { entry ->
            HistoryTester.assertHistoryAttr(entry, "locale", "de_DE", "", PropertyOpType.Update, Locale::class)
            HistoryTester.assertHistoryAttr(entry, "timeZoneString", "Europe/Berlin", null, PropertyOpType.Insert)
        }
    }

    @Test
    fun testOldAuftragHistory() {
        ensureSetup()
        val order = AuftragDO()
        order.id = 70810000
        val hist = createHistoryTester()
        hist.loadHistory(
            order,
            26,
            46,
            "The history entries of the embedded positions should be found, because they are part of an history attr entry: 70840000,70870000,70900000,70930000,70960000."
        )
        hist.getEntriesByEntityId(70810000L, 9, 27)
        hist.getEntriesByEntityId(70840000L, 3, 3)
        hist.getEntriesByEntityId(70870000L, 3, 3)
        hist.getEntriesByEntityId(70900000L, 3, 3)
        hist.getEntriesByEntityId(70930000L, 3, 3)
        hist.getEntriesByEntityId(70960000L, 5, 7)
        hist.getEntriesByPk(getNewHistoryEntryId(70990000), 1, 18)
        // val orderPos = AuftragsPositionDO()
        // orderPos.id = 70960000
    }

    @Test
    fun testOldEmployeeTimedAttrHistory() {
        ensureSetup()
        val employee = EmployeeDO()
        employee.id = 70090000
        val hist = createHistoryTester()
        hist.loadHistory(employee, 5, 6)
        hist.getEntry(0, 1).assertAttr(
            "annualleave.2007-11-26",
            value = "30",
            oldValue = "32",
            opType = PropertyOpType.Update,
            propertyTypeClass = BigDecimal::class
        )
        hist.getEntry(1, 1).assertAttr(
            "status.2015-09-01",
            value = null,
            oldValue = null,
            opType = PropertyOpType.Delete,
            propertyTypeClass = null
        )
        hist.getEntry(2, 2)
            .assertAttr(
                "annualleave.2019-03-01",
                value = "30",
                oldValue = null,
                opType = PropertyOpType.Insert,
                propertyTypeClass = BigDecimal::class
            )
            .assertAttr(
                "status.2019-03-01",
                value = "fibu.employee.status.festAngestellter",
                oldValue = null,
                opType = PropertyOpType.Insert
            )
        hist.getEntry(3, 1).assertAttr(
            "status.2018-04-01",
            value = "fibu.employee.status.aushilfe",
            oldValue = null,
            opType = PropertyOpType.Insert
        )
        hist.getEntry(4, 1).assertAttr(
            "status.2017-03-01",
            value = "fibu.employee.status.studentischeHilfskraft",
            oldValue = null,
            opType = PropertyOpType.Insert
        )
    }

    private fun ensureSetup() {
        ensureSetup(this, persistenceService, historyService)
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

        internal fun ensureSetup(testClass: AbstractTestBase, persistenceService: PfPersistenceService, historyService: HistoryService) {
            if (historyEntryMap.isNotEmpty()) {
                return // Already done.
            }
            testClass.recreateDataBase()
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
                context.executeNativeUpdate("insert into t_fibu_rechnung (pk,deleted,datum) values (703300,false,'2023-12-29')")
                context.executeNativeUpdate("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (703600,false,703300,1)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (704500,false,703600,0,10.10)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (704800,false,703600,1),20.20")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index,netto) values (705100,false,703600,2),30.30")
                context.executeNativeUpdate("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (703900,false,703300,2)")
                context.executeNativeUpdate("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (704200,false,703900,0)")
                context.executeNativeUpdate("insert into t_fibu_konto (pk,deleted,bezeichnung,nummer) values (701500,false,'ACME Int.',19001)")
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
                GroupDO::class, propertyName = "$propertyName${HistoryOldFormatConverter.NEWVAL_SUFFIX}", value = value
            )
            val attr2 = HistoryCreateUtils.createAttr(
                oldPropertyClass, "$propertyName${HistoryOldFormatConverter.OP_SUFFIX}", value = operationType.name
            )
            val attr3 = HistoryCreateUtils.createAttr(
                GroupDO::class, "$propertyName${HistoryOldFormatConverter.OLDVAL_SUFFIX}", value = oldValue
            )
            val attrs = mutableListOf(attr1, attr2, attr3)

            historyService.save(entry, attrs)!!

            Assertions.assertEquals(
                "org.projectforge.framework.persistence.user.entities.PFUserDO", entry.entityName
            )
            Assertions.assertEquals(entity.id, entry.entityId)
            val user = ThreadLocalUserContext.loggedInUser
            Assertions.assertEquals(user?.id?.toString() ?: "anon", entry.modifiedBy)
            val createdAt = entry.modifiedAt!!.time
            Assertions.assertTrue(
                Math.abs(System.currentTimeMillis() - createdAt) < 10000,
                "createdAt should be near to now (10s)",
            )

            Assertions.assertEquals(entry.id, attr1.parent!!.id)
        }
    }
}
