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
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.persistence.database.JdbcUtils.getBigDecimal
import org.projectforge.framework.persistence.database.JdbcUtils.getInt
import org.projectforge.framework.persistence.database.JdbcUtils.getLocalDate
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private val log = KotlinLogging.logger {}

/**
 * Uses jdbc instead of Hibernate for faster access to the database.
 */
@Service
class RechnungJdbcService {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    fun selectRechnungInfos(entityClass: KClass<out AbstractRechnungDO>): List<RechnungInfo> {
        val rechnungen = mutableMapOf<Long, AbstractRechnungDO>()
        val sql =
            if (entityClass == RechnungDO::class) SELECT_RECHNUNG_WITH_KOST else SELECT_EINGANGS_RECHNUNG_WITH_KOST
        jdbcTemplate.query(sql, ResultSetExtractor { rs ->
            while (rs.next()) {
                val rechnungId = rs.getLong("rechnung_id")
                val rechnung = rechnungen.computeIfAbsent(rechnungId) {
                    val instance = entityClass.createInstance()
                    // r.pk,r.deleted,r.status,r.nummer,r.discountmaturity,r.discountpercent
                    instance.also {
                        it.id = rechnungId
                        it.deleted = rs.getBoolean("deleted")
                        it.datum = getLocalDate(rs, "datum")
                        it.bezahlDatum = getLocalDate(rs, "bezahl_datum")
                        it.faelligkeit = getLocalDate(rs, "faelligkeit")
                        it.zahlBetrag = getBigDecimal(rs, "zahl_betrag")
                        it.discountMaturity = getLocalDate(rs, "discountmaturity")
                        it.discountPercent = getBigDecimal(rs, "discountpercent")
                        if (it is RechnungDO) {
                            it.status = RechnungStatus.safeValueOf(rs.getString("status"))
                            it.nummer = getInt(rs, "nummer")
                        }
                    }
                }
                val posId = getLong(rs, "pos_id")
                var pos = rechnung.positionen?.find { it.id == posId } as? AbstractRechnungsPositionDO ?: run {
                    val instance = if (rechnung is RechnungDO) {
                        RechnungsPositionDO().also {
                            rechnung.positionen = rechnung.positionen ?: mutableListOf()
                            rechnung.positionen!!.add(it)
                            getLong(rs, "auftrags_position_fk")?.let { posId ->
                                it.auftragsPosition = AuftragsPositionDO().also { it.id = posId }
                            }
                        }
                    } else {
                        rechnung as EingangsrechnungDO
                        EingangsrechnungsPositionDO().also {
                            rechnung.positionen = rechnung.positionen ?: mutableListOf()
                            rechnung.positionen!!.add(it)
                        }
                    }
                    instance.also {
                        //  p.pk,p.number,p.menge,p.einzel_netto,p.vat
                        it.id = posId
                        it.number = rs.getShort("number")
                        it.menge = getBigDecimal(rs, "menge")
                        it.einzelNetto = getBigDecimal(rs, "einzel_netto")
                        it.vat = getBigDecimal(rs, "vat")
                        it.text = rs.getString("s_text")
                    }
                    instance
                }
                val kostId = getLong(rs, "kost_id")
                if (pos.kostZuweisungen?.any { it.id == kostId } == true) {
                    log.error { "KostZuweisung already exists: $kostId for invoice $rechnungId and position $posId. (shouldn't occur.)" }
                    continue
                }
                val kost = KostZuweisungDO().also {
                    // k.pk,k.netto,k.index,k.kost1_fk,k.kost2_fk
                    it.id = kostId
                    it.netto = getBigDecimal(rs, "netto")
                    it.index = rs.getShort("index")
                    it.kost1 = Kost1DO().also { it.id = getLong(rs, "kost1_fk") }
                    it.kost2 = Kost2DO().also { it.id = getLong(rs, "kost2_fk") }
                }
                pos.kostZuweisungen = pos.kostZuweisungen ?: mutableListOf()
                pos.kostZuweisungen!!.add(kost)
            }
        })
        return rechnungen.map { RechnungCalculator.calculate(it.value, useCaches = false) }
    }

    fun selectRechnungsPositionenWithAuftragPosition(): List<RechnungsPositionDO> {
        return jdbcTemplate.query(SELECT_RECHNUNG_ORDER, object : RowMapper<RechnungsPositionDO> {
            @Throws(SQLException::class)
            override fun mapRow(rs: ResultSet, rowNum: Int): RechnungsPositionDO {
                return RechnungsPositionDO().also {
                    it.id = getLong(rs, "pk")
                    it.number = rs.getShort("number")
                    it.deleted = rs.getBoolean("deleted")
                    it.menge = getBigDecimal(rs, "menge")
                    it.einzelNetto = getBigDecimal(rs, "einzel_netto")
                    it.vat = getBigDecimal(rs, "vat")
                    getLong(rs, "auftrags_position_fk")?.let { posId ->
                        it.auftragsPosition = AuftragsPositionDO().also { it.id = posId }
                    }
                    getLong(rs, "rechnung_fk")?.let { rechnungId ->
                        it.rechnung = RechnungDO().also { it.id = rechnungId }
                    }
                }
            }
        }
        )
    }

    companion object {
        private val SELECT_RECHNUNG_ORDER = """
            SELECT p.pk,p.deleted,p.rechnung_fk,p.number,p.menge,p.einzel_netto,p.vat,p.auftrags_position_fk
            FROM t_fibu_rechnung_position p
            WHERE p.auftrags_position_fk IS NOT NULL
        """.trimIndent()

        private val SELECT_RECHNUNG_WITH_KOST = """
            SELECT r.pk as rechnung_id,r.deleted,r.status,r.nummer,r.datum,r.bezahl_datum,r.zahl_betrag,r.faelligkeit,r.discountmaturity,r.discountpercent,
                   p.pk as pos_id,p.number,p.menge,p.einzel_netto,p.vat,p.s_text,p.auftrags_position_fk,
                   k.pk as kost_id,k.netto,k.index,k.kost1_fk,k.kost2_fk
            FROM t_fibu_rechnung r
            LEFT JOIN t_fibu_rechnung_position p ON p.rechnung_fk = r.pk
            LEFT JOIN t_fibu_kost_zuweisung k ON k.rechnungs_pos_fk = p.pk
        """.trimIndent()
        private val SELECT_EINGANGS_RECHNUNG_WITH_KOST = """
            SELECT r.pk as rechnung_id,r.deleted,r.datum,r.bezahl_datum,r.zahl_betrag,r.faelligkeit,r.discountmaturity,r.discountpercent,
                   p.pk as pos_id,p.number,p.menge,p.einzel_netto,p.vat,p.s_text,
                   k.pk as kost_id,k.netto,k.index,k.kost1_fk,k.kost2_fk
            FROM t_fibu_eingangsrechnung r
            LEFT JOIN t_fibu_eingangsrechnung_position p ON p.eingangsrechnung_fk = r.pk
            LEFT JOIN t_fibu_kost_zuweisung k ON k.eingangsrechnungs_pos_fk = p.pk
        """.trimIndent()
    }
}
