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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportCell;
import org.projectforge.business.excel.ExportRow;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskNode;
import org.projectforge.common.DateFormatType;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Forcast excel export.
 *
 * @author Florian Blumenstein
 */
@Service
public class ForecastExport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ForecastExport.class);

  @Autowired
  private AuftragDao orderBookDao;

  @Autowired
  private RechnungCache rechnungCache;

  @Autowired
  private ApplicationContext applicationContext;

  PosCol[] monthCols = { PosCol.MONTH1, PosCol.MONTH2, PosCol.MONTH3, PosCol.MONTH4, PosCol.MONTH5, PosCol.MONTH6,
      PosCol.MONTH7, PosCol.MONTH8, PosCol.MONTH9, PosCol.MONTH10, PosCol.MONTH11, PosCol.MONTH12 };

  List<AuftragsPositionsStatus> auftragsPositionsStatusToShow = Arrays
      .asList(AuftragsPositionsStatus.POTENZIAL, AuftragsPositionsStatus.GELEGT, AuftragsPositionsStatus.BEAUFTRAGT, AuftragsPositionsStatus.LOI);

  public byte[] export(final List<AuftragDO> auftragList, Calendar startDate) throws IOException
  {
    startDate.set(Calendar.DAY_OF_MONTH, 1);

    if (CollectionUtils.isEmpty(auftragList)) {
      return null;
    }

    Resource forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xls");
    ExportWorkbook xls = new ExportWorkbook(forecastTemplate.getInputStream());
    ContentProvider contentProvider = new MyXlsContentProvider(xls);

    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    ExportSheet sheet = xls.getSheet("Forecast_Data");
    List<String> colList = new ArrayList<>();
    for (PosCol col : Arrays.asList(PosCol.values())) {
      colList.add(col.name());
    }
    String[] colArr = new String[colList.size()];
    colArr = colList.toArray(colArr);
    sheet.setPropertyNames(colArr);
    replaceMonthDatesInHeaderRow(startDate, sheet.getRow(0));
    //ExportColumn[] columns = createPosColumns(startDate)
    ContentProvider sheetProvider = sheet.getContentProvider();
    sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, PosCol.NETSUM, PosCol.INVOICED, PosCol.TO_BE_INVOICED);
    sheetProvider
        .putFormat(DateFormats.getExcelFormatString(DateFormatType.DATE), PosCol.DATE_OF_OFFER, PosCol.DATE_OF_ENTRY, PosCol.PERIOD_OF_PERFORMANCE_BEGIN,
            PosCol.PERIOD_OF_PERFORMANCE_END);
    Map<PosCol, BigDecimal> istSumMap = createIstSumMap();
    for (AuftragDO order : auftragList) {
      if (order.getPositionenExcludingDeleted() == null) {
        continue;
      }
      orderBookDao.calculateInvoicedSum(order);
      for (AuftragsPositionDO pos : order.getPositionenExcludingDeleted()) {
        calculateIstSum(istSumMap, startDate, pos);
        if (pos.getStatus() != null && auftragsPositionsStatusToShow.contains(pos.getStatus())) {
          PropertyMapping mapping = new PropertyMapping();
          addPosMapping(mapping, order, pos, startDate);
          sheet.addRow(mapping.getMapping(), 0);
        }
      }
    }
    fillIstSum(sheet, istSumMap);
    sheet.setAutoFilter();

    return xls.getAsByteArray();
  }

  //Has to be in same order like excel template file headers
  private enum PosCol
  {
    NUMBER, POS_NUMBER, DATE_OF_OFFER, DATE_OF_ENTRY, DATE_OF_DESICION, HOB_MANAGER, PROJECT, ORDER_TITLE, TITLE, TYPE, PAYMENTTYPE, STATUS_ORDER, STATUS_POS, PERSON_DAYS, NETSUM, INVOICED, TO_BE_INVOICED, COMPLETELY_INVOICED, INVOICES,
    PERIOD_OF_PERFORMANCE_BEGIN, PERIOD_OF_PERFORMANCE_END, PROBABILITY_OF_OCCURRENCE, CONTACT_PERSON, TASK, COMMENT, PROBABILITY_OF_OCCURRENCE_VALUE, MONTHEND_STARTDATE_ADD1, MONTHEND_ENDDATE_ADD1, MONTHCOUNT, EMPTY,
    MONTH1, MONTH2, MONTH3, MONTH4, MONTH5, MONTH6, MONTH7, MONTH8, MONTH9, MONTH10, MONTH11, MONTH12
  }

  private Map<PosCol, BigDecimal> createIstSumMap()
  {
    Map<PosCol, BigDecimal> istSumMap = new TreeMap<>();
    for (PosCol posCol : monthCols) {
      istSumMap.put(posCol, BigDecimal.ZERO);
    }
    return istSumMap;
  }

  private void fillIstSum(ExportSheet sheet, Map<PosCol, BigDecimal> istSumMap)
  {
    ExportRow istRow = sheet.getRow(6);
    int i = 30;
    for (PosCol monthCol : istSumMap.keySet()) {
      istRow.getCell(i).setValue(istSumMap.get(monthCol));
      i++;
    }
  }

  private void calculateIstSum(Map<PosCol, BigDecimal> istSumMap, Calendar startDate, AuftragsPositionDO pos)
  {
    final Set<RechnungsPositionVO> invoicePositions = rechnungCache
        .getRechnungsPositionVOSetByAuftragsPositionId(pos.getId());
    if (invoicePositions != null) {
      Calendar beginActualMonth = Calendar.getInstance();
      beginActualMonth.set(Calendar.DAY_OF_MONTH, 1);
      int startMonth = startDate.get(Calendar.MONTH);
      for (RechnungsPositionVO rpo : invoicePositions) {
        Calendar rDate = Calendar.getInstance();
        rDate.setTime(rpo.getDate());
        if (rDate.after(startDate) && rDate.before(beginActualMonth)) {
          int monthCol = 0;
          if (startDate.get(Calendar.YEAR) == rDate.get(Calendar.YEAR)) {
            monthCol = rDate.get(Calendar.MONTH) - startMonth;
          } else {
            monthCol = 12 - startMonth + rDate.get(Calendar.MONTH);
          }
          istSumMap.replace(monthCols[monthCol], istSumMap.get(monthCols[monthCol]).add(rpo.getNettoSumme()));
        }
      }
    }
  }

  private void replaceMonthDatesInHeaderRow(Calendar startDate, ExportRow row)
  {
    // Adding month columns
    Calendar startDateColumn = Calendar.getInstance();
    startDateColumn.setTime(startDate.getTime());
    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM yyyy");
    int i = 30;

    for (PosCol month : monthCols) {
      ExportCell cell = row.getCell(i);
      cell.setValue(sdf.format(startDateColumn.getTime()));
      startDateColumn.add(Calendar.MONTH, 1);
      i++;
    }
  }

  private void addPosMapping(final PropertyMapping mapping, final AuftragDO order, final AuftragsPositionDO pos, final Calendar startDate)
  {
    mapping.add(PosCol.NUMBER, order.getNummer());
    mapping.add(PosCol.POS_NUMBER, "#" + pos.getNumber());
    mapping.add(PosCol.DATE_OF_OFFER, order.getAngebotsDatum());
    mapping.add(PosCol.DATE_OF_ENTRY, order.getErfassungsDatum());
    mapping.add(PosCol.DATE_OF_DESICION, ensureErfassungsDatum(order));
    mapping.add(PosCol.HOB_MANAGER, order.getHeadOfBusinessManager() != null ? order.getHeadOfBusinessManager().getFullname() : "");
    mapping.add(PosCol.PROJECT, order.getProjektAsString());
    mapping.add(PosCol.ORDER_TITLE, order.getTitel());
    mapping.add(PosCol.TITLE, pos.getTitel());
    mapping.add(PosCol.TYPE, pos.getArt() != null ? ThreadLocalUserContext.getLocalizedString(pos.getArt().getI18nKey()) : "");
    mapping.add(PosCol.PAYMENTTYPE, pos.getPaymentType() != null ? ThreadLocalUserContext.getLocalizedString(pos.getPaymentType().getI18nKey()) : "");
    mapping
        .add(PosCol.STATUS_ORDER, order.getAuftragsStatus() != null ? ThreadLocalUserContext.getLocalizedString(order.getAuftragsStatus().getI18nKey()) : "");
    mapping.add(PosCol.STATUS_POS, pos.getStatus() != null ? ThreadLocalUserContext.getLocalizedString(pos.getStatus().getI18nKey()) : "");
    mapping.add(PosCol.PERSON_DAYS, pos.getPersonDays());
    final BigDecimal netSum = pos.getNettoSumme() != null ? pos.getNettoSumme() : BigDecimal.ZERO;
    final BigDecimal invoicedSum = pos.getFakturiertSum() != null ? pos.getFakturiertSum() : BigDecimal.ZERO;
    final BigDecimal toBeInvoicedSum = netSum.subtract(invoicedSum);
    mapping.add(PosCol.NETSUM, netSum);
    addCurrency(mapping, PosCol.INVOICED, invoicedSum);
    addCurrency(mapping, PosCol.TO_BE_INVOICED, toBeInvoicedSum);
    mapping.add(PosCol.COMPLETELY_INVOICED, pos.isVollstaendigFakturiert() ? "x" : "");
    final Set<RechnungsPositionVO> invoicePositions = rechnungCache
        .getRechnungsPositionVOSetByAuftragsPositionId(pos.getId());
    mapping.add(PosCol.INVOICES, getInvoices(invoicePositions));
    if (PeriodOfPerformanceType.OWN.equals(pos.getPeriodOfPerformanceType())) {
      // use "own" period -> from pos
      mapping.add(PosCol.PERIOD_OF_PERFORMANCE_BEGIN, pos.getPeriodOfPerformanceBegin());
      mapping.add(PosCol.PERIOD_OF_PERFORMANCE_END, pos.getPeriodOfPerformanceEnd());
    } else {
      // use "see above" period -> from order
      mapping.add(PosCol.PERIOD_OF_PERFORMANCE_BEGIN, order.getPeriodOfPerformanceBegin());
      mapping.add(PosCol.PERIOD_OF_PERFORMANCE_END, order.getPeriodOfPerformanceEnd());
    }
    final BigDecimal probability = getProbabilityOfAccurence(order, pos);
    mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE, probability.multiply(new BigDecimal(100)));
    //    mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE, order.getProbabilityOfOccurrence())
    mapping.add(PosCol.CONTACT_PERSON, order.getContactPerson() != null ? order.getContactPerson().getFullname() : "");
    final TaskNode node = TenantRegistryMap.getInstance().getTenantRegistry().getTaskTree().getTaskNodeById(pos.getTaskId());
    mapping.add(PosCol.TASK, node != null && node.getTask() != null ? node.getTask().getTitle() : "");
    mapping.add(PosCol.COMMENT, pos.getBemerkung());
    mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE_VALUE, computeAccurenceValue(order, pos));
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    mapping.add(PosCol.MONTHEND_STARTDATE_ADD1, sdf.format(getStartLeistungszeitraumNextMonthEnd(order, pos).getTime()));
    mapping.add(PosCol.MONTHEND_ENDDATE_ADD1, sdf.format(getEndLeistungszeitraumNextMonthEnd(order, pos).getTime()));
    mapping.add(PosCol.MONTHCOUNT, getMonthCountForOrderPosition(order, pos));

    switch (pos.getPaymentType()) {
      case TIME_AND_MATERIALS:
        fillMonthColumnsDistributed(mapping, order, pos, startDate);
        break;
      case PAUSCHALE:
        if (order.getProbabilityOfOccurrence() != null) {
          fillMonthColumnsDistributed(mapping, order, pos, startDate);
        }
        break;
      case FESTPREISPAKET:
        fillByPaymentSchedule(mapping, order, pos, startDate);
        break;
    }

  }

  private void fillByPaymentSchedule(final PropertyMapping mapping, final AuftragDO order, final AuftragsPositionDO pos, final Calendar startDate)
  {
    // payment values
    BigDecimal sumPaymentSchedule = new BigDecimal(0.0);
    BigDecimal probability = getProbabilityOfAccurence(order, pos);
    BigDecimal posNettoSum = pos.getNettoSumme();
    PosCol periodOfPerformanceEnd = null;

    // stop processing if no posNettoSum value exists
    if (posNettoSum == null) {
      return;
    }

    posNettoSum = posNettoSum.multiply(probability);

    // get payment schedule for order position
    List<PaymentScheduleDO> paymentSchedules = getPaymentSchedule(order, pos);

    // compute total sum
    for (PaymentScheduleDO schedule : paymentSchedules) {
      sumPaymentSchedule = sumPaymentSchedule.add(schedule.getAmount().multiply(probability));
    }

    final Calendar currentMonth = Calendar.getInstance();
    currentMonth.setTime(startDate.getTime());
    //currentMonth.add(Calendar.MONTH, -1);
    final Calendar posEndDate = getEndLeistungszeitraumNextMonthEnd(order, pos);

    for (int i = 0; i < monthCols.length; i++) {
      //currentMonth.add(Calendar.MONTH, 1);
      BigDecimal sum = new BigDecimal(0.0);

      for (PaymentScheduleDO schedule : paymentSchedules) {
        if (schedule.isVollstaendigFakturiert()) {
          continue;
        }

        final Calendar cal = Calendar.getInstance();
        cal.setTime(schedule.getScheduleDate());
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

        if (cal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
            && cal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)) {
          sum = sum.add(schedule.getAmount().multiply(probability));
        }
      }

      if (sum.compareTo(BigDecimal.ZERO) > 0 && checkAfterMonthBefore(currentMonth)) {
        mapping.add(monthCols[i], sum);
      }

      // detect period of performance end
      if (posEndDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
          posEndDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)) {
        periodOfPerformanceEnd = monthCols[i];
      }
    }

    // check for payment difference
    posNettoSum = posNettoSum.subtract(sumPaymentSchedule);
    if (posNettoSum.compareTo(BigDecimal.ZERO) == 0 || periodOfPerformanceEnd == null) {
      return;
    }

    // handle payment difference
    final Object previousValue = mapping.getMapping().get(periodOfPerformanceEnd.name());

    if (previousValue == null && checkAfterMonthBefore(currentMonth)) {
      mapping.add(periodOfPerformanceEnd, posNettoSum);
    } else {
      posNettoSum = posNettoSum.add((BigDecimal) previousValue);
      if (checkAfterMonthBefore(currentMonth)) {
        mapping.add(periodOfPerformanceEnd, posNettoSum);
      }
    }
  }

  /**
   * Checks, if given date is behind the month before now.
   *
   * @param toCheck
   * @return
   */
  private boolean checkAfterMonthBefore(Calendar toCheck)
  {
    Calendar oneMonthBeforeNow = Calendar.getInstance();
    oneMonthBeforeNow.add(Calendar.MONTH, -1);
    if (toCheck.get(Calendar.YEAR) == oneMonthBeforeNow.get(Calendar.YEAR)) {
      return toCheck.get(Calendar.MONTH) > oneMonthBeforeNow.get(Calendar.MONTH);
    }
    if (toCheck.get(Calendar.YEAR) < oneMonthBeforeNow.get(Calendar.YEAR)) {
      return false;
    }
    return true;
  }

  private List<PaymentScheduleDO> getPaymentSchedule(final AuftragDO order, final AuftragsPositionDO pos)
  {
    List<PaymentScheduleDO> schedules = order.getPaymentSchedules();

    if (schedules == null)
      return Collections.emptyList();

    List<PaymentScheduleDO> schedulesFiltered = schedules.stream()
        .filter(schedule -> schedule.getPositionNumber() == pos.getNumber() && schedule.getScheduleDate() != null && schedule.getAmount() != null)
        .collect(Collectors.toList());

    return schedulesFiltered;
  }

  private void fillMonthColumnsDistributed(final PropertyMapping mapping, final AuftragDO order, final AuftragsPositionDO pos, final Calendar startDate)
  {
    Calendar posStartDate = getStartLeistungszeitraumNextMonthEnd(order, pos);
    Calendar posEndDate = getEndLeistungszeitraumNextMonthEnd(order, pos);
    Calendar oneMonthBeforeNow = Calendar.getInstance();
    oneMonthBeforeNow.add(Calendar.MONTH, -1);
    Calendar startDateWhile = Calendar.getInstance();
    startDateWhile.setTime(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.setTime(startDate.getTime());
    endDate.add(Calendar.YEAR, 1);
    BigDecimal posSum = computeAccurenceValue(order, pos);
    if (posSum != null) {
      BigDecimal monthCount = getMonthCount(oneMonthBeforeNow, posEndDate);
      if (monthCount != null && monthCount.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal partlyNettoSum = posSum.divide(monthCount, RoundingMode.HALF_UP);
        int i = 0;
        while (endDate.after(posStartDate) && (posEndDate.equals(posStartDate) || posEndDate.after(posStartDate)) && i < monthCols.length) {
          if (posStartDate.get(Calendar.MONTH) == startDateWhile.get(Calendar.MONTH) && posStartDate.get(Calendar.YEAR) == startDateWhile.get(Calendar.YEAR)) {
            if (checkAfterMonthBefore(startDateWhile)) {
              mapping.add(monthCols[i], partlyNettoSum);
            }
            posStartDate.add(Calendar.MONTH, 1);
          }
          startDateWhile.add(Calendar.MONTH, 1);
          i++;
        }
      }
    }
  }

  private BigDecimal computeAccurenceValue(final AuftragDO order, final AuftragsPositionDO pos)
  {
    BigDecimal netSum = pos.getNettoSumme() != null ? pos.getNettoSumme() : BigDecimal.ZERO;
    BigDecimal invoicedSum = pos.getFakturiertSum() != null ? pos.getFakturiertSum() : BigDecimal.ZERO;
    BigDecimal toBeInvoicedSum = netSum.subtract(invoicedSum);

    final BigDecimal probability = getProbabilityOfAccurence(order, pos);
    return toBeInvoicedSum.multiply(probability);
  }

  private BigDecimal getProbabilityOfAccurence(final AuftragDO order, final AuftragsPositionDO pos)
  {
    if (pos.getStatus() == AuftragsPositionsStatus.BEAUFTRAGT) {
      return BigDecimal.ONE;
    }

    if (order.getProbabilityOfOccurrence() != null) {
      return BigDecimal.valueOf(order.getProbabilityOfOccurrence() / 100);
    }

    switch (pos.getStatus()) {
      case GELEGT:
        return BigDecimal.valueOf(0.5);
      case LOI:
        return BigDecimal.valueOf(0.9);
      case BEAUFTRAGT:
      case ABGESCHLOSSEN:
        return BigDecimal.ONE;
      default:
        return BigDecimal.ZERO;
    }
  }

  private Calendar getStartLeistungszeitraumNextMonthEnd(final AuftragDO order, final AuftragsPositionDO pos)
  {
    Calendar cal = Calendar.getInstance();
    if (pos.getPeriodOfPerformanceType().equals(PeriodOfPerformanceType.OWN)) {
      if (pos.getPeriodOfPerformanceBegin() != null) {
        cal.setTime(pos.getPeriodOfPerformanceBegin());
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
      }
    } else {
      if (order.getPeriodOfPerformanceBegin() != null) {
        cal.setTime(order.getPeriodOfPerformanceBegin());
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
      }
    }
    return cal;
  }

  private Calendar getEndLeistungszeitraumNextMonthEnd(final AuftragDO order, final AuftragsPositionDO pos)
  {
    Calendar cal = Calendar.getInstance();
    if (pos.getPeriodOfPerformanceType().equals(PeriodOfPerformanceType.OWN)) {
      if (pos.getPeriodOfPerformanceEnd() != null) {
        cal.setTime(pos.getPeriodOfPerformanceEnd());
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
      }
    } else {
      if (order.getPeriodOfPerformanceEnd() != null) {
        cal.setTime(order.getPeriodOfPerformanceEnd());
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
      }
    }
    return cal;
  }

  private BigDecimal getMonthCountForOrderPosition(final AuftragDO order, final AuftragsPositionDO pos)
  {
    BigDecimal result = null;
    Calendar startCalendar = Calendar.getInstance();
    Calendar endCalendar = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    if (pos.getPeriodOfPerformanceType().equals(PeriodOfPerformanceType.OWN)) {
      if (pos.getPeriodOfPerformanceEnd() != null && pos.getPeriodOfPerformanceBegin() != null) {
        startCalendar.setTime(pos.getPeriodOfPerformanceBegin());
        endCalendar.setTime(pos.getPeriodOfPerformanceEnd());
        result = getMonthCount(startCalendar, endCalendar);
      }
    } else {
      if (order.getPeriodOfPerformanceEnd() != null && order.getPeriodOfPerformanceBegin() != null) {
        startCalendar.setTime(order.getPeriodOfPerformanceBegin());
        endCalendar.setTime(order.getPeriodOfPerformanceEnd());
        result = getMonthCount(startCalendar, endCalendar);
      }
    }
    return result;
  }

  private BigDecimal getMonthCount(Calendar startCalendar, Calendar endCalendar)
  {
    int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
    int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH) + 1;
    return BigDecimal.valueOf(diffMonth);
  }

  private void addCurrency(final PropertyMapping mapping, final Enum<?> col, final BigDecimal value)
  {
    if (NumberHelper.isNotZero(value)) {
      mapping.add(col, value);
    } else {
      mapping.add(col, "");
    }
  }

  private String getInvoices(final Set<RechnungsPositionVO> invoicePositions)
  {
    final StringBuilder sb = new StringBuilder();
    if (invoicePositions != null) {
      String delimiter = "";
      for (final RechnungsPositionVO invoicePos : invoicePositions) {
        sb.append(delimiter).append(invoicePos.getRechnungNummer());
        delimiter = ", ";
      }
    }
    return sb.toString();
  }

  private java.sql.Date ensureErfassungsDatum(AuftragDO order)
  {
    if (order.getErfassungsDatum() == null) {
      if (order.getCreated() == null) {
        if (order.getAngebotsDatum() == null) {
          order.setErfassungsDatum(new java.sql.Date(new Date().getTime()));
        } else {
          order.setErfassungsDatum(new java.sql.Date(order.getAngebotsDatum().getTime()));
        }
      } else {
        order.setErfassungsDatum(new java.sql.Date(order.getCreated().getTime()));
      }
    }
    return order.getErfassungsDatum();
  }
}