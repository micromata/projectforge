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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.business.fibu.*;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.statistics.IntAggregatedValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.Date;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Service
public class LiquidityForecast implements Serializable
{
  private static final long serialVersionUID = 5385319337895942452L;

  @Autowired
  KontoCache accountCache;

  private final List<LiquidityEntry> entries = new LinkedList<>();

  private Collection<LiquidityEntry> liquiEntries;

  private Collection<RechnungDO> invoices;

  private Collection<LiquidityEntry> invoicesLiquidityEntries;

  /**
   * Used for calculating the expected date of payment for future invoices.
   */
  private final Map<String, IntAggregatedValues> aggregatedDebitorInvoicesValuesMap = new HashMap<>();

  /**
   * Used for calculating the expected date of payment for future invoices.
   */
  private final Map<String, IntAggregatedValues> aggregatedCreditorInvoicesValuesMap = new HashMap<>();

  private Collection<EingangsrechnungDO> creditorInvoices;

  private Collection<LiquidityEntry> creditorInvoicesLiquidityEntries;

  /**
   * Refresh forecast from stored liqui-entries, invoices and creditor invoices and sort the entries.
   * 
   * @return this for chaining.
   * @see #sort()
   */
  public LiquidityForecast build()
  {
    entries.clear();
    entries.addAll(this.liquiEntries);
    entries.addAll(this.invoicesLiquidityEntries);
    entries.addAll(this.creditorInvoicesLiquidityEntries);
    sort();
    return this;
  }

  /**
   * @return this for chaining.
   */
  private LiquidityForecast sort()
  {
    entries.sort(new Comparator<LiquidityEntry>() {
      @Override
      public int compare(final LiquidityEntry o1, final LiquidityEntry o2) {
        if (o1.getDateOfPayment() == null) {
          if (o2.getDateOfPayment() != null) {
            return -1;
          }
        } else if (o2.getDateOfPayment() == null) {
          return 1;
        } else {
          final int compare = o1.getDateOfPayment().compareTo(o2.getDateOfPayment());
          if (compare != 0) {
            return compare;
          }
        }
        final String s1 = o1.getSubject() != null ? o1.getSubject() : "";
        final String s2 = o2.getSubject() != null ? o2.getSubject() : "";
        return s1.compareTo(s2);
      }
    });
    return this;
  }

  /**
   * @return the entries
   */
  public List<LiquidityEntry> getEntries()
  {
    return entries;
  }

  public LiquidityForecast set(final Collection<LiquidityEntryDO> list)
  {
    this.liquiEntries = new LinkedList<>();
    if (list == null) {
      return this;
    }
    for (final LiquidityEntryDO liquiEntry : list) {
      final LiquidityEntry entry = new LiquidityEntry();
      entry.setDateOfPayment(liquiEntry.getDateOfPayment());
      entry.setAmount(liquiEntry.getAmount());
      entry.setPaid(liquiEntry.getPaid());
      entry.setSubject(liquiEntry.getSubject());
      entry.setType(LiquidityEntryType.LIQUIDITY);
      this.liquiEntries.add(entry);
    }
    return this;
  }

  /**
   * For calculating the expected date of payment of future invoices. <br/>
   * Should be called before {@link #setInvoices(Collection)}!
   * 
   * @param list
   */
  public LiquidityForecast calculateExpectedTimeOfPayments(final Collection<RechnungDO> list)
  {
    if (list == null) {
      return this;
    }
    for (final RechnungDO invoice : list) {
      final DayHolder date = new DayHolder(invoice.getDatum());
      final DayHolder dateOfPayment = new DayHolder(invoice.getBezahlDatum());
      if (date == null || dateOfPayment == null) {
        continue;
      }
      final int timeForPayment = date.daysBetween(dateOfPayment);
      final int amount = invoice.getGrossSum().intValue();
      // Store values for different groups:
      final Integer projectId = invoice.getProjektId();
      if (projectId != null) {
        ensureAndAddDebitorPaymentValue("project#" + projectId, timeForPayment, amount);
      }
      final Integer customerId = invoice.getKundeId();
      if (customerId != null) {
        ensureAndAddDebitorPaymentValue("customer#" + customerId, timeForPayment, amount);
      }
      final KontoDO account = accountCache.getKonto(invoice);
      final Integer accountId = account != null ? account.getId() : null;
      if (accountId != null) {
        ensureAndAddDebitorPaymentValue("account#" + accountId, timeForPayment, amount);
      }
      String customerText = invoice.getKundeText();
      if (customerText != null) {
        customerText = customerText.toLowerCase();
        ensureAndAddDebitorPaymentValue("customer:" + customerText, timeForPayment, amount);
        if (customerText.length() > 5) {
          customerText = customerText.substring(0, 5);
        }
        ensureAndAddDebitorPaymentValue("shortCustomer:" + customerText, timeForPayment, amount);
      }
    }
    return this;
  }

