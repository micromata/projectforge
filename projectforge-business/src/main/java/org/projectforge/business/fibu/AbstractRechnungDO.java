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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropertyType;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.xstream.XmlObjectReader;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

@MappedSuperclass
@JpaXmlPersist(beforePersistListener = AbstractRechnungXmlBeforePersistListener.class,
    persistAfter = Kost2ArtDO.class)
public abstract class AbstractRechnungDO<T extends AbstractRechnungsPositionDO> extends DefaultBaseDO
{
  private static final long serialVersionUID = -8936320220788212987L;

  @PropertyInfo(i18nKey = "fibu.rechnung.datum")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  protected Date datum;

  @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  protected String betreff;

  @PropertyInfo(i18nKey = "comment")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  protected String bemerkung;

  @PropertyInfo(i18nKey = "fibu.rechnung.besonderheiten")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  protected String besonderheiten;

  @PropertyInfo(i18nKey = "fibu.rechnung.faelligkeit")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  protected Date faelligkeit;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  protected transient Integer zahlungsZielInTagen;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  protected transient Integer discountZahlungsZielInTagen;

  @PropertyInfo(i18nKey = "fibu.rechnung.bezahlDatum")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  protected Date bezahlDatum;

  @PropertyInfo(i18nKey = "fibu.rechnung.zahlBetrag", type = PropertyType.CURRENCY)
  protected BigDecimal zahlBetrag;

  @PropertyInfo(i18nKey = "fibu.konto")
  @IndexedEmbedded(depth = 1)
  private KontoDO konto;

  @PropertyInfo(i18nKey = "fibu.rechnung.discountPercent")
  private BigDecimal discountPercent;

  @PropertyInfo(i18nKey = "fibu.rechnung.discountMaturity")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date discountMaturity;

  @PropertyInfo(i18nKey = "fibu.rechnung.receiver")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String receiver;

  @PropertyInfo(i18nKey = "fibu.rechnung.iban")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String iban;

  @PropertyInfo(i18nKey = "fibu.rechnung.bic")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String bic;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  protected List<T> positionen = null;

  @NoHistory
  protected String uiStatusAsXml;

  @NoHistory
  protected RechnungUIStatus uiStatus;

  @Override
  public void recalculate()
  {
    // recalculate the transient fields
    if (this.datum == null) {
      this.zahlungsZielInTagen = null;
      this.discountZahlungsZielInTagen = null;
      return;
    }

    final DateHolder date = new DateHolder(this.datum);
    this.zahlungsZielInTagen = (this.faelligkeit == null) ? null : date.daysBetween(this.faelligkeit);
    this.discountZahlungsZielInTagen = (this.discountMaturity == null) ? null : date.daysBetween(this.discountMaturity);
  }

  @Column(length = 4000)
  public String getBetreff()
  {
    return betreff;
  }

  public AbstractRechnungDO<T> setBetreff(final String betreff)
  {
    this.betreff = betreff;
    return this;
  }

  @Column(length = 4000)
  public String getBesonderheiten()
  {
    return besonderheiten;
  }

  public AbstractRechnungDO<T> setBesonderheiten(final String besonderheiten)
  {
    this.besonderheiten = besonderheiten;
    return this;
  }

  @Column(length = 4000)
  public String getBemerkung()
  {
    return bemerkung;
  }

  public AbstractRechnungDO<T> setBemerkung(final String bemerkung)
  {
    this.bemerkung = bemerkung;
    return this;
  }

  @Column(nullable = false)
  public Date getDatum()
  {
    return datum;
  }

  public AbstractRechnungDO<T> setDatum(final Date datum)
  {
    this.datum = datum;
    return this;
  }

  @Column
  public Date getFaelligkeit()
  {
    return faelligkeit;
  }

  public AbstractRechnungDO<T> setFaelligkeit(final Date faelligkeit)
  {
    this.faelligkeit = faelligkeit;
    return this;
  }

  @Column
  public String getReceiver()
  {
    return receiver;
  }

  public void setReceiver(String receiver)
  {
    this.receiver = receiver;
  }

  @Column(length = 50)
  public String getIban()
  {
    return iban;
  }

  public void setIban(String iban)
  {
    this.iban = iban;
  }

  @Column(length = 11)
  public String getBic()
  {
    return bic;
  }

  public void setBic(String bic)
  {
    this.bic = bic;
  }

  @Column
  public BigDecimal getDiscountPercent()
  {
    return discountPercent;
  }

  public void setDiscountPercent(BigDecimal discountPercent)
  {
    this.discountPercent = discountPercent;
  }

  @Column
  public Date getDiscountMaturity()
  {
    return discountMaturity;
  }

  public void setDiscountMaturity(Date discountMaturity)
  {
    this.discountMaturity = discountMaturity;
  }

  /**
   * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
   * werden.
   *
   * @see #recalculate()
   */
  @Transient
  public Integer getZahlungsZielInTagen()
  {
    return zahlungsZielInTagen;
  }

  public AbstractRechnungDO<T> setZahlungsZielInTagen(final Integer zahlungsZielInTagen)
  {
    this.zahlungsZielInTagen = zahlungsZielInTagen;
    return this;
  }

  /**
   * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
   * werden.
   *
   * @see #recalculate()
   */
  @Transient
  public Integer getDiscountZahlungsZielInTagen()
  {
    return discountZahlungsZielInTagen;
  }

