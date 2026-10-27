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

package org.projectforge.business.user

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.business.address.ImageType
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.Date

/**
 * The avatar of a user: a square picture (at most 400 px, see [UserAvatarDao]) and a small preview of it for the
 * chat and the user menu. At most one per user.
 */
@Entity
@Table(
    name = "T_PF_USER_AVATAR",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_pf_user_avatar_user", columnNames = ["user_fk"])],
)
open class UserAvatarDO : IdObject<Long> {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    override open var id: Long? = null

    @get:Column(name = "last_update")
    open var lastUpdate: Date? = null

    @get:OneToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @get:Column(columnDefinition = "BLOB")
    @get:Basic(fetch = FetchType.LAZY)
    open var image: ByteArray? = null

    @get:Column(name = "image_preview", columnDefinition = "BLOB")
    @get:Basic(fetch = FetchType.LAZY)
    open var imagePreview: ByteArray? = null

    @get:Column(name = "image_type", length = 5)
    @get:Enumerated(EnumType.STRING)
    open var imageType: ImageType? = null
}
