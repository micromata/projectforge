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

package org.projectforge.business.book

import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.search.annotations.*
import org.hibernate.search.annotations.Index
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import javax.persistence.*

/**
 * For managing libraries including lend-out functionality.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_BOOK",
        uniqueConstraints = [UniqueConstraint(columnNames = ["signature", "tenant_id"])],
        indexes = [javax.persistence.Index(name = "idx_fk_t_book_lend_out_by",
                columnList = "lend_out_by"), javax.persistence.Index(name = "idx_fk_t_book_tenant_id",
                columnList = "tenant_id"), javax.persistence.Index(name = "t_book_pkey", columnList = "pk")])
@NamedQueries(
        NamedQuery(name = BookDO.FIND_BY_SIGNATURE, query = "from BookDO where signature=:signature"),
        NamedQuery(name = BookDO.FIND_OTHER_BY_SIGNATURE, query = "from BookDO where signature=:signature and id<>:id"))
open class BookDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "book.title", required = true)
    @Field
    @get:Column(length = 255)
    open var title: String? = null

    @PropertyInfo(i18nKey = "book.keywords")
    @Field
    @get:Column(length = 1024)
    open var keywords: String? = null

    @PropertyInfo(i18nKey = "book.lendOutBy")
    @IndexedEmbedded(depth = 1, includePaths = ["username", "firstname", "lastname"])
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:Fetch(FetchMode.SELECT)
    @get:JoinColumn(name = "lend_out_by")
    open var lendOutBy: PFUserDO? = null

    @PropertyInfo(i18nKey = "date")
    @Field(index = Index.YES, analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "lend_out_date")
    open var lendOutDate: Date? = null

    @PropertyInfo(i18nKey = "book.lendOutNote")
    @Field
    @get:Column(name = "lend_out_comment", length = 1024)
    open var lendOutComment: String? = null

    @PropertyInfo(i18nKey = "book.isbn")
    @Field
    @get:Column(length = 255)
    open var isbn: String? = null

    @PropertyInfo(i18nKey = "book.signature")
    @Field
    @get:Column(length = 255)
    open var signature: String? = null

    @PropertyInfo(i18nKey = "book.publisher")
    @Field
    @get:Column(length = 255)
    open var publisher: String? = null

    @PropertyInfo(i18nKey = "book.editor")
    @Field
    @get:Column(length = 255)
    open var editor: String? = null

    @PropertyInfo(i18nKey = "book.yearOfPublishing")
    @Field(index = Index.YES, store = Store.NO, name = "year")
    @get:Column(name = "year_of_publishing", length = 4)
    open var yearOfPublishing: String? = null

    @PropertyInfo(i18nKey = "book.authors")
    @Field
    @get:Column(length = 1000)
    open var authors: String? = null

    @PropertyInfo(i18nKey = "book.abstract")
    @Field(index = Index.YES, store = Store.NO, name = "abstract")
    @get:Column(name = "abstract_text", length = 4000)
    open var abstractText: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 1000)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "status")
    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: BookStatus? = null

    @PropertyInfo(i18nKey = "book.type")
    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "book_type", length = 20, nullable = true)
    open var type: BookType? = null

    /**
     * Converts numbers in signature for alphanumeric sorting in 5-digit form. For example: "WT-145a" -&gt; "WT-00145a".
     */
    val signature4Sort: String?
        @Transient
        get() {
            if (this.signature == null) {
                return null
            }
            val buf = StringBuffer()
            var no: StringBuffer? = null
            for (i in 0 until this.signature!!.length) {
                val ch = this.signature!![i]
                if (!Character.isDigit(ch)) {
                    if (no != null && no.isNotEmpty()) {
                        buf.append(StringUtils.leftPad(no.toString(), 5, '0'))
                        no = null
                    }
                    buf.append(ch)
                } else {
                    if (no == null) {
                        no = StringBuffer()
                    }
                    no.append(ch)
                }
            }
            if (no != null && no.isNotEmpty()) {
                buf.append(StringUtils.leftPad(no.toString(), 5, '0'))
            }
            return buf.toString()
        }

    companion object {
        internal const val FIND_BY_SIGNATURE = "BookDO_FindBySignature"

        internal const val FIND_OTHER_BY_SIGNATURE = "BookDO_FindOtherBySignature"
    }
}
