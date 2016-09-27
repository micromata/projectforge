package org.projectforge.export;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportColumn;
import org.projectforge.excel.PropertyMapping;

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
