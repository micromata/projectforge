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

package org.projectforge.framework.persistence.user.entities

import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table
import javax.persistence.Transient

import org.hibernate.Hibernate
import org.hibernate.search.annotations.ContainedIn
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.multitenancy.TenantTableTruncater
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.micromata.genome.jpa.impl.ATableTruncater

/**
 * Represents a single tenant (client) for multi-tenancy.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_TENANT")
@ATableTruncater(TenantTableTruncater::class)
class TenantDO : DefaultBaseDO(), ShortDisplayNameCapable {

    private val log = LoggerFactory.getLogger(TenantDO::class.java)

    /**
     * The short name is the display name.
     */
    @get:Column(length = 100)
    var shortName: String? = null

    /**
     * @return the name
     */
    @get:Column(length = 255)
    var name: String? = null

    /**
     * @return the description
     */
    @get:Column(length = 4000)
    var description: String? = null

    /**
     * No or only one default tenant should be exist. All entities in the database without a given tenant_id are
     * automatically assigned to this tenant. This feature should only be used for ProjectForge installations migrated
     * from single tenant into a multi tenancy installation.
     */
    @get:Column(name = "default_tenant")
    var defaultTenant: Boolean? = null

    private var usernames: String? = null

    @ContainedIn
    @IndexedEmbedded(depth = 1)
    @get:ManyToMany(targetEntity = org.projectforge.framework.persistence.user.entities.PFUserDO::class, cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @get:JoinTable(name = "T_TENANT_USER", joinColumns = [JoinColumn(name = "TENANT_ID")], inverseJoinColumns = [JoinColumn(name = "USER_ID")])
    var assignedUsers: MutableSet<PFUserDO>? = null

    /**
     * Returns the collection of assigned users only if initialized. Avoids a LazyInitializationException.
     *
     * @return
     */
    val safeAssignedUsers: Set<PFUserDO>?
        @Transient
        get() = if (this.assignedUsers == null || !Hibernate.isInitialized(this.assignedUsers)) {
            null
        } else this.assignedUsers

    /**
     * @return the defaultTenant
     */
    val isDefault: Boolean
        @Transient
        get() = defaultTenant != null && defaultTenant == true

    fun addUser(user: PFUserDO) {
        if (this.assignedUsers == null) {
            this.assignedUsers = HashSet()
        }
        this.assignedUsers!!.add(user)
    }

    /**
     * TODO RK DESIGN BUG doesn't belong to DO.
     *
     * @return
     */
    @Transient
    @Deprecated("org.projectforge.business.multitenancy.TenantDao.getUsernameCommaList(TenantDO)\n" +
            "    ")
    fun getUsernames(): String {
        if (usernames != null) {
            return usernames!!
        }

        //    final TenantDO tenant = Registry.instance().getTenantsCache().getTenant(getId()); // If not initialized.
        //    if (tenant.getAssignedUsers() == null) {
        //      return "";
        //    }
        //    final List<String> list = new ArrayList<String>();
        //    for (final PFUserDO user : tenant.getAssignedUsers()) {
        //      if (user != null) {
        //        list.add(user.getUsername());
        //      }
        //    }
        //    usernames = StringHelper.listToString(list, ", ", true);

        usernames = "DESIGN_BUG_LIFES_HERE"
        log.error("org.projectforge.business.multitenancy.TenantDO.getUsernames() is deprecated")
        return usernames!!
    }

    /**
     * @param tenant Parameter will be ignored, this is used as tenant to set instead.
     * @see org.projectforge.framework.persistence.api.BaseDO.setTenant
     */
    override fun setTenant(tenant: TenantDO): TenantDO {
        super.setTenant(this)
        return this
    }

    /**
     * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable.getShortDisplayName
     */
    @Transient
    override fun getShortDisplayName(): String? {
        return name
    }
}
