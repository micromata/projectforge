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

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * Repräsentiert einen Auftrag oder ein Angebot. Ein Angebot kann abgelehnt oder durch ein anderes ersetzt werden, muss
 * also nicht zum tatsächlichen Auftrag werden. Wichtig ist: Alle Felder sind historisiert, so dass Änderungen wertvolle
 * Informationen enthalten, wie beispielsweise die Beauftragungshistorie: LOI am 05.03.08 durch Herrn Müller und
 * schriftlich am 04.04.08 durch Beschaffung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_auftrag",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "nummer", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_contact_person_fk", columnList = "contact_person_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_projectManager_fk", columnList = "projectmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_headofbusinessmanager_fk", columnList = "headofbusinessmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_salesmanager_fk", columnList = "salesmanager_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_kunde_fk", columnList = "kunde_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_projekt_fk", columnList = "projekt_fk"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_auftrag_tenant_id", columnList = "tenant_id")
    })
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { AuftragsPositionDO.class, PaymentScheduleDO.class })
public class AuftragDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -3114903689890703366L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AuftragDO.class);

  private Integer nummer;

  /**
   * Dies sind die alten Auftragsnummern oder Kundenreferenzen.
   */
  @Fields({ @Field(index = Index.YES /* TOKENIZED */, name = "referenz_tokenized", store = Store.NO),
      @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO) })
  private String referenz;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  @IndexedEmbedded(depth = 1)
  private List<AuftragsPositionDO> positionen = null;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private AuftragsStatus auftragsStatus;

  @IndexedEmbedded(depth = 1)
  private PFUserDO contactPerson;

  @IndexedEmbedded(depth = 1)
  private KundeDO kunde;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String kundeText;

  @IndexedEmbedded(depth = 2)
  private ProjektDO projekt;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String titel;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String bemerkung;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String statusBeschreibung;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date angebotsDatum;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date erfassungsDatum;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date entscheidungsDatum;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date bindungsFrist;

  private String beauftragungsBeschreibung;

  private Date beauftragungsDatum;

  private BigDecimal fakturiertSum = null;

  @NoHistory
  protected String uiStatusAsXml;

  @NoHistory
  protected AuftragUIStatus uiStatus;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  private List<PaymentScheduleDO> paymentSchedules = null;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceBegin;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date periodOfPerformanceEnd;

  private Integer probabilityOfOccurrence;

  @IndexedEmbedded(depth = 1)
  private PFUserDO projectManager;

  @IndexedEmbedded(depth = 1)
  private PFUserDO headOfBusinessManager;

  @IndexedEmbedded(depth = 1)
  private PFUserDO salesManager;

  /**
   * Datum der Angebotslegung.
   *
   * @return
   */
  @Column(name = "angebots_datum")
  public Date getAngebotsDatum()
  {
    return angebotsDatum;
  }

  public AuftragDO setAngebotsDatum(final Date angebotsDatum)
  {
    this.angebotsDatum = angebotsDatum;
    return this;
  }

  /**
   * Datum der Erfassungslegung.
   *
   * @return
   */
  @Column(name = "erfassungs_datum")
  public Date getErfassungsDatum()
  {
    return erfassungsDatum;
  }

  public AuftragDO setErfassungsDatum(final Date erfassungsDatum)
  {
    this.erfassungsDatum = erfassungsDatum;
    return this;
  }

  /**
   * Datum der Entscheidung / Beauftragung des Kunden.
   *
   * @return
   */
  @Column(name = "entscheidungs_datum")
  public Date getEntscheidungsDatum()
  {
    return entscheidungsDatum;
  }

  public AuftragDO setEntscheidungsDatum(final Date entscheidungsDatum)
  {
    this.entscheidungsDatum = entscheidungsDatum;
    return this;
  }

  @Column(name = "bindungs_frist")
  public Date getBindungsFrist()
  {
    return bindungsFrist;
  }

  public AuftragDO setBindungsFrist(final Date bindungsFrist)
  {
    this.bindungsFrist = bindungsFrist;
    return this;
  }

  /**
   * Wann wurde beauftragt? Beachte: Alle Felder historisiert, so dass hier ein Datum z. B. mit dem LOI und später das
   * Datum der schriftlichen Beauftragung steht.
   */
  @Column(name = "beauftragungs_datum")
  public Date getBeauftragungsDatum()
  {
    return beauftragungsDatum;
  }

  public AuftragDO setBeauftragungsDatum(final Date beauftragungsDatum)
  {
    this.beauftragungsDatum = beauftragungsDatum;
    return this;
  }

  /**
   * Adds all net sums of the positions (without not ordered positions) and return the total sum.
   */
  @Transient
  public BigDecimal getNettoSumme()
  {
    if (positionen == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (final AuftragsPositionDO position : positionen) {
      if (position.isDeleted()) {
        continue;
      }
      final BigDecimal nettoSumme = position.getNettoSumme();
      if (nettoSumme != null && position.getStatus() != AuftragsPositionsStatus.ABGELEHNT && position.getStatus() != AuftragsPositionsStatus.ERSETZT) {
        sum = sum.add(nettoSumme);
      }
    }
    return sum;
  }

  /**
   * Adds all net sums of the positions (only ordered positions) and return the total sum.
   */
  @Transient
  public BigDecimal getBeauftragtNettoSumme()
  {
    if (positionen == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (final AuftragsPositionDO position : positionen) {
      if (position.isDeleted()) {
        continue;
      }
      final BigDecimal nettoSumme = position.getNettoSumme();
      if (nettoSumme != null
          && position.getStatus() != null
          && position.getStatus().isIn(AuftragsPositionsStatus.ABGESCHLOSSEN, AuftragsPositionsStatus.BEAUFTRAGT)) {
        sum = sum.add(nettoSumme);
      }
    }

    return sum;
  }

  /**
   * Auftragsnummer ist eindeutig und wird fortlaufend erzeugt.
   */
  @Column(nullable = false)
  public Integer getNummer()
  {
    return nummer;
  }

  public AuftragDO setNummer(final Integer nummer)
  {
    this.nummer = nummer;
    return this;
  }

  @Column(length = 255)
  public String getReferenz()
  {
    return referenz;
  }

  public AuftragDO setReferenz(final String referenz)
  {
    this.referenz = referenz;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30)
  public AuftragsStatus getAuftragsStatus()
  {
    return auftragsStatus;
  }

  /**
   * @return FAKTURIERT if isVollstaendigFakturiert == true, otherwise AuftragsStatus as String.
   */
  @Transient
  public String getAuftragsStatusAsString()
  {
    if (isVollstaendigFakturiert() == true) {
      return "FAKTURIERT";
    }
    return auftragsStatus != null ? auftragsStatus.toString() : null;
  }

  public AuftragDO setAuftragsStatus(final AuftragsStatus auftragsStatus)
  {
    this.auftragsStatus = auftragsStatus;
    return this;
  }

  /**
   * Wer hat wann und wie beauftragt? Z. B. Beauftragung per E-Mail durch Herrn Müller.
   *
   * @return
   */
  @Column(name = "beauftragungs_beschreibung", length = 4000)
  public String getBeauftragungsBeschreibung()
  {
    return beauftragungsBeschreibung;
  }

  public AuftragDO setBeauftragungsBeschreibung(final String beauftragungsBeschreibung)
  {
    this.beauftragungsBeschreibung = beauftragungsBeschreibung;
    return this;
  }

  @Column(length = 4000, name = "status_beschreibung")
  public String getStatusBeschreibung()
  {
    return statusBeschreibung;
  }

  public AuftragDO setStatusBeschreibung(final String statusBeschreibung)
  {
    this.statusBeschreibung = statusBeschreibung;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kunde_fk", nullable = true)
  public KundeDO getKunde()
  {
    return kunde;
  }

  public AuftragDO setKunde(final KundeDO kunde)
  {
    this.kunde = kunde;
    return this;
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
   * @see ProjektFormatter#formatProjektKundeAsString(ProjektDO, KundeDO, String)
   */
  @Transient
  public String getProjektKundeAsString()
  {
    return ProjektFormatter.formatProjektKundeAsString(this.projekt, this.kunde, this.kundeText);
  }

  /**
   * @see KundeFormatter#formatKundeAsString(KundeDO, String)
   */
  @Transient
  public String getKundeAsString()
  {
    return KundeFormatter.formatKundeAsString(this.kunde, this.kundeText);
  }

  @Transient
  public String getProjektAsString()
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    if (this.projekt != null) {
      if (projekt.getKunde() != null) {
        if (first == true) {
          first = false;
        } else {
          buf.append("; ");
        }
        buf.append(projekt.getKunde().getName());
      }
      if (StringUtils.isNotBlank(projekt.getName()) == true) {
        if (first == true) {
          first = false;
        } else {
          buf.append(" - ");
        }
        buf.append(projekt.getName());
      }
    }
    return buf.toString();
  }

  /**
   * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
   */
  @Column(name = "kunde_text", length = 1000)
  public String getKundeText()
  {
    return kundeText;
  }

  public AuftragDO setKundeText(final String kundeText)
  {
    this.kundeText = kundeText;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "projekt_fk", nullable = true)
  public ProjektDO getProjekt()
  {
    return projekt;
  }

  public AuftragDO setProjekt(final ProjektDO projekt)
  {
    this.projekt = projekt;
    return this;
  }

  @Transient
  public Integer getProjektId()
  {
    if (this.projekt == null) {
      return null;
    }
    return projekt.getId();
  }

  public AuftragDO setTitel(final String titel)
  {
    this.titel = titel;
    return this;
  }

  @Column(name = "titel", length = 1000)
  public String getTitel()
  {
    return titel;
  }

  @Column(length = 4000)
  public String getBemerkung()
  {
    return bemerkung;
  }

  public AuftragDO setBemerkung(final String bemerkung)
  {
    this.bemerkung = bemerkung;
    return this;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "contact_person_fk", nullable = true)
  public PFUserDO getContactPerson()
  {
    return contactPerson;
  }

  public AuftragDO setContactPerson(final PFUserDO contactPerson)
  {
    this.contactPerson = contactPerson;
    return this;
  }

  @Transient
  public Integer getContactPersonId()
  {
    if (this.contactPerson == null) {
      return null;
    }
    return contactPerson.getId();
  }

  /**
   * @return true wenn alle Auftragspositionen vollständig fakturiert sind.
   * @see AuftragsPositionDO#isCompleteInvoiced()
   */
  @Transient
  public boolean isVollstaendigFakturiert()
  {
    if (positionen == null || auftragsStatus != AuftragsStatus.ABGESCHLOSSEN) {
      return false;
    }
    for (final AuftragsPositionDO position : positionen) {
      if (position.isDeleted()) {
        continue;
      }
      if (position.isVollstaendigFakturiert() == false
          && (position.getStatus() == null || position.getStatus().isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT) == false)) {
        return false;
      }
    }
    return true;
  }

  @Transient
  public boolean isAbgeschlossenUndNichtVollstaendigFakturiert()
  {
    if (getAuftragsStatus().isIn(AuftragsStatus.ABGESCHLOSSEN) == true && isVollstaendigFakturiert() == false) {
      return true;
    }
    if (getPositionen() != null) {
      for (final AuftragsPositionDO pos : getPositionen()) {
        if (pos.isDeleted()) {
          continue;
        }
        if (pos.getStatus() == AuftragsPositionsStatus.ABGESCHLOSSEN && pos.isVollstaendigFakturiert() == false) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the position entries for this object.
   */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "auftrag")
  //@OrderColumn(name = "number")
  //TODO: Kann so nicht verwendet werden, da Zähler bei 1 starten muss. Größerer Umbau von nöten, um es zu ändern.
  @IndexColumn(name = "number", base = 1)
  public List<AuftragsPositionDO> getPositionen()
  {
    if (this.positionen == null) {
      log.debug("The list of AuftragsPositionDO is null. AuftragDO id: " + this.getId());
      return Collections.emptyList();
    }
    for (AuftragsPositionDO aPosition : this.positionen) {
      if (aPosition == null) {
        log.debug("AuftragsPositionDO is null in list. AuftragDO id: " + this.getId());
      }
    }
    return this.positionen;
  }

  /**
   * @param number
   * @return AuftragsPositionDO with given position number or null (iterates through the list of positions and compares
   * the number), if not exist.
   */
  public AuftragsPositionDO getPosition(final short number)
  {
    if (positionen == null || positionen.size() < 1) {
      return null;
    }
    for (final AuftragsPositionDO position : this.positionen) {
      if (position.getNumber() == number) {
        return position;
      }
    }
    return null;
  }

  public AuftragDO setPositionen(final List<AuftragsPositionDO> positionen)
  {
    this.positionen = positionen;
    return this;
  }

  public AuftragDO addPosition(final AuftragsPositionDO position)
  {
    ensureAndGetPositionen();
    short number = 1;
    for (final AuftragsPositionDO pos : positionen) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    position.setNumber(number);
    position.setAuftrag(this);
    this.positionen.add(position);
    return this;
  }

  public List<AuftragsPositionDO> ensureAndGetPositionen()
  {
    if (this.positionen == null) {
      setPositionen(new ArrayList<AuftragsPositionDO>());
    }
    return getPositionen();
  }

  /**
   * @return The sum of person days of all positions.
   */
  @Transient
  public BigDecimal getPersonDays()
  {
    BigDecimal result = BigDecimal.ZERO;
    if (this.positionen != null) {
      for (final AuftragsPositionDO pos : this.positionen) {
        if (pos.isDeleted()) {
          continue;
        }
        if (pos.getPersonDays() != null) {
          result = result.add(pos.getPersonDays());
        }
      }
    }
    return result;
  }

  /**
   * Sums all positions. Must be set in all positions before usage. The value is not calculated automatically!
   *
   * @see AuftragDao#calculateInvoicedSum(java.util.Collection)
   */
  @Transient
  public BigDecimal getFakturiertSum()
  {
    if (this.fakturiertSum == null) {
      this.fakturiertSum = BigDecimal.ZERO;
      if (positionen != null) {
        for (final AuftragsPositionDO pos : positionen) {
          if (pos.isDeleted()) {
            continue;
          }
          if (NumberHelper.isNotZero(pos.getFakturiertSum()) == true) {
            this.fakturiertSum = this.fakturiertSum.add(pos.getFakturiertSum());
          }
        }
      }
    }
    return this.fakturiertSum;
  }

  public AuftragDO setFakturiertSum(final BigDecimal fakturiertSum)
  {
    this.fakturiertSum = fakturiertSum;
    return this;
  }

  @Transient
  public BigDecimal getZuFakturierenSum()
  {
    BigDecimal val = BigDecimal.ZERO;
    if (positionen != null) {
      for (final AuftragsPositionDO pos : positionen) {
        if (pos.isDeleted()) {
          continue;
        }
        if (pos.getStatus() != null) {
          if (pos.getStatus().equals(AuftragsPositionsStatus.ABGELEHNT) || pos.getStatus().equals(AuftragsPositionsStatus.ERSETZT) || pos.getStatus()
              .equals(AuftragsPositionsStatus.OPTIONAL)) {
            continue;
          }
        }
        BigDecimal net = pos.getNettoSumme();
        if (net == null) {
          net = BigDecimal.ZERO;
        }
        BigDecimal invoiced = pos.getFakturiertSum();
        if (invoiced == null) {
          invoiced = BigDecimal.ZERO;
        }
        val = val.add(net).subtract(invoiced);
      }
    }
    return val;
  }

  /**
   * The user interface status of an order. The {@link AuftragUIStatus} is stored as XML.
   *
   * @return the XML representation of the uiStatus.
   * @see AuftragUIStatus
   */
  @Column(name = "ui_status_as_xml", length = 10000)
  public String getUiStatusAsXml()
  {
    return uiStatusAsXml;
  }

  /**
   * @param uiStatus the uiStatus to set
   * @return this for chaining.
   */
  public AuftragDO setUiStatusAsXml(final String uiStatus)
  {
    this.uiStatusAsXml = uiStatus;
    return this;
  }

  /**
   * @return the rechungUiStatus
   */
  @Transient
  public AuftragUIStatus getUiStatus()
  {
    if (uiStatus == null) {
      uiStatus = new AuftragUIStatus();
    }
    return uiStatus;
  }

  /**
   * @param rechungUiStatus the rechungUiStatus to set
   * @return this for chaining.
   */
  public AuftragDO setUiStatus(final AuftragUIStatus uiStatus)
  {
    this.uiStatus = uiStatus;
    return this;
  }

  /**
   * Get the payment schedule entries for this object.
   */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "auftrag")
  @IndexColumn(name = "number", base = 1)
  public List<PaymentScheduleDO> getPaymentSchedules()
  {
    return this.paymentSchedules;
  }

  /**
   * @param number
   * @return PaymentScheduleDO with given position number or null (iterates through the list of payment schedules and
   * compares the number), if not exist.
   */
  public PaymentScheduleDO getPaymentSchedule(final short number)
  {
    if (paymentSchedules == null) {
      return null;
    }
    for (final PaymentScheduleDO schedule : this.paymentSchedules) {
      if (schedule.getNumber() == number) {
        return schedule;
      }
    }
    return null;
  }

  public AuftragDO setPaymentSchedules(final List<PaymentScheduleDO> paymentSchedules)
  {
    this.paymentSchedules = paymentSchedules;
    return this;
  }

  public AuftragDO addPaymentSchedule(final PaymentScheduleDO paymentSchedule)
  {
    ensureAndGetPaymentSchedules();
    short number = 1;
    for (final PaymentScheduleDO pos : paymentSchedules) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    paymentSchedule.setNumber(number);
    paymentSchedule.setAuftrag(this);
    this.paymentSchedules.add(paymentSchedule);
    return this;
  }

  public List<PaymentScheduleDO> ensureAndGetPaymentSchedules()
  {
    if (this.paymentSchedules == null) {
      setPaymentSchedules(new ArrayList<PaymentScheduleDO>());
    }
    return getPaymentSchedules();
  }

  @Transient
  public boolean isZahlplanAbgeschlossenUndNichtVollstaendigFakturiert()
  {
    if (getPaymentSchedules() != null) {
      for (final PaymentScheduleDO pos : getPaymentSchedules()) {
        if (pos.isReached() == true && pos.isVollstaendigFakturiert() == false) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return the timeOfPerformanceBegin
   */
  @Column(name = "period_of_performance_begin")
  public Date getPeriodOfPerformanceBegin()
  {
    return periodOfPerformanceBegin;
  }

  /**
   * @param periodOfPerformanceBegin the periodOfPerformanceBegin to set
   * @return this for chaining.
   */
  public AuftragDO setPeriodOfPerformanceBegin(final Date periodOfPerformanceBegin)
  {
    this.periodOfPerformanceBegin = periodOfPerformanceBegin;
    return this;
  }

  /**
   * @return the timeOfPerformanceEnd
   */
  @Column(name = "period_of_performance_end")
  public Date getPeriodOfPerformanceEnd()
  {
    return periodOfPerformanceEnd;
  }

  /**
   * @param periodOfPerformanceEnd the periodOfPerformanceEnd to set
   * @return this for chaining.
   */
  public AuftragDO setPeriodOfPerformanceEnd(final Date periodOfPerformanceEnd)
  {
    this.periodOfPerformanceEnd = periodOfPerformanceEnd;
    return this;
  }

  @Column(name = "probability_of_occurrence")
  public Integer getProbabilityOfOccurrence()
  {
    return probabilityOfOccurrence;
  }

  public void setProbabilityOfOccurrence(final Integer probabilityOfOccurrence)
  {
    this.probabilityOfOccurrence = probabilityOfOccurrence;
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

  public AuftragDO setProjectManager(final PFUserDO projectManager)
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

  public AuftragDO setHeadOfBusinessManager(final PFUserDO headOfBusinessManager)
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

  public AuftragDO setSalesManager(final PFUserDO salesManager)
  {
    this.salesManager = salesManager;
    return this;
  }

  @Transient
  public String getAssignedPersons()
  {
    List<String> result = new ArrayList<>();
    if (projectManager != null) {
      result.add(projectManager.getFullname());
    }
    if (headOfBusinessManager != null) {
      result.add(headOfBusinessManager.getFullname());
    }
    if (salesManager != null) {
      result.add(salesManager.getFullname());
    }
    if (contactPerson != null) {
      result.add(contactPerson.getFullname());
    }
    return String.join("; ", result);
  }
}
