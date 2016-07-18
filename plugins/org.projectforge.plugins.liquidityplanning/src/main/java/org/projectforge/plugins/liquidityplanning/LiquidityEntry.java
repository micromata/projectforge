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

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropertyType;

/**
 * Represents entities of {@link LiquidityEntryDO}, {@link RechnungDO} and {@link EingangsrechnungDO}.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityEntry implements Serializable
{
  private static final long serialVersionUID = 8878006067746874578L;

  @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.dateOfPayment")
  private Date dateOfPayment;

  @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.expectedDateOfPayment")
  private Date expectedDateOfPayment;

  @PropertyInfo(i18nKey = "fibu.common.betrag", type = PropertyType.CURRENCY)
  private BigDecimal amount;

  @PropertyInfo(i18nKey = "fibu.rechnung.status.bezahlt")
  private boolean paid;

  @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
  private String subject;

  @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.type")
  private LiquidityEntryType type;

  @PropertyInfo(i18nKey = "comment")
  private String comment;

  public Date getDateOfPayment()
  {
    return dateOfPayment;
  }

  public LiquidityEntry setDateOfPayment(final Date date)
  {
    this.dateOfPayment = date;
    return this;
  }

  /**
   * For invoices (debitor) the expected date of payment is calculated from previous time of payments of former paid invoices from the same
   * debitor.
   * @return the expectedDateOfPayment
   */
  public Date getExpectedDateOfPayment()
  {
    return expectedDateOfPayment;
  }

  /**
   * @param expectedDateOfPayment the expectedDateOfPayment to set
   * @return this for chaining.
   */
  public LiquidityEntry setExpectedDateOfPayment(final Date expectedDateOfPayment)
  {
    this.expectedDateOfPayment = expectedDateOfPayment;
    return this;
  }

  public BigDecimal getAmount()
  {
    return amount;
  }

  public LiquidityEntry setAmount(final BigDecimal amount)
  {
    this.amount = amount;
    return this;
  }

  public String getSubject()
  {
    return subject;
  }

  public LiquidityEntry setSubject(final String subject)
  {
    this.subject = subject;
    return this;
  }

  public boolean isPaid()
  {
    return paid;
  }

  public LiquidityEntry setPaid(final boolean paid)
  {
    this.paid = paid;
    return this;
  }

  public LiquidityEntry setType(final LiquidityEntryType type)
  {
    this.type = type;
    return this;
  }

  public LiquidityEntryType getType()
  {
    return type;
  }

  /**
   * @return the comment
   */
  public String getComment()
  {
    return comment;
  }

  /**
   * @param comment the comment to set
   * @return this for chaining.
   */
  public LiquidityEntry setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }
}