  private void setExpectedTimeOfPayment(final LiquidityEntry entry, final RechnungDO invoice)
  {
    Date dateOfInvoice = invoice.getDatum();
    if (dateOfInvoice == null) {
      dateOfInvoice = new DayHolder().getSQLDate();
    }
    final ProjektDO project = invoice.getProjekt();
    if (project != null
        && setExpectedDateOfPayment(entry, dateOfInvoice, "project#" + project.getId(),
        ProjektFormatter.formatProjektKundeAsString(project, null, null))) {
      return;
    }
    final KundeDO customer = invoice.getKunde();
    if (customer != null
        && setExpectedDateOfPayment(entry, dateOfInvoice, "customer#" + customer.getId(),
        KundeFormatter.formatKundeAsString(customer, null))) {
      return;
    }
    final KontoDO account = accountCache.getKonto(invoice);
    if (account != null
        && setExpectedDateOfPayment(entry, dateOfInvoice, "account#" + account.getId(),
        "" + account.getNummer() + " - " + account.getBezeichnung())) {
      return;
    }
    String customerText = invoice.getKundeText();
    if (customerText != null) {
      customerText = customerText.toLowerCase();
      if (setExpectedDateOfPayment(entry, dateOfInvoice, "customer:" + customerText, customerText)) {
        return;
      }
      if (customerText.length() > 5) {
        customerText = customerText.substring(0, 5);
      }
      if (setExpectedDateOfPayment(entry, dateOfInvoice, "shortCustomer:" + customerText, customerText)) {
        return;
      }
    }
  }

  private boolean setExpectedDateOfPayment(final LiquidityEntry entry, final Date dateOfInvoice, final String mapKey,
      final String area)
  {
    final IntAggregatedValues values = aggregatedDebitorInvoicesValuesMap.get(mapKey);
    if (values != null && values.getNumberOfValues() >= 1) {
      entry.setExpectedDateOfPayment(getDate(dateOfInvoice, values.getWeightedAverage()));
      entry.setComment(mapKey
          + ": "
          + area
          + ": "
          + values.getWeightedAverage()
          + " days ("
          + values.getNumberOfValues()
          + " paid invoices)");
      return true;
    } else {
      return false;
    }
  }

  private void ensureAndAddDebitorPaymentValue(final String mapId, final int timeForPayment, final int amount)
  {
    IntAggregatedValues values = aggregatedDebitorInvoicesValuesMap.get(mapId);
    if (values == null) {
      values = new IntAggregatedValues();
      aggregatedDebitorInvoicesValuesMap.put(mapId, values);
    }
    values.add(timeForPayment, amount);
  }

  /**
   * For calculating the expected date of payment of future invoices. <br/>
   * Should be called before {@link #setInvoices(Collection)}!
   * 
   * @param list
   */
  public LiquidityForecast calculateExpectedTimeOfCreditorPayments(final Collection<EingangsrechnungDO> list)
  {
    if (list == null) {
      return this;
    }
    for (final EingangsrechnungDO invoice : list) {
      final DayHolder date = new DayHolder(invoice.getDatum());
      final DayHolder dateOfPayment = new DayHolder(invoice.getBezahlDatum());
      if (date == null || dateOfPayment == null) {
        continue;
      }
      final int timeForPayment = date.daysBetween(dateOfPayment);
      final int amount = invoice.getGrossSum().intValue();
      final KontoDO account = invoice.getKonto();
      final Integer accountId = account != null ? account.getId() : null;
      if (accountId != null) {
        ensureAndAddCreditorPaymentValue("account#" + accountId, timeForPayment, amount);
      }
      String creditorText = invoice.getKreditor();
      if (creditorText != null) {
        creditorText = creditorText.toLowerCase();
        ensureAndAddCreditorPaymentValue("creditor:" + creditorText, timeForPayment, amount);
        if (creditorText.length() > 5) {
          creditorText = creditorText.substring(0, 5);
        }
        ensureAndAddCreditorPaymentValue("shortCreditor:" + creditorText, timeForPayment, amount);
      }
    }
    return this;
  }

