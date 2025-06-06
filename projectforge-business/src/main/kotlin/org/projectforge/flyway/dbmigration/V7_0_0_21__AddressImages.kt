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

package org.projectforge.flyway.dbmigration

import mu.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.projectforge.framework.utils.Crypt
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.util.*

private val log = KotlinLogging.logger {}


/**
 * Address images were moved to separate entity (db table). So do the migration here.
 */
class V7_0_0_21__AddressImages : BaseJavaMigration() {
    class MyAddress(val addressId: Int?, val name: String?, val image: ByteArray?, val imagePreview: ByteArray?)

    override fun migrate(context: Context) {
        log.info("Migrating images of addresses...'")
        val ds = context.configuration.dataSource
        val jdbc = JdbcTemplate(ds)
        var counter = 0
        val now = Date()
        val rowMapper = RowMapper { rs, _ ->
            MyAddress(
                    rs.getInt("addressId"),
                    rs.getString("name"),
                    rs.getBytes("imagedata"),
                    rs.getBytes("image_data_preview")
            )
        }
        jdbc.query("select a.pk as addressId, a.name as name, a.imagedata as imagedata, a.image_data_preview as image_data_preview from t_address as a",
                rowMapper)
                .forEach {
                    if (it.image != null) {
                        log.info { "Migrating images of address '${it.name}'..." }
                        val parameters = mutableMapOf<String, Any?>()
                        parameters["pk"] = ++counter
                        parameters["address_fk"] = it.addressId
                        parameters["last_update"] = now
                        parameters["image"] = it.image
                        parameters["image_preview"] = it.imagePreview
                        SimpleJdbcInsert(ds).withTableName("T_ADDRESS_IMAGE").execute(parameters)
                        jdbc.update("update T_ADDRESS set image=? where pk=?", true, it.addressId)
                    }
                }

        if (counter > 0) { // counter > 0
            log.info("Number of successful migrated images: $counter")
        } else {
            log.info("No images found to migrate (OK, if no address images exist in the data base).")
        }
    }
}