  public AbstractRechnungDO<T> setDiscountZahlungsZielInTagen(final Integer discountZahlungsZielInTagen)
  {
    this.discountZahlungsZielInTagen = discountZahlungsZielInTagen;
    return this;
  }

  @Transient
  public BigDecimal getGrossSum()
  {
    BigDecimal brutto = BigDecimal.ZERO;
    if (this.positionen != null) {
      for (final T position : this.positionen) {
        brutto = brutto.add(position.getBruttoSum());
      }
    }
    return brutto;
  }

  @Transient
  public BigDecimal getNetSum()
  {
    BigDecimal netto = BigDecimal.ZERO;
    if (this.positionen != null) {
      for (final T position : this.positionen) {
        netto = netto.add(position.getNetSum());
      }
    }
    return netto;
  }

  @Transient
  public BigDecimal getVatAmountSum()
  {
    BigDecimal vatAmount = BigDecimal.ZERO;
    if (this.positionen != null) {
      for (final T position : this.positionen) {
        vatAmount = vatAmount.add(position.getVatAmount());
      }
    }
    return vatAmount;
  }

  @Column(name = "bezahl_datum")
  public Date getBezahlDatum()
  {
    return bezahlDatum;
  }

  public AbstractRechnungDO<T> setBezahlDatum(final Date bezahlDatum)
  {
    this.bezahlDatum = bezahlDatum;
    return this;
  }

  /**
   * Bruttobetrag, den der Kunde bezahlt hat.
   *
   * @return
   */
  @Column(name = "zahl_betrag", scale = 2, precision = 12)
  public BigDecimal getZahlBetrag()
  {
    return zahlBetrag;
  }

  public AbstractRechnungDO<T> setZahlBetrag(final BigDecimal zahlBetrag)
  {
    this.zahlBetrag = zahlBetrag;
    return this;
  }

  @Transient
  public abstract boolean isBezahlt();

  @Transient
  public boolean isUeberfaellig()
  {
    if (isBezahlt() == true) {
      return false;
    }
    final DayHolder today = new DayHolder();
    return (this.faelligkeit == null || this.faelligkeit.before(today.getDate()) == true);
  }

  /**
   * This Datev account number is used for the exports of invoices. For debitor invoices (RechnungDO): If not given then
   * the account number assigned to the ProjektDO if set or KundeDO is used instead (default).
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

  @Transient
  public abstract List<T> getPositionen();

  public AbstractRechnungDO<T> setPositionen(final List<T> positionen)
  {
    this.positionen = positionen;
    return this;
  }

  /**
   * @param idx
   * @return PositionDO with given index or null, if not exist.
   */
  public T getPosition(final int idx)
  {
    if (positionen == null) {
      return null;
    }
    if (idx >= positionen.size()) { // Index out of bounds.
      return null;
    }
    return positionen.get(idx);
  }

  public AbstractRechnungDO<T> addPosition(final T position)
  {
    ensureAndGetPositionen();
    short number = 1;
    for (final T pos : positionen) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    position.setNumber(number);
    position.setRechnung(this);
    this.positionen.add(position);
    return this;
  }

  public List<T> ensureAndGetPositionen()
  {
    {
      if (this.positionen == null) {
        setPositionen(new ArrayList<T>());
      }
      return getPositionen();
    }
  }

  /**
   * @return The total sum of all cost assignment net amounts of all positions.
   */
  @Transient
  public BigDecimal getKostZuweisungenNetSum()
  {
    if (this.positionen == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal netSum = BigDecimal.ZERO;
    for (final T pos : this.positionen) {
      if (CollectionUtils.isNotEmpty(pos.kostZuweisungen) == true) {
        for (final KostZuweisungDO zuweisung : pos.kostZuweisungen) {
          if (zuweisung.getNetto() != null) {
            netSum = netSum.add(zuweisung.getNetto());
          }
        }
      }
    }
    return netSum;
  }

  @Transient
  public BigDecimal getKostZuweisungFehlbetrag()
  {
    return getKostZuweisungenNetSum().subtract(getNetSum());
  }

  public boolean hasKostZuweisungen()
  {
    if (this.positionen == null) {
      return false;
    }
    for (final T pos : this.positionen) {
      if (CollectionUtils.isNotEmpty(pos.kostZuweisungen) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * The user interface status of an invoice. The {@link RechnungUIStatus} is stored as XML.
   *
   * @return the XML representation of the uiStatus.
   * @see RechnungUIStatus
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
  public AbstractRechnungDO<T> setUiStatusAsXml(final String uiStatus)
  {
    this.uiStatusAsXml = uiStatus;
    return this;
  }

  /**
   * @return the rechungUiStatus
   */
  @Transient
  public RechnungUIStatus getUiStatus()
  {
    if (uiStatus == null && StringUtils.isEmpty(uiStatusAsXml)) {
      uiStatus = new RechnungUIStatus();
    } else if (uiStatus == null) {
      final XmlObjectReader reader = new XmlObjectReader();
      reader.initialize(RechnungUIStatus.class);
      uiStatus = (RechnungUIStatus) reader.read(uiStatusAsXml);
    }

    return uiStatus;
  }

  /**
   * @param rechungUiStatus the rechungUiStatus to set
   * @return this for chaining.
   */
  public AbstractRechnungDO<T> setUiStatus(final RechnungUIStatus uiStatus)
  {
    this.uiStatus = uiStatus;
    return this;
  }
}