  private void setExpectedTimeOfPayment(final LiquidityEntry entry, final EingangsrechnungDO invoice)
  {
    Date dateOfInvoice = invoice.getDatum();
    if (dateOfInvoice == null) {
      dateOfInvoice = new DayHolder().getSQLDate();
    }
    final KontoDO account = invoice.getKonto();
    if (account != null
        && setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "account#" + account.getId(),
        "" + account.getNummer() + " - " + account.getBezeichnung())) {
      return;
    }
    String creditorText = invoice.getKreditor();
    if (creditorText != null) {
      creditorText = creditorText.toLowerCase();
      if (setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "creditor:" + creditorText, creditorText)) {
        return;
      }
      if (creditorText.length() > 5) {
        creditorText = creditorText.substring(0, 5);
      }
      if (setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "shortCreditor:" + creditorText,
          creditorText)) {
        return;
      }
    }
  }

  private boolean setExpectedDateOfCreditorPayment(final LiquidityEntry entry, final Date dateOfInvoice,
      final String mapKey,
      final String area)
  {
    final IntAggregatedValues values = aggregatedCreditorInvoicesValuesMap.get(mapKey);
    if (values != null && values.getNumberOfValues() >= 1) {
      entry.setExpectedDateOfPayment(getDate(dateOfInvoice, values.getWeightedAverage()));
      entry.setComment(mapKey
          + ": "
          + area
          + ": "
          + values.getWeightedAverage()
          + " days ("
          + values.getNumberOfValues()
          + " paid invoices)");
      return true;
    } else {
      return false;
    }
  }

  private void ensureAndAddCreditorPaymentValue(final String mapId, final int timeForPayment, final int amount)
  {
    IntAggregatedValues values = aggregatedCreditorInvoicesValuesMap.get(mapId);
    if (values == null) {
      values = new IntAggregatedValues();
      aggregatedCreditorInvoicesValuesMap.put(mapId, values);
    }
    values.add(timeForPayment, amount);
  }

  private Date getDate(final Date date, final int timeOfPayment)
  {
    final DayHolder day = new DayHolder(date);
    day.add(Calendar.DAY_OF_YEAR, timeOfPayment);
    return day.getSQLDate();
  }

  /**
   * Should be called after {@link #calculateExpectedTimeOfPayments(Collection)}-
   * 
   * @param list
   * @return
   */
  public LiquidityForecast setInvoices(final Collection<RechnungDO> list)
  {
    this.invoices = list;
    this.invoicesLiquidityEntries = new LinkedList<>();
    if (list == null) {
      return this;
    }
    for (final RechnungDO invoice : list) {
      final LiquidityEntry entry = new LiquidityEntry();
      if (invoice.getBezahlDatum() != null) {
        entry.setDateOfPayment(invoice.getBezahlDatum());
      } else {
        entry.setDateOfPayment(invoice.getFaelligkeit());
      }
      entry.setAmount(invoice.getGrossSum());
      entry.setPaid(invoice.isBezahlt());
      entry.setSubject("#" + invoice.getNummer() + ": " + invoice.getKundeAsString() + ": " + invoice.getBetreff());
      entry.setType(LiquidityEntryType.DEBITOR);
      setExpectedTimeOfPayment(entry, invoice);
      this.invoicesLiquidityEntries.add(entry);
    }
    return this;
  }

  public LiquidityForecast setCreditorInvoices(final Collection<EingangsrechnungDO> list)
  {
    this.creditorInvoices = list;
    this.creditorInvoicesLiquidityEntries = new LinkedList<>();
    if (list == null) {
      return this;
    }
    for (final EingangsrechnungDO invoice : list) {
      final LiquidityEntry entry = new LiquidityEntry();
      if (invoice.getBezahlDatum() != null) {
        entry.setDateOfPayment(invoice.getBezahlDatum());
      } else {
        entry.setDateOfPayment(invoice.getFaelligkeit());
      }
      entry.setAmount(invoice.getGrossSum().negate());
      entry.setPaid(invoice.isBezahlt());
      entry.setSubject(invoice.getKreditor() + ": " + invoice.getBetreff());
      entry.setType(LiquidityEntryType.CREDITOR);
      setExpectedTimeOfPayment(entry, invoice);
      this.creditorInvoicesLiquidityEntries.add(entry);
    }
    return this;
  }

  /**
   * @return the invoices
   */
  public Collection<LiquidityEntry> getInvoicesLiquidityEntries()
  {
    return invoicesLiquidityEntries;
  }

  /**
   * @return the invoices
   */
  public Collection<RechnungDO> getInvoices()
  {
    return invoices;
  }

  /**
   * @return the creditorInvoices
   */
  public Collection<LiquidityEntry> getCreditorInvoicesLiquidityEntries()
  {
    return creditorInvoicesLiquidityEntries;
  }

  /**
   * @return the creditorInvoices
   */
  public Collection<EingangsrechnungDO> getCreditorInvoices()
  {
    return creditorInvoices;
  }
}
