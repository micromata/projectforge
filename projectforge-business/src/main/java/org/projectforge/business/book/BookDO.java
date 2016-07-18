/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.book;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_BOOK",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "signature", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_book_lend_out_by", columnList = "lend_out_by"),
        @javax.persistence.Index(name = "idx_fk_t_book_task_id", columnList = "task_id"),
        @javax.persistence.Index(name = "idx_fk_t_book_tenant_id", columnList = "tenant_id"),
        @javax.persistence.Index(name = "t_book_pkey", columnList = "pk")
    })
public class BookDO extends DefaultBaseDO
{
  // private static final Logger log = Logger.getLogger(TaskDO.class);

  private static final long serialVersionUID = 8036741307214351813L;
  @IndexedEmbedded(depth = 1, includePaths = { "title" })
  private TaskDO task;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title; // 255 not null

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String keywords; // 1024

  @IndexedEmbedded(depth = 1, includePaths = { "username", "firstname", "lastname" })
  private PFUserDO lendOutBy;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date lendOutDate;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String lendOutComment; // 1024

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String isbn; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String signature; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String publisher; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String editor; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO, name = "year")
  private String yearOfPublishing; // 4

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String authors; // 1000

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO, name = "abstract")
  private String abstractText; // 4000

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment; // 4000;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private BookStatus status;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private BookType type;

  @Column(length = 255)
  public String getPublisher()
  {
    return publisher;
  }

  public BookDO setPublisher(final String publisher)
  {
    this.publisher = publisher;
    return this;
  }

  @Column(length = 255)
  public String getEditor()
  {
    return editor;
  }

  public BookDO setEditor(final String editor)
  {
    this.editor = editor;
    return this;
  }

  @Column(length = 1000)
  public String getAuthors()
  {
    return authors;
  }

  public BookDO setAuthors(final String authors)
  {
    this.authors = authors;
    return this;
  }

  @Column(length = 1000)
  public String getComment()
  {
    return comment;
  }

  public BookDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Column(length = 255)
  public String getIsbn()
  {
    return isbn;
  }

  public BookDO setIsbn(final String isbn)
  {
    this.isbn = isbn;
    return this;
  }

  @Column(length = 255)
  public String getSignature()
  {
    return signature;
  }

  public BookDO setSignature(final String signature)
  {
    this.signature = signature;
    return this;
  }

  /**
   * Converts numbers in signature for alphanumeric sorting in 5-digit form. For example: "WT-145a" -&gt; "WT-00145a".
   */
  @Transient
  public String getSignature4Sort()
  {
    if (this.signature == null) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    StringBuffer no = null;
    for (int i = 0; i < this.signature.length(); i++) {
      final char ch = this.signature.charAt(i);
      if (Character.isDigit(ch) == false) {
        if (no != null && no.length() > 0) {
          buf.append(StringUtils.leftPad(no.toString(), 5, '0'));
          no = null;
        }
        buf.append(ch);
      } else {
        if (no == null) {
          no = new StringBuffer();
        }
        no.append(ch);
      }
    }
    if (no != null && no.length() > 0) {
      buf.append(StringUtils.leftPad(no.toString(), 5, '0'));
    }
    return buf.toString();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "lend_out_by")
  public PFUserDO getLendOutBy()
  {
    return lendOutBy;
  }

  public BookDO setLendOutBy(final PFUserDO lendOutBy)
  {
    this.lendOutBy = lendOutBy;
    return this;
  }

  @Transient
  public Integer getLendOutById()
  {
    if (this.lendOutBy == null) {
      return null;
    }
    return lendOutBy.getId();
  }

  @Column(name = "lend_out_date")
  public Date getLendOutDate()
  {
    return lendOutDate;
  }

  public BookDO setLendOutDate(final Date lendOutDate)
  {
    this.lendOutDate = lendOutDate;
    return this;
  }

  @Column(name = "lend_out_comment", length = 1024)
  public String getLendOutComment()
  {
    return lendOutComment;
  }

  public BookDO setLendOutComment(final String lendOutComment)
  {
    this.lendOutComment = lendOutComment;
    return this;
  }

  @Column(length = 1024)
  public String getKeywords()
  {
    return keywords;
  }

  public BookDO setKeywords(final String keywords)
  {
    this.keywords = keywords;
    return this;
  }

  @Column(name = "abstract_text", length = 4000)
  public String getAbstractText()
  {
    return abstractText;
  }

  public BookDO setAbstractText(final String abstractText)
  {
    this.abstractText = abstractText;
    return this;
  }

  /**
   * Not used as object due to performance reasons.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  public TaskDO getTask()
  {
    return task;
  }

  public BookDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  @Transient
  public Integer getTaskId()
  {
    if (this.task == null) {
      return null;
    }
    return task.getId();
  }

  @Column(length = 255)
  public String getTitle()
  {
    return title;
  }

  public BookDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @Column(name = "year_of_publishing", length = 4)
  public String getYearOfPublishing()
  {
    return yearOfPublishing;
  }

  public BookDO setYearOfPublishing(final String yearOfPublishing)
  {
    this.yearOfPublishing = yearOfPublishing;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  public BookStatus getStatus()
  {
    return status;
  }

  public BookDO setStatus(final BookStatus status)
  {
    this.status = status;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "book_type", length = 20, nullable = true)
  public BookType getType()
  {
    return type;
  }

  public BookDO setType(final BookType type)
  {
    this.type = type;
    return this;
  }
}
