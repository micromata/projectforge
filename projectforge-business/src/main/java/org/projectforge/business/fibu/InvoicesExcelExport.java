/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.excel.*;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.props.PropUtils;
import org.projectforge.export.MyExcelExporter;

import java.lang.reflect.Field;
import java.util.Collection;

public class InvoicesExcelExport
{
  public InvoicesExcelExport()
  {
  }

  public void addDebitorInvoicesSheet(final MyExcelExporter exporter, final String title, final Collection<RechnungDO> list)
  {
    final ExportSheet sheet = exporter.addSheet(title);
    sheet.createFreezePane(0, 1);
    final ContentProvider sheetProvider = sheet.getContentProvider();

    final ExportColumn[] cols = new ExportColumn[6];
    int i = 0;
    cols[i++] = createColumn(exporter, sheet, AbstractRechnungDO.class, "datum");
    cols[i++] = createColumn(exporter, sheet, AbstractRechnungDO.class, "faelligkeit");
    cols[i] = new I18nExportColumn("gross", "fibu.rechnung.bruttoBetrag", 10);
    exporter.putCurrencyFormat(sheetProvider, cols[i++]);
    cols[i++] = new I18nExportColumn("number", "fibu.rechnung.nummer", 6);
    cols[i++] = new I18nExportColumn("debitor", "fibu.common.debitor", 60);
    cols[i++] = new I18nExportColumn("subject", PropUtils.getI18nKey(AbstractRechnungDO.class, "betreff"), 100);
    // column property names
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    for (final RechnungDO entry : list) {
      entry.recalculate();
      mapping.add("datum", entry.getDatum());
      mapping.add("faelligkeit", entry.getFaelligkeit());
      mapping.add("gross", entry.getGrossSum());
      mapping.add("number", entry.getNummer());
      mapping.add("debitor", entry.getKundeAsString());
      mapping.add("subject", entry.getBetreff());
      sheet.addRow(mapping.getMapping(), 0);
    }
  }

  public void addCreditorInvoicesSheet(final MyExcelExporter exporter, final String title, final Collection<EingangsrechnungDO> list)
  {
    final ExportSheet sheet = exporter.addSheet(title);
    sheet.createFreezePane(0, 1);
    final ContentProvider sheetProvider = sheet.getContentProvider();

    final ExportColumn[] cols = new ExportColumn[5];
    int i = 0;
    cols[i++] = createColumn(exporter, sheet, AbstractRechnungDO.class, "datum");
    cols[i++] = createColumn(exporter, sheet, AbstractRechnungDO.class, "faelligkeit");
    cols[i] = new I18nExportColumn("gross", "fibu.rechnung.bruttoBetrag", 10);
    exporter.putCurrencyFormat(sheetProvider, cols[i++]);
    cols[i++] = new I18nExportColumn("creditor", "fibu.common.creditor", 60);
    cols[i++] = new I18nExportColumn("subject", PropUtils.getI18nKey(AbstractRechnungDO.class, "betreff"), 100);
    // column property names
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    if (list != null) {
      for (final EingangsrechnungDO entry : list) {
        entry.recalculate();
        mapping.add("datum", entry.getDatum());
        mapping.add("faelligkeit", entry.getFaelligkeit());
        mapping.add("gross", entry.getGrossSum());
        mapping.add("creditor", entry.getKreditor());
        mapping.add("subject", entry.getBetreff());
        sheet.addRow(mapping.getMapping(), 0);
      }
    }
  }

  private I18nExportColumn createColumn(final ExcelExporter exporter, final ExportSheet sheet, final Class<?> clazz, final String property)
  {
    final Field field = PropUtils.getField(clazz, property);
    final PropertyInfo propInfo = PropUtils.get(field);
    final I18nExportColumn exportColumn = new I18nExportColumn(property, propInfo.i18nKey(), 100);
    exporter.putFieldFormat(sheet, field, propInfo, exportColumn);
    return exportColumn;
  }
}
