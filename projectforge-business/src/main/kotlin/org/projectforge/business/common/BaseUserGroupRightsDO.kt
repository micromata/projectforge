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

package org.projectforge.business.common

import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.Column
import javax.persistence.MappedSuperclass
import javax.persistence.Transient

/**
 * Base class for objects supporting user and group specific rights. You may define single group and user ids for the
 * different access types, such as owner, full access, readonly access and minimal access.
 */
@MappedSuperclass
abstract class BaseUserGroupRightsDO : DefaultBaseDO() {

    /**
     * The owner of this object.
     */
    @get:Transient
    abstract val owner: PFUserDO?


    /**
     * The id of the owner if exist, otherwise null.
     */
    val ownerId
        @Transient
        get() = owner?.id

    /**
     * Members of these groups have full read/write access to all entries of this object.
     */
    @get:Column(name = "full_access_group_ids", nullable = true)
    open var fullAccessGroupIds: String? = null

    @get:Column(name = "full_access_user_ids", nullable = true)
    open var fullAccessUserIds: String? = null

    /**
     * Members of these groups have full read-only access to all entries of this object.
     */
    @get:Column(name = "readonly_access_group_ids", nullable = true)
    open var readonlyAccessGroupIds: String? = null

    /**
     * These users have full read-only access to all entries of this object.
     */
    @get:Column(name = "readonly_access_user_ids", nullable = true)
    open var readonlyAccessUserIds: String? = null

    /**
     * Members of these group have read-only access to all entries of this object, but they can only see a minimal
     * set of the data of this object. This is used e. g. for calendar entries, where the users may only see the
     * start and end time of an event, but no information of details such as location, notes etc.
     */
    @get:Column(name = "minimal_access_group_ids", nullable = true)
    open var minimalAccessGroupIds: String? = null

    /**
     * Members of this group have only access to the start and stop time, nothing else.
     */
    @get:Column(name = "minimal_access_user_ids", nullable = true)
    open var minimalAccessUserIds: String? = null
}
