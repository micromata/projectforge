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

package org.projectforge.business.orga;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTRACT", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "number", "tenant_id" }) }, indexes = {
    @javax.persistence.Index(name = "idx_fk_t_contract_tenant_id", columnList = "tenant_id")
})
public class ContractDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1399338188515793833L;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
      bridge = @FieldBridge(impl = IntegerBridge.class))
  private Integer number;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date date;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date validFrom;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date validUntil;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String coContractorA;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String contractPersonA;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String signerA;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String coContractorB;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String contractPersonB;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String signerB;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date signingDate;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String type;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String status;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String text;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String reference;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String filing;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date resubmissionOnDate;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date dueDate;

  /**
   * consecutively numbered.
   */
  @Column(nullable = true)
  public Integer getNumber()
  {
    return number;
  }

  public ContractDO setNumber(final Integer number)
  {
    this.number = number;
    return this;
  }

  @Column(name = "c_date")
  public Date getDate()
  {
    return date;
  }

  public ContractDO setDate(final Date date)
  {
    this.date = date;
    return this;
  }

  /**
   * @return Start of the validity period.
   */
  @Column(name = "valid_from")
  public Date getValidFrom()
  {
    return validFrom;
  }

  public ContractDO setValidFrom(final Date validFrom)
  {
    this.validFrom = validFrom;
    return this;
  }

  /**
   * @return End of the validity period.
   */
  @Column(name = "valid_until")
  public Date getValidUntil()
  {
    return validUntil;
  }

  public ContractDO setValidUntil(final Date validUntil)
  {
    this.validUntil = validUntil;
    return this;
  }

  @Column(length = 1000)
  public String getTitle()
  {
    return title;
  }

  public ContractDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  /**
   * Types (as free texts) are configurable in ProjectForge's config file.
   */
  @Column(length = 100)
  public String getType()
  {
    return type;
  }

  public ContractDO setType(final String type)
  {
    this.type = type;
    return this;
  }

  @Column(length = 100)
  public String getStatus()
  {
    return status;
  }

  public ContractDO setStatus(final String status)
  {
    this.status = status;
    return this;
  }

  @Column(length = 4000)
  public String getText()
  {
    return text;
  }

  public ContractDO setText(final String text)
  {
    this.text = text;
    return this;
  }

  @Column(length = 1000)
  public String getReference()
  {
    return reference;
  }

  public ContractDO setReference(final String reference)
  {
    this.reference = reference;
    return this;
  }

  @Column(name = "resubmission_on_date")
  public Date getResubmissionOnDate()
  {
    return resubmissionOnDate;
  }

  public ContractDO setResubmissionOnDate(final Date resubmissionOnDate)
  {
    this.resubmissionOnDate = resubmissionOnDate;
    return this;
  }

  @Column(name = "due_date")
  public Date getDueDate()
  {
    return dueDate;
  }

  public ContractDO setDueDate(final Date dueDate)
  {
    this.dueDate = dueDate;
    return this;
  }

  @Column(length = 1000)
  public String getFiling()
  {
    return filing;
  }

  public ContractDO setFiling(final String filing)
  {
    this.filing = filing;
    return this;
  }

  @Column(length = 1000, name = "co_contractor_a")
  public String getCoContractorA()
  {
    return coContractorA;
  }

  public ContractDO setCoContractorA(final String coContractorA)
  {
    this.coContractorA = coContractorA;
    return this;
  }

  @Column(length = 1000, name = "contract_person_a")
  public String getContractPersonA()
  {
    return contractPersonA;
  }

  public ContractDO setContractPersonA(final String contractPersonA)
  {
    this.contractPersonA = contractPersonA;
    return this;
  }

  @Column(length = 1000, name = "signer_a")
  public String getSignerA()
  {
    return signerA;
  }

  public ContractDO setSignerA(final String signerA)
  {
    this.signerA = signerA;
    return this;
  }

  @Column(length = 1000, name = "co_contractor_b")
  public String getCoContractorB()
  {
    return coContractorB;
  }

  public ContractDO setCoContractorB(final String coContractorB)
  {
    this.coContractorB = coContractorB;
    return this;
  }

  @Column(length = 1000, name = "contract_person_b")
  public String getContractPersonB()
  {
    return contractPersonB;
  }

  public ContractDO setContractPersonB(final String contractPersonB)
  {
    this.contractPersonB = contractPersonB;
    return this;
  }

  @Column(length = 1000, name = "signer_b")
  public String getSignerB()
  {
    return signerB;
  }

  public ContractDO setSignerB(final String signerB)
  {
    this.signerB = signerB;
    return this;
  }

  @Column(name = "signing_date")
  public Date getSigningDate()
  {
    return signingDate;
  }

  public ContractDO setSigningDate(final Date signingDate)
  {
    this.signingDate = signingDate;
    return this;
  }
}
