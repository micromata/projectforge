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

package org.projectforge.business.fibu.kost;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.excel.*;
import org.projectforge.business.fibu.*;
import org.projectforge.common.StringHelper;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.CurrencyHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class KostZuweisungExport {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KostZuweisungExport.class);

  @Autowired
  KontoDao kontoDao;

  private class MyContentProvider extends MyXlsContentProvider {
    public MyContentProvider(final ExportWorkbook workbook) {
      super(workbook);
    }

    @Override
    public ContentProvider newInstance() {
      return new MyContentProvider(this.workbook);
    }
  }

  private enum InvoicesCol {
    BRUTTO("fibu.common.brutto", MyXlsContentProvider.LENGTH_CURRENCY), //
    VAT("fibu.common.vat", MyXlsContentProvider.LENGTH_BOOLEAN), //
    KONTO("fibu.buchungssatz.konto", 14), //
    REFERENZ("fibu.common.reference", MyXlsContentProvider.LENGTH_STD), //
    DATE("date", MyXlsContentProvider.LENGTH_DATE), //
    GEGENKONTO("fibu.buchungssatz.gegenKonto", 14), //
    KOST1("fibu.kost1", MyXlsContentProvider.LENGTH_KOSTENTRAEGER), //
    KOST2("fibu.kost2", MyXlsContentProvider.LENGTH_KOSTENTRAEGER), //
    TEXT("description", MyXlsContentProvider.LENGTH_EXTRA_LONG), //
    KORREKTUR("fibu.common.fehlBetrag", MyXlsContentProvider.LENGTH_CURRENCY);

    final String theTitle;

    final int width;

    InvoicesCol(final String theTitle, final int width) {
      this.theTitle = theTitle;
      this.width = (short) width;
    }
  }

  /**
   * Export all cost assignements of the given invoices as excel list.
   *
   * @param list
   * @return
   */
  public byte[] exportRechnungen(final List<? extends AbstractRechnungDO> list,
                                 final String sheetTitle, final KontoCache kontoCache) {
    final List<KostZuweisungDO> zuweisungen = new ArrayList<>();
    for (final AbstractRechnungDO rechnung : list) {
      if (rechnung.getAbstractPositionen() != null) {
        for (final AbstractRechnungsPositionDO position : rechnung.getAbstractPositionen()) {
          if (CollectionUtils.isNotEmpty(position.getKostZuweisungen())) {
            for (final KostZuweisungDO zuweisung : position.getKostZuweisungen()) {
              if (NumberHelper.isZeroOrNull(zuweisung.getBrutto())) {
                // Skip entries with zero amounts.
                continue;
              }
              zuweisungen.add(zuweisung);
            }
          } else {
            final KostZuweisungDO zuweisung = new KostZuweisungDO();
            if (position instanceof RechnungsPositionDO) {
              zuweisung.setRechnungsPosition((RechnungsPositionDO) position);
            } else {
              zuweisung.setEingangsrechnungsPosition((EingangsrechnungsPositionDO) position);
            }
            zuweisungen.add(zuweisung);
          }
        }
      }
    }
    return export(zuweisungen, sheetTitle, kontoCache);
  }

  /**
   * Exports the filtered list as table.
   */
  public byte[] export(final List<KostZuweisungDO> list, final String sheetTitle, final KontoCache kontoCache) {
    log.info("Exporting kost zuweisung list.");
    final ExportWorkbook xls = new ExportWorkbook();
    final ContentProvider contentProvider = new MyContentProvider(xls);
    // create a default Date format and currency column
    xls.setContentProvider(contentProvider);

    final ExportSheet sheet = xls.addSheet(sheetTitle);
    sheet.createFreezePane(0, 1);

    final ExportColumn[] cols = new ExportColumn[InvoicesCol.values().length];
    int i = 0;
    for (final InvoicesCol col : InvoicesCol.values()) {
      cols[i++] = new I18nExportColumn(col, col.theTitle, col.width);
    }

    // column property names
    sheet.setColumns(cols);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    sheetProvider.putFormat(InvoicesCol.BRUTTO, "#,##0.00;[Red]-#,##0.00");
    sheetProvider.putFormat(InvoicesCol.KORREKTUR, "#,##0.00;[Red]-#,##0.00");
    sheetProvider.putFormat(InvoicesCol.KOST1, "#");
    sheetProvider.putFormat(InvoicesCol.KOST2, "#");
    sheetProvider.putFormat(InvoicesCol.DATE, "dd.MM.yyyy");

    final PropertyMapping mapping = new PropertyMapping();
    for (final KostZuweisungDO zuweisung : list) {
      final AbstractRechnungsPositionDO position;
      final AbstractRechnungDO rechnung;
      final String referenz;
      final String text;
      if (zuweisung.getRechnungsPosition() != null) {
        position = zuweisung.getRechnungsPosition();
        rechnung = ((RechnungsPositionDO) position).getRechnung();
        final RechnungDO r = (RechnungDO) rechnung;
        referenz = String.valueOf(r.getNummer());
        text = ProjektFormatter.formatProjektKundeAsString(r.getProjekt(), r.getKunde(), r.getKundeText());
      } else {
        position = zuweisung.getEingangsrechnungsPosition();
        rechnung = ((EingangsrechnungsPositionDO) position).getEingangsrechnung();
        final EingangsrechnungDO r = (EingangsrechnungDO) rechnung;
        referenz = r.getReferenz();
        text = r.getKreditor();
      }
      final BigDecimal grossSum = position.getBruttoSum();

      BigDecimal korrektur = null;
      if (grossSum.compareTo(position.getKostZuweisungGrossSum()) != 0) {
        korrektur = CurrencyHelper.getGrossAmount(position.getKostZuweisungNetFehlbetrag(), position.getVat());
        if (NumberHelper.isZeroOrNull(korrektur)) {
          korrektur = null;
        }
      }
      mapping.add(InvoicesCol.BRUTTO, zuweisung.getBrutto());
      mapping.add(InvoicesCol.VAT, NumberHelper.isNotZero(position.getVat()));
      Integer kontoNummer = null;
      if (rechnung instanceof RechnungDO) {
        final KontoDO konto = kontoCache.getKonto(((RechnungDO) rechnung));
        if (konto != null) {
          kontoNummer = konto.getNummer();
        }
      } else if (rechnung instanceof EingangsrechnungDO) {
        final Integer kontoId = ((EingangsrechnungDO) rechnung).getKontoId();
        if (kontoId != null) {
          final KontoDO konto = kontoCache.getKonto(kontoId);
          if (konto != null) {
            kontoNummer = konto.getNummer();
          }
        }
      }
      mapping.add(InvoicesCol.KONTO, kontoNummer != null ? kontoNummer : "");
      mapping.add(InvoicesCol.REFERENZ, StringHelper.removeNonDigitsAndNonASCIILetters(referenz));
      mapping.add(InvoicesCol.DATE, rechnung.getDatum());
      mapping.add(InvoicesCol.GEGENKONTO, "");
      mapping.add(InvoicesCol.KOST1, zuweisung.getKost1() != null ? zuweisung.getKost1().getNummer() : "");
      mapping.add(InvoicesCol.KOST2, zuweisung.getKost2() != null ? zuweisung.getKost2().getNummer() : "");
      mapping.add(InvoicesCol.TEXT, text);
      mapping.add(InvoicesCol.KORREKTUR, korrektur);
      sheet.addRow(mapping.getMapping(), 0);
    }
    addAccounts(xls, contentProvider);
    return xls.getAsByteArray();
  }

  private enum AccountsCol {
    NUMBER("fibu.konto.nummer", 16), //
    NAME("fibu.konto.bezeichnung", MyXlsContentProvider.LENGTH_STD), //
    STATUS("status", 14), //
    DATE_OF_LAST_MODIFICATION("lastUpdate", MyXlsContentProvider.LENGTH_TIMESTAMP), //
    DATE_OF_CREATION("created", MyXlsContentProvider.LENGTH_TIMESTAMP), //
    DESCRIPTION("comment", MyXlsContentProvider.LENGTH_EXTRA_LONG);

    final String theTitle;

    final int width;

    AccountsCol(final String theTitle, final int width) {
      this.theTitle = theTitle;
      this.width = (short) width;
    }
  }

  private void addAccounts(final ExportWorkbook xls, final ContentProvider contentProvider) {
    final ExportSheet sheet = xls.addSheet(ThreadLocalUserContext.getLocalizedString("fibu.konto.konten"));
    sheet.createFreezePane(0, 1);

    final ExportColumn[] cols = new ExportColumn[AccountsCol.values().length];
    int i = 0;
    for (final AccountsCol col : AccountsCol.values()) {
      cols[i++] = new I18nExportColumn(col, col.theTitle, col.width);
    }

    // column property names
    sheet.setColumns(cols);

    final ContentProvider sheetProvider = sheet.getContentProvider();
    sheetProvider.putFormat(AccountsCol.DATE_OF_LAST_MODIFICATION, "dd.MM.yyyy HH:mm");
    sheetProvider.putFormat(AccountsCol.DATE_OF_CREATION, "dd.MM.yyyy HH:mm");
    sheetProvider.putFormat(AccountsCol.NUMBER, "#");

    final QueryFilter filter = new QueryFilter();
    filter.addOrder(SortProperty.desc("lastUpdate"));
    final List<KontoDO> list = kontoDao.getList(filter);

    final PropertyMapping mapping = new PropertyMapping();
    for (final KontoDO konto : list) {
      mapping.add(AccountsCol.NUMBER, konto.getNummer());
      mapping.add(AccountsCol.NAME, konto.getBezeichnung());
      mapping.add(AccountsCol.DATE_OF_LAST_MODIFICATION, konto.getLastUpdate());
      mapping.add(AccountsCol.DATE_OF_CREATION, konto.getCreated());
      String status = "";
      if (konto.isDeleted()) {
        status = ThreadLocalUserContext.getLocalizedString("deleted");
      } else if (konto.getStatus() != null) {
        status = ThreadLocalUserContext.getLocalizedString(konto.getStatus().getI18nKey());
      }
      mapping.add(AccountsCol.STATUS, status);
      mapping.add(AccountsCol.DESCRIPTION, konto.getDescription());
      sheet.addRow(mapping.getMapping(), 0);
    }
  }
}
