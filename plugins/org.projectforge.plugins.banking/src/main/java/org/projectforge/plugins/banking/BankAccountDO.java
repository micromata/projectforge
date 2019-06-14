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

package org.projectforge.plugins.banking;

import de.micromata.genome.db.jpa.history.api.WithHistory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

@Entity
@Indexed
@Table(name = "T_PLUGIN_BANK_ACCOUNT",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "account_number", "tenant_id" }) },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_bank_account_tenant_id", columnList = "tenant_id")
    })
@WithHistory
public class BankAccountDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = -6492718816678384637L;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String accountNumber;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String bank;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String bankIdentificationCode;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @Column(name = "account_number", length = 255, nullable = false)
  public String getAccountNumber()
  {
    return accountNumber;
  }

  public void setAccountNumber(final String accountNumber)
  {
    this.accountNumber = accountNumber;
  }

  @Column(length = 255)
  public String getBank()
  {
    return bank;
  }

  public void setBank(final String bank)
  {
    this.bank = bank;
  }

  @Column(name = "bank_identification_code", length = 100)
  public String getBankIdentificationCode()
  {
    return bankIdentificationCode;
  }

  public void setBankIdentificationCode(final String bankIdentificationCode)
  {
    this.bankIdentificationCode = bankIdentificationCode;
  }

  @Column(length = 255)
  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public void setDescription(final String description)
  {
    this.description = description;
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return String.valueOf(accountNumber);
  }
}
