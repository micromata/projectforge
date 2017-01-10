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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * Projekte sind Kunden zugeordnet und haben eine zweistellige Nummer. Sie sind Bestandteile von KOST2 (5. und 6.
 * Ziffer).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "kost2", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchProjectKostBridge.class)
@Table(name = "T_FIBU_PROJEKT",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "nummer", "kunde_id", "tenant_id" }),
        @UniqueConstraint(columnNames = { "nummer", "intern_kost2_4", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_konto_id", columnList = "konto_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_kunde_id", columnList = "kunde_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_projektmanager_group_fk",
            columnList = "projektmanager_group_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_projectManager_fk", columnList = "projectmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_headofbusinessmanager_fk", columnList = "headofbusinessmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_salesmanager_fk", columnList = "salesmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_task_fk", columnList = "task_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_projekt_tenant_id", columnList = "tenant_id")
    })
@WithHistory
public class ProjektDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = 7842680360705785761L;

  @Field(index = Index.YES, analyze = Analyze.NO/* UN_TOKENIZED */, store = Store.NO)
  private int nummer;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String identifier;

  @IndexedEmbedded(depth = 1)
  private KundeDO kunde;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private Integer internKost2_4;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private ProjektStatus status;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @IndexedEmbedded(depth = 1)
  private GroupDO projektManagerGroup;

  @IndexedEmbedded(depth = 1)
  private PFUserDO projectManager;

  @IndexedEmbedded(depth = 1)
  private PFUserDO headOfBusinessManager;

  @IndexedEmbedded(depth = 1)
  private PFUserDO salesManager;

  private TaskDO task;

  private KontoDO konto;

  @Transient
  public String getKost()
  {
    return KostFormatter.format(this);
  }

  /**
   * 1. Ziffer des Kostenträgers: Ist 4 für interne Projekte (kunde nicht gegeben) ansonsten 5.
   */
  @Transient
  public int getNummernkreis()
  {
    return kunde != null ? 5 : 4;
  }

  /**
   * Wenn Kunde gesetzt ist, wird die Kundennummer, ansonsten internKost2_4 zurückgegeben.
   */
  @Transient
  public Integer getBereich()
  {
    return kunde != null ? kunde.getId() : internKost2_4;
  }

  /**
   * Ziffer 5-6 von KOST2 (00-99)
   */
  @Column(nullable = false)
  public int getNummer()
  {
    return nummer;
  }

  public ProjektDO setNummer(final int nummer)
  {
    this.nummer = nummer;
    return this;
  }

  /**
   * @see #getNummer
   */
  @Transient
  public int getTeilbereich()
  {
    return nummer;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kunde_id")
  public KundeDO getKunde()
  {
    return kunde;
  }

  public ProjektDO setKunde(final KundeDO kunde)
  {
    this.kunde = kunde;
    return this;
  }

  /**
   * The member of this group have access to orders assigned to this project.
   *
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "projektmanager_group_fk")
  public GroupDO getProjektManagerGroup()
  {
    return projektManagerGroup;
  }

  @Transient
  public Integer getProjektManagerGroupId()
  {
    return projektManagerGroup != null ? projektManagerGroup.getId() : null;
  }

  public ProjektDO setProjektManagerGroup(final GroupDO projektManagerGroup)
  {
    this.projektManagerGroup = projektManagerGroup;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "projectmanager_fk")
  public PFUserDO getProjectManager()
  {
    return projectManager;
  }

  @Transient
  public Integer getProjectManagerId()
  {
    return projectManager != null ? projectManager.getId() : null;
  }

  public ProjektDO setProjectManager(final PFUserDO projectManager)
  {
    this.projectManager = projectManager;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "headofbusinessmanager_fk")
  public PFUserDO getHeadOfBusinessManager()
  {
    return headOfBusinessManager;
  }

  @Transient
  public Integer getHeadOfBusinessManagerId()
  {
    return headOfBusinessManager != null ? headOfBusinessManager.getId() : null;
  }

  public ProjektDO setHeadOfBusinessManager(final PFUserDO headOfBusinessManager)
  {
    this.headOfBusinessManager = headOfBusinessManager;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salesmanager_fk")
  public PFUserDO getSalesManager()
  {
    return salesManager;
  }

  @Transient
  public Integer getSalesManagerId()
  {
    return salesManager != null ? salesManager.getId() : null;
  }

  public ProjektDO setSalesManager(final PFUserDO salesManager)
  {
    this.salesManager = salesManager;
    return this;
  }

  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public ProjektDO setName(final String name)
  {
    this.name = name;
    return this;
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

  public ProjektDO setIdentifier(final String identifier)
  {
    this.identifier = identifier;
    return this;
  }

  /**
   * @return Identifier if exists otherwise name of project.
   */
  @Transient
  public String getProjektIdentifierDisplayName()
  {
    if (StringUtils.isNotBlank(this.identifier) == true) {
      return this.identifier;
    }
    return this.name;
  }

  @Transient
  public Integer getKundeId()
  {
    if (this.kunde == null) {
      return null;
    }
    return kunde.getId();
  }

  /**
   * Nur bei internen Projekten ohne Kundennummer, stellt diese Nummer die Ziffern 2-4 aus 4.* dar.
   */
  @Column(name = "intern_kost2_4")
  public Integer getInternKost2_4()
  {
    return internKost2_4;
  }

  public ProjektDO setInternKost2_4(final Integer internKost2_4)
  {
    this.internKost2_4 = internKost2_4;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  public ProjektStatus getStatus()
  {
    return status;
  }

  public ProjektDO setStatus(final ProjektStatus status)
  {
    this.status = status;
    return this;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public ProjektDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_fk", nullable = true)
  public TaskDO getTask()
  {
    return task;
  }

  public ProjektDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  @Transient
  public Integer getTaskId()
  {
    return this.task != null ? task.getId() : null;
  }

  /**
   * This Datev account number is used for the exports of invoices. If not given then the account number assigned to the
   * KundeDO is used instead (default).
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

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return KostFormatter.formatProjekt(this);
  }
}
