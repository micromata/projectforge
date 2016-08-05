package org.projectforge.export;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportColumn;
import org.projectforge.excel.I18nExportColumn;
import org.projectforge.excel.PropertyMapping;
import org.projectforge.framework.time.DateHelper;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class DOWithAttrListExcelExporter<PK extends Serializable, T extends TimeableAttrRow<PK>> extends DOListExcelExporter
{
  private final TimeableService<PK, T> timeableService;

  private final String[] fieldsToExport;

  private final AttrColumnDescription[] attrFieldsToExport;

  private final Date dateToSelectAttrRow;

  public DOWithAttrListExcelExporter(final String filenameIdentifier, final TimeableService<PK, T> timeableService, final String[] fieldsToExport,
      final AttrColumnDescription[] attrFieldsToExport, final Date dateToSelectAttrRow)
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

    for (final AttrColumnDescription attrFieldToExport : attrFieldsToExport) {
      exportColumns.add(new I18nExportColumn(attrFieldToExport.getCombinedName(), "fibu.employee.tabs.salary")); // TODO CT: i18n key
    }

    return exportColumns;
  }

  @Override
  protected void addMappings(PropertyMapping mapping, Object entry)
  {
    @SuppressWarnings("unchecked")
    final EntityWithTimeableAttr<PK, T> entity = (EntityWithTimeableAttr<PK, T>) entry;

    for (final AttrColumnDescription attrFieldToExport : attrFieldsToExport) {
      final List<T> attrRowsOfGroup = timeableService.getTimeableAttrRowsForGroupName(entity, attrFieldToExport.getGroupName());
      final T attrRow = getAttrRowForSameMonth(attrRowsOfGroup);

      if (attrRow != null) {
        final String attributeValue = attrRow.getStringAttribute(attrFieldToExport.getPropertyName());
        mapping.add(attrFieldToExport.getCombinedName(), attributeValue);
      }
    }
  }

  private T getAttrRowForSameMonth(final List<T> attrRows)
  {
    return attrRows
        .stream()
        .filter(row -> (row.getStartTime() != null && DateHelper.isSameMonth(row.getStartTime(), dateToSelectAttrRow)))
        .findFirst()
        .orElse(null);
  }
}
