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

package org.projectforge.plugins.liquidityplanning;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropertyType;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import java.math.BigDecimal;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Beside entries of debitors and creditors invoices additional entries (for accommodation, taxes, planned salaries,
 * assurance etc.) are important for a complete liquidity planning.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_LIQUI_ENTRY", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_plugin_liqui_entry_tenant_id", columnList = "tenant_id")
})
public class LiquidityEntryDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 6006883617791360816L;

  @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.dateOfPayment")
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date dateOfPayment;

  @PropertyInfo(i18nKey = "fibu.common.betrag", type = PropertyType.CURRENCY)
  private BigDecimal amount;

  @PropertyInfo(i18nKey = "fibu.rechnung.status.bezahlt")
  private boolean paid;

  @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String subject;

  @PropertyInfo(i18nKey = "comment")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @Column(length = Constants.LENGTH_TITLE)
  public String getSubject()
  {
    return subject;
  }

  /**
   * @param title
   * @return this for chaining.
   */
  public LiquidityEntryDO setSubject(final String subject)
  {
    this.subject = subject;
    return this;
  }

  /**
   * @return the dateOfPayment
   */
  @Column(name = "date_of_payment")
  public Date getDateOfPayment()
  {
    return dateOfPayment;
  }

  /**
   * @return this for chaining.
   */
  /**
   * @param dateOfPayment the dateOfPayment to set
   * @return this for chaining.
   */
  public LiquidityEntryDO setDateOfPayment(final Date dateOfPayment)
  {
    this.dateOfPayment = dateOfPayment;
    return this;
  }

  @Column(scale = 2, precision = 12)
  public BigDecimal getAmount()
  {
    return amount;
  }

  public LiquidityEntryDO setAmount(final BigDecimal amount)
  {
    this.amount = amount;
    return this;
  }

  /**
   * @return the paid
   */
  @Column
  public boolean isPaid()
  {
    return paid;
  }

  /**
   * @param paid the paid to set
   * @return this for chaining.
   */
  public LiquidityEntryDO setPaid(final boolean paid)
  {
    this.paid = paid;
    return this;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getComment()
  {
    return comment;
  }

  public LiquidityEntryDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }
}
