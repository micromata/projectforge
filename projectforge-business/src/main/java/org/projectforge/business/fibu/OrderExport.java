/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections4.CollectionUtils;
import org.projectforge.business.excel.*;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.DateFormatType;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class OrderExport {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderExport.class);

    @Autowired
    protected AccessChecker accessChecker;

    @Autowired
    private RechnungCache rechnungCache;

    @Autowired
    private AuftragsCache auftragsCache;

    @Autowired
    private TaskTree taskTree;

    private ExportColumn[] createOrderColumns() {
        return new ExportColumn[]{
                new I18nExportColumn(OrderCol.NUMMER, "fibu.auftrag.nummer.short", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(OrderCol.NUMBER_OF_POSITIONS, "fibu.auftrag.positions", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(OrderCol.DATE_OF_OFFER, "fibu.auftrag.angebot.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.DATE_OF_ENTRY, "fibu.auftrag.erfassung.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.DATE_OF_DESICION, "fibu.auftrag.entscheidung.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.ORDER_DATE, "fibu.auftrag.beauftragungsdatum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.STATUS, "status", 10),
                new I18nExportColumn(OrderCol.STATUS_COMMENT, "fibu.auftrag.statusBeschreibung", 10),
                new I18nExportColumn(OrderCol.PROJECT, "fibu.projekt", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.PROJECT_CUSTOMER, "fibu.kunde", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.TITLE, "fibu.auftrag.title", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.PROJECTMANAGER, "fibu.projectManager", 30),
                new I18nExportColumn(OrderCol.HEADOFBUSINESSMANAGER, "fibu.headOfBusinessManager", 30),
                new I18nExportColumn(OrderCol.SALESMANAGER, "fibu.salesManager", 30),
                new I18nExportColumn(OrderCol.NETSUM, "fibu.auftrag.nettoSumme", MyXlsContentProvider.LENGTH_CURRENCY),
                new I18nExportColumn(OrderCol.INVOICED, "fibu.fakturiert", MyXlsContentProvider.LENGTH_CURRENCY),
                new I18nExportColumn(OrderCol.TO_BE_INVOICED, "fibu.toBeInvoiced", MyXlsContentProvider.LENGTH_CURRENCY),
                new I18nExportColumn(OrderCol.COMPLETELY_INVOICED, "fibu.auftrag.vollstaendigFakturiert", MyXlsContentProvider.LENGTH_BOOLEAN),
                new I18nExportColumn(OrderCol.INVOICES, "fibu.rechnungen", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.PERIOD_OF_PERFORMANCE_BEGIN, "fibu.periodOfPerformance.from", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.PERIOD_OF_PERFORMANCE_END, "fibu.periodOfPerformance.to", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.PROBABILITY_OF_OCCURRENCE, "fibu.probabilityOfOccurrence", MyXlsContentProvider.LENGTH_PERCENT),
                new I18nExportColumn(OrderCol.FORECAST_TYPE, AuftragForecastType.getBaseKey(), MyXlsContentProvider.LENGTH_PERCENT),
                new I18nExportColumn(OrderCol.CONTACT_PERSON, "contactPerson", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.REFERENCE, "fibu.common.reference", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(OrderCol.COMMENT, "comment", MyXlsContentProvider.LENGTH_COMMENT)
        };
    }

    private void addOrderMapping(final PropertyMapping mapping, final AuftragDO order) {
        var orderInfo = auftragsCache.getOrderInfo(order);
        mapping.add(OrderCol.NUMMER, order.getNummer());
        mapping.add(OrderCol.NUMBER_OF_POSITIONS,
                "#" + (order.getPositionenExcludingDeleted() != null ? order.getPositionenExcludingDeleted().size() : "0"));
        mapping.add(OrderCol.DATE_OF_ENTRY, order.getErfassungsDatum());
        mapping.add(OrderCol.DATE_OF_OFFER, order.getAngebotsDatum());
        mapping.add(OrderCol.DATE_OF_DESICION, order.getEntscheidungsDatum());
        mapping.add(OrderCol.ORDER_DATE, order.getBeauftragungsDatum());
        mapping.add(OrderCol.STATUS,
                order.getStatus() != null
                        ? ThreadLocalUserContext.getLocalizedString(order.getStatus().getI18nKey()) : "");
        mapping.add(OrderCol.STATUS_COMMENT, order.getStatusBeschreibung());
        mapping.add(OrderCol.PROJECT, order.getProjektAsString());
        final ProjektDO project = order.getProjekt();
        final String projectCustomer = KundeFormatter.formatKundeAsString(project != null ? project.getKunde() : null,
                order.getKundeText());
        mapping.add(OrderCol.PROJECT_CUSTOMER, projectCustomer);
        mapping.add(OrderCol.TITLE, order.getTitel());
        mapping.add(OrderCol.PROJECTMANAGER, order.getProjectManager() != null ? order.getProjectManager().getFullname() : "");
        mapping.add(OrderCol.HEADOFBUSINESSMANAGER, order.getHeadOfBusinessManager() != null ? order.getHeadOfBusinessManager().getFullname() : "");
        mapping.add(OrderCol.SALESMANAGER, order.getSalesManager() != null ? order.getSalesManager().getFullname() : "");
        final BigDecimal netSum = orderInfo.getNetSum();
        final BigDecimal invoicedSum = orderInfo.getInvoicedSum();
        final BigDecimal toBeInvoicedSum = orderInfo.getNotYetInvoicedSum();
        mapping.add(OrderCol.NETSUM, netSum);
        addCurrency(mapping, OrderCol.INVOICED, invoicedSum);
        addCurrency(mapping, OrderCol.TO_BE_INVOICED, toBeInvoicedSum);
        mapping.add(OrderCol.COMPLETELY_INVOICED, orderInfo.isVollstaendigFakturiert() ? "x" : "");
        final List<RechnungPosInfo> invoicePositions = rechnungCache
                .getRechnungsPosInfosByAuftragId(order.getId());
        mapping.add(OrderCol.INVOICES, getInvoices(invoicePositions));
        mapping.add(OrderCol.PERIOD_OF_PERFORMANCE_BEGIN, order.getPeriodOfPerformanceBegin());
        mapping.add(OrderCol.PERIOD_OF_PERFORMANCE_END, order.getPeriodOfPerformanceEnd());
        mapping.add(OrderCol.PROBABILITY_OF_OCCURRENCE, order.getProbabilityOfOccurrence());
        mapping.add(OrderCol.FORECAST_TYPE, order.getForecastType());

        final PFUserDO contactPerson = UserGroupCache.getInstance().getUserIfNotInitialized(order.getContactPerson());
        mapping.add(OrderCol.CONTACT_PERSON, contactPerson != null ? contactPerson.getFullname() : "");
        mapping.add(OrderCol.REFERENCE, order.getReferenz());
        mapping.add(OrderCol.COMMENT, order.getBemerkung());
    }

    private ExportColumn[] createPosColumns() {
        return new ExportColumn[]{ //
                new I18nExportColumn(PosCol.NUMBER, "fibu.auftrag.nummer.short", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(PosCol.POS_NUMBER, "fibu.auftrag.position", 5),
                new I18nExportColumn(PosCol.DATE_OF_OFFER, "fibu.auftrag.angebot.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(PosCol.DATE_OF_ENTRY, "fibu.auftrag.erfassung.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(PosCol.DATE_OF_DESICION, "fibu.auftrag.entscheidung.datum", MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(PosCol.PROJECT, "fibu.projekt", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PosCol.ORDER_TITLE, "fibu.auftrag.title", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PosCol.TITLE, "fibu.auftrag.title", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PosCol.TYPE, "fibu.auftrag.position.art", 10),
                new I18nExportColumn(PosCol.PAYMENTTYPE, "fibu.auftrag.position.paymenttype", 20),
                new I18nExportColumn(PosCol.STATUS, "status", 10),
                new I18nExportColumn(PosCol.PERSON_DAYS, "projectmanagement.personDays.short", 8),
                new I18nExportColumn(PosCol.NETSUM, "fibu.auftrag.nettoSumme", MyXlsContentProvider.LENGTH_CURRENCY), //
                new I18nExportColumn(PosCol.INVOICED, "fibu.fakturiert", MyXlsContentProvider.LENGTH_CURRENCY), //
                new I18nExportColumn(PosCol.TO_BE_INVOICED, "fibu.tobeinvoiced", MyXlsContentProvider.LENGTH_CURRENCY),
                new I18nExportColumn(PosCol.COMPLETELY_INVOICED, "fibu.auftrag.vollstaendigFakturiert",
                        MyXlsContentProvider.LENGTH_BOOLEAN),
                new I18nExportColumn(PosCol.INVOICES, "fibu.rechnungen", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PosCol.PERIOD_OF_PERFORMANCE_BEGIN, null, MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(PosCol.PERIOD_OF_PERFORMANCE_END, null, MyXlsContentProvider.LENGTH_DATE),
                new I18nExportColumn(OrderCol.PROBABILITY_OF_OCCURRENCE, "fibu.probabilityOfOccurrence", MyXlsContentProvider.LENGTH_PERCENT),
                new I18nExportColumn(OrderCol.FORECAST_TYPE, AuftragForecastType.getBaseKey(), MyXlsContentProvider.LENGTH_PERCENT),
                new I18nExportColumn(OrderCol.CONTACT_PERSON, "contactPerson", 30),
                new I18nExportColumn(PosCol.TASK, "task", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PosCol.COMMENT, "comment", MyXlsContentProvider.LENGTH_COMMENT)};
    }

    private void addPosMapping(final PropertyMapping mapping, final AuftragDO order, final AuftragsPositionDO pos) {
        mapping.add(PosCol.NUMBER, order.getNummer());
        mapping.add(PosCol.POS_NUMBER, "#" + pos.getNumber());
        mapping.add(PosCol.DATE_OF_OFFER, order.getAngebotsDatum());
        mapping.add(PosCol.DATE_OF_ENTRY, order.getErfassungsDatum());
        mapping.add(PosCol.DATE_OF_DESICION, ensureErfassungsDatum(order));
        mapping.add(PosCol.PROJECT, order.getProjektAsString());
        mapping.add(PosCol.ORDER_TITLE, order.getTitel());
        mapping.add(PosCol.TITLE, pos.getTitel());
        mapping.add(PosCol.TYPE,
                pos.getArt() != null ? ThreadLocalUserContext.getLocalizedString(pos.getArt().getI18nKey()) : "");
        mapping.add(PosCol.PAYMENTTYPE,
                pos.getPaymentType() != null ? ThreadLocalUserContext.getLocalizedString(pos.getPaymentType().getI18nKey()) : "");
        mapping.add(PosCol.STATUS,
                pos.getStatus() != null ? ThreadLocalUserContext.getLocalizedString(pos.getStatus().getI18nKey()) :
                        (order.getStatus() != null ? ThreadLocalUserContext.getLocalizedString(order.getStatus().getI18nKey()) : ""));
        mapping.add(PosCol.PERSON_DAYS, pos.getPersonDays());
        var orderInfo = auftragsCache.getOrderInfo(order);
        var posInfo = orderInfo.getInfoPosition(pos.getId());
        final BigDecimal netSum = pos.getNettoSumme() != null ? pos.getNettoSumme() : BigDecimal.ZERO;
        BigDecimal invoicedSum = posInfo != null ? posInfo.getInvoicedSum() : BigDecimal.ZERO;
        BigDecimal toBeInvoicedSum = netSum.subtract(invoicedSum);
        if (pos.getStatus() != null) {
            if (pos.getStatus().equals(AuftragsStatus.ABGELEHNT) || pos.getStatus().equals(AuftragsStatus.ERSETZT) || pos.getStatus()
                    .equals(AuftragsStatus.OPTIONAL)) {
                toBeInvoicedSum = BigDecimal.ZERO;
            }
        }
        mapping.add(PosCol.NETSUM, netSum);
        addCurrency(mapping, PosCol.INVOICED, invoicedSum);
        addCurrency(mapping, PosCol.TO_BE_INVOICED, toBeInvoicedSum);
        mapping.add(PosCol.COMPLETELY_INVOICED, pos.getVollstaendigFakturiert() ? "x" : "");
        final Collection<RechnungPosInfo> invoicePositions = rechnungCache
                .getRechnungsPosInfosByAuftragsPositionId(pos.getId());
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
        mapping.add(OrderCol.PROBABILITY_OF_OCCURRENCE, order.getProbabilityOfOccurrence());
        mapping.add(OrderCol.FORECAST_TYPE, ForecastUtils.getForecastType(order, pos));
        mapping.add(OrderCol.CONTACT_PERSON, order.getContactPerson() != null ? order.getContactPerson().getFullname() : "");
        final TaskDO task = pos.getTask();
        final TaskNode node = task != null ? taskTree.getTaskNodeById(task.getId()) : null;
        mapping.add(PosCol.TASK, node != null ? node.getTask().getTitle() : "");
        mapping.add(PosCol.COMMENT, pos.getBemerkung());
    }

    private Object ensureErfassungsDatum(AuftragDO order) {
        if (order.getErfassungsDatum() == null) {
            if (order.getCreated() == null) {
                if (order.getAngebotsDatum() == null) {
                    order.setErfassungsDatum(PFDay.now().getLocalDate());
                } else {
                    order.setErfassungsDatum(order.getAngebotsDatum());
                }
            } else {
                order.setErfassungsDatum(PFDay.from(order.getCreated()).getLocalDate());
            }
        }
        return order.getErfassungsDatum();
    }

    private void addPaymentsMapping(final PropertyMapping mapping, final AuftragDO order,
                                    final PaymentScheduleDO scheduleDO) {
        mapping.add(PaymentsCol.NUMBER, order.getNummer());
        final Short positionNumber = scheduleDO.getPositionNumber();
        mapping.add(PaymentsCol.POS_NUMBER, positionNumber != null ? "#" + positionNumber : "");
        mapping.add(PaymentsCol.PAY_NUMBER, "#" + scheduleDO.getNumber());
        mapping.add(PaymentsCol.AMOUNT, scheduleDO.getAmount());
        mapping.add(PaymentsCol.COMMENT, scheduleDO.getComment());
        mapping.add(PaymentsCol.REACHED, scheduleDO.getReached() ? "x" : "");
        mapping.add(PaymentsCol.VOLLSTAENDIG_FAKTURIERT, scheduleDO.getVollstaendigFakturiert() ? "x" : "");
        mapping.add(PaymentsCol.SCHEDULE_DATE, scheduleDO.getScheduleDate());
    }

    private String getInvoices(final Collection<RechnungPosInfo> invoicePositions) {
        final StringBuilder sb = new StringBuilder();
        if (invoicePositions != null) {
            String delimiter = "";
            for (final RechnungPosInfo invoicePos : invoicePositions) {
                sb.append(delimiter).append(invoicePos.getRechnungInfo().getNummer());
                delimiter = ", ";
            }
        }
        return sb.toString();
    }

    private void addCurrency(final PropertyMapping mapping, final Enum<?> col, final BigDecimal value) {
        if (NumberHelper.isNotZero(value)) {
            mapping.add(col, value);
        } else {
            mapping.add(col, "");
        }
    }

    /**
     * Exports the filtered list as table with almost all fields. For members of group FINANCE_GROUP (PF_Finance) and
     * MARKETING_GROUP (PF_Marketing) all addresses are exported, for others only those which are marked as personal
     * favorites.
     */
    public byte[] export(final List<AuftragDO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        log.info("Exporting order list.");
        final ExportWorkbook xls = new ExportWorkbook();
        final ContentProvider contentProvider = new MyContentProvider(xls);
        // create a default Date format and currency column
        xls.setContentProvider(contentProvider);

        ExportColumn[] columns = createOrderColumns();
        String sheetTitle = ThreadLocalUserContext.getLocalizedString("fibu.auftrag.auftraege");
        ExportSheet sheet = xls.addSheet(sheetTitle);
        ContentProvider sheetProvider = sheet.getContentProvider();
        sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, OrderCol.NETSUM, OrderCol.INVOICED,
                OrderCol.TO_BE_INVOICED);
        sheetProvider.putFormat(DateFormats.getExcelFormatString(DateFormatType.DATE), OrderCol.DATE_OF_ENTRY, OrderCol.DATE_OF_OFFER, OrderCol.ORDER_DATE);
        sheet.createFreezePane(1, 1);
        sheet.setColumns(columns);
        for (final AuftragDO order : list) {
            final PropertyMapping mapping = new PropertyMapping();
            addOrderMapping(mapping, order);
            sheet.addRow(mapping.getMapping(), 0);
        }
        sheet.setAutoFilter();
        columns = createPosColumns();
        sheetTitle = ThreadLocalUserContext.getLocalizedString("fibu.auftrag.positions");
        sheet = xls.addSheet(sheetTitle);
        sheetProvider = sheet.getContentProvider();
        sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, PosCol.NETSUM, PosCol.INVOICED,
                PosCol.TO_BE_INVOICED);
        sheetProvider.putFormat(DateFormats.getExcelFormatString(DateFormatType.DATE), PosCol.DATE_OF_OFFER, PosCol.DATE_OF_ENTRY,
                PosCol.PERIOD_OF_PERFORMANCE_BEGIN,
                PosCol.PERIOD_OF_PERFORMANCE_END);
        sheet.createFreezePane(1, 1);
        sheet.setColumns(columns);
        sheet.setMergedRegion(0, 0, PosCol.PERIOD_OF_PERFORMANCE_BEGIN.ordinal(),
                PosCol.PERIOD_OF_PERFORMANCE_END.ordinal(),
                ThreadLocalUserContext.getLocalizedString("fibu.periodOfPerformance"));
        for (final AuftragDO order : list) {
            for (final AuftragsPositionDO pos : order.getPositionenExcludingDeleted()) {
                final PropertyMapping mapping = new PropertyMapping();
                addPosMapping(mapping, order, pos);
                sheet.addRow(mapping.getMapping(), 0);
            }
        }
        sheet.setAutoFilter();

        columns = createPaymentColumns();
        sheetTitle = ThreadLocalUserContext.getLocalizedString("fibu.auftrag.paymentschedule");
        sheet = xls.addSheet(sheetTitle);
        sheetProvider = sheet.getContentProvider();
        sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, PaymentsCol.AMOUNT);
        sheet.createFreezePane(1, 1);
        sheet.setColumns(columns);
        for (final AuftragDO order : list) {
            if (order.getPaymentSchedules() == null) {
                continue;
            }
            for (final PaymentScheduleDO paymentScheduleDO : order.getPaymentSchedules()) {
                final PropertyMapping mapping = new PropertyMapping();
                addPaymentsMapping(mapping, order, paymentScheduleDO);
                sheet.addRow(mapping.getMapping(), 0);
            }
        }
        sheet.setAutoFilter();
        return xls.getAsByteArray();
    }

    private ExportColumn[] createPaymentColumns() {
        return new ExportColumn[]{
                new I18nExportColumn(PaymentsCol.NUMBER, "fibu.auftrag.nummer.short", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(PaymentsCol.POS_NUMBER, "fibu.auftrag.position", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(PaymentsCol.PAY_NUMBER, "fibu.auftrag.zahlung", MyXlsContentProvider.LENGTH_ID),
                new I18nExportColumn(PaymentsCol.AMOUNT, "fibu.common.betrag", MyXlsContentProvider.LENGTH_CURRENCY),
                new I18nExportColumn(PaymentsCol.COMMENT, "comment", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PaymentsCol.REACHED, "fibu.common.reached", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PaymentsCol.VOLLSTAENDIG_FAKTURIERT, "fibu.auftrag.vollstaendigFakturiert", MyXlsContentProvider.LENGTH_STD),
                new I18nExportColumn(PaymentsCol.SCHEDULE_DATE, "date", MyXlsContentProvider.LENGTH_DATE)
        };
    }

    private enum OrderCol {
        NUMMER, NUMBER_OF_POSITIONS, DATE_OF_OFFER, DATE_OF_ENTRY, DATE_OF_DESICION, ORDER_DATE, STATUS, STATUS_COMMENT, PROJECT, PROJECT_CUSTOMER, TITLE, PROJECTMANAGER, HEADOFBUSINESSMANAGER, SALESMANAGER, NETSUM, INVOICED, TO_BE_INVOICED, COMPLETELY_INVOICED, INVOICES, PERIOD_OF_PERFORMANCE_BEGIN, PERIOD_OF_PERFORMANCE_END, PROBABILITY_OF_OCCURRENCE, FORECAST_TYPE, CONTACT_PERSON, REFERENCE, COMMENT
    }

    private enum PosCol {
        NUMBER, POS_NUMBER, DATE_OF_OFFER, DATE_OF_ENTRY, DATE_OF_DESICION, PROJECT, ORDER_TITLE, TITLE, TYPE, PAYMENTTYPE, STATUS, PERSON_DAYS, NETSUM, INVOICED, TO_BE_INVOICED, COMPLETELY_INVOICED, INVOICES, PERIOD_OF_PERFORMANCE_BEGIN, PERIOD_OF_PERFORMANCE_END, TASK, COMMENT
    }

    private enum PaymentsCol {
        NUMBER, POS_NUMBER, PAY_NUMBER, AMOUNT, COMMENT, REACHED, VOLLSTAENDIG_FAKTURIERT, SCHEDULE_DATE
    }

    private class MyContentProvider extends MyXlsContentProvider {
        private MyContentProvider(final ExportWorkbook workbook) {
            super(workbook);
        }

        @Override
        public ContentProvider newInstance() {
            return new MyContentProvider(this.workbook);
        }
    }
}
