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

package org.projectforge.export;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.PropertyMapping;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class DOWithAttrListExcelExporter<PK extends Serializable, T extends TimeableAttrRow<PK>> extends DOListExcelExporter
{
  private final TimeableService timeableService;

  private final String[] fieldsToExport;

  private final List<AttrColumnDescription> attrFieldsToExport;

  private final Date dateToSelectAttrRow;

  public DOWithAttrListExcelExporter(final String filenameIdentifier, final TimeableService timeableService, final String[] fieldsToExport,
      final List<AttrColumnDescription> attrFieldsToExport, final Date dateToSelectAttrRow)
  {
    super(filenameIdentifier);
    this.timeableService = timeableService;
    this.fieldsToExport = fieldsToExport;
    this.attrFieldsToExport = attrFieldsToExport;
    this.dateToSelectAttrRow = dateToSelectAttrRow;
  }

  @Override
  protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider, final List<ExportColumn> columns)
  {
    final List<ExportColumn> exportColumns = reorderAndRemoveOtherColumns(columns, fieldsToExport);

    // add the attr fields
    attrFieldsToExport
        .stream()
        .map(AttrColumnDescription::toI18nExportColumn)
        .forEach(exportColumns::add);

    return exportColumns;
  }

  @Override
  protected void addMappings(PropertyMapping mapping, Object entry)
  {
    @SuppressWarnings("unchecked")
    final EntityWithTimeableAttr<PK, T> entity = (EntityWithTimeableAttr<PK, T>) entry;

    for (final AttrColumnDescription attrFieldToExport : attrFieldsToExport) {
      final T attrRow = timeableService.getAttrRowForSameMonth(entity, attrFieldToExport.getGroupName(), dateToSelectAttrRow);

      if (attrRow != null) {
        final String attributeValue = attrRow.getStringAttribute(attrFieldToExport.getPropertyName());
        mapping.add(attrFieldToExport.getCombinedName(), attributeValue);
      }
    }
  }

}
