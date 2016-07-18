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

package org.projectforge.business.fibu;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.IManualIndex;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO;

/**
 * Jeder Kunde bei Micromata hat eine Kundennummer. Die Kundennummer ist Bestandteil von KOST2 (2.-4. Ziffer). Aufträge
 * aus dem Auftragsbuch, sowie Rechnungen etc. werden Kunden zugeordnet.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KUNDE",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_kunde_konto_id", columnList = "konto_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_kunde_tenant_id", columnList = "tenant_id")
    })
@Analyzer(impl = ClassicAnalyzer.class)
public class KundeDO extends AbstractHistorizableBaseDO<Integer> implements ShortDisplayNameCapable, IManualIndex
{
  private static final long serialVersionUID = -2138613066430251341L;

  public static final int MAX_ID = 999;

  private Integer id;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String identifier;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String division;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private KundeStatus status;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  private KontoDO konto;

  /**
   * @return "5.###" ("5.<kunde id>")
   */
  @Transient
  public String getKost()
  {
    return "5." + KostFormatter.format3Digits(id);
  }

  /**
   * 1. Ziffer des Kostenträgers: Ist für Kunden immer 5.
   * 
   * @return 5
   */
  @Transient
  public int getNummernkreis()
  {
    return 5;
  }

  /**
   * Kundennummer.
   * 
   * @see #getId()
   */
  @Transient
  public Integer getBereich()
  {
    return id;
  }

  /** Ziffer 2-4 von KOST2 (000-999). Ist der primary key. */
  @Override
  @Id
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  @Override
  public void setId(final Integer id)
  {
    this.id = id;
  }

  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * The identifier is used e. g. for display the project as short name in human resources planning tables.
   * 
   * @return
   */
  @Column(length = 20)
  public String getIdentifier()
  {
    return identifier;
  }

  public void setIdentifier(final String identifier)
  {
    this.identifier = identifier;
  }

  /**
   * @return Identifier if exists otherwise name of project.
   */
  @Transient
  public String getKundeIdentifierDisplayName()
  {
    if (StringUtils.isNotBlank(this.identifier) == true) {
      return this.identifier;
    }
    return this.name;
  }

  @Column(length = 255)
  public String getDivision()
  {
    return division;
  }

  public void setDivision(final String division)
  {
    this.division = division;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  public KundeStatus getStatus()
  {
    return status;
  }

  public void setStatus(final KundeStatus status)
  {
    this.status = status;
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

  /**
   * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable#getShortDisplayName()
   * @see KostFormatter#format(KundeDO)
   */
  @Override
  @Transient
  public String getShortDisplayName()
  {
    return KostFormatter.formatKunde(this);
  }

  /**
   * This Datev account number is used for the exports of invoices. This account numbers may-be overwritten by the
   * ProjektDO which is assigned to an invoice.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "konto_id")
  public KontoDO getKonto()
  {
    return konto;
  }

  public void setKonto(final KontoDO konto)
  {
    this.konto = konto;
  }

  @Transient
  public Integer getKontoId()
  {
    return konto != null ? konto.getId() : null;
  }
}
