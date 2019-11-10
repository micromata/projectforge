/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.plugintemplate.model

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Indexed
@Table(name = "T_PLUGIN_PLUGINTEMPLATE")
@WithHistory
open class PluginTemplateDO : DefaultBaseDO() {

    @Field
    @PropertyInfo(i18nKey = "plugins.plugintemplate.key")
    @get:Column(nullable = false)
    open var key: String? = null

    @Field
    @PropertyInfo(i18nKey = "plugins.plugintemplate.value")
    @get:Column
    open var value: String? = null

}
