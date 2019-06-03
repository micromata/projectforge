package org.projectforge.business.common

import javax.persistence.Column
import javax.persistence.MappedSuperclass

import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * Created by blumenstein on 18.07.17.
 */
@MappedSuperclass
open class BaseUserGroupRightsDO : DefaultBaseDO() {

    /**
     * Members of these groups have full read/write access to all entries of this object.
     */
    @get:Column(name = "full_access_group_ids", nullable = true)
    var fullAccessGroupIds: String? = null

    @get:Column(name = "full_access_user_ids", nullable = true)
    var fullAccessUserIds: String? = null

    /**
     * Members of these groups have full read-only access to all entries of this object.
     */
    @get:Column(name = "readonly_access_group_ids", nullable = true)
    var readonlyAccessGroupIds: String? = null

    /**
     * These users have full read-only access to all entries of this object.
     */
    @get:Column(name = "readonly_access_user_ids", nullable = true)
    var readonlyAccessUserIds: String? = null

    /**
     * Members of these group have read-only access to all entries of this object, but they can only see the event start
     * and stop time
     */
    @get:Column(name = "minimal_access_group_ids", nullable = true)
    var minimalAccessGroupIds: String? = null

    /**
     * Members of this group have only access to the start and stop time, nothing else.
     */
    @get:Column(name = "minimal_access_user_ids", nullable = true)
    var minimalAccessUserIds: String? = null
}
