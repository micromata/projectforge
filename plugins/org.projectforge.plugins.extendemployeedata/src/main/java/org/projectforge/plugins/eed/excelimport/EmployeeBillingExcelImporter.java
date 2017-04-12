package org.projectforge.plugins.eed.excelimport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.excel.ExcelImport;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedElementWithAttrs;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.ExtendEmployeeDataEnum;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.util.bean.PrivateBeanUtils;

public class EmployeeBillingExcelImporter
{
  private static final Logger log = Logger.getLogger(EmployeeBillingExcelRow.class);

  private static final String NAME_OF_EXCEL_SHEET = "employees";

  private static final int ROW_INDEX_OF_COLUMN_NAMES = 0;

  private static final String[] DIFF_PROPERTIES = {};

  private final EmployeeService employeeService;

  private final TimeableService timeableService;

  private final ImportStorage<EmployeeDO> storage;

  private final Date dateToSelectAttrRow;

  public EmployeeBillingExcelImporter(final EmployeeService employeeService,
      final TimeableService timeableService,
      final ImportStorage<EmployeeDO> storage, final Date dateToSelectAttrRow)
  {
    this.employeeService = employeeService;
    this.timeableService = timeableService;
    this.storage = storage;
    this.dateToSelectAttrRow = dateToSelectAttrRow;
  }

  public List<AttrColumnDescription> doImport(final InputStream is) throws IOException
  {
    final ExcelImport<EmployeeBillingExcelRow> importer = new ExcelImport<>(is);

    // search the sheet
    for (short idx = 0; idx < importer.getWorkbook().getNumberOfSheets(); idx++) {
      importer.setActiveSheet(idx);
      final String name = importer.getWorkbook().getSheetName(idx);
      if (NAME_OF_EXCEL_SHEET.equals(name)) {
        importer.setActiveSheet(idx);
        //        final HSSFSheet sheet = importer.getWorkbook().getSheetAt(idx);
        return importEmployeeBillings(importer);
      }
    }
    log.error("Oups, no sheet named '" + NAME_OF_EXCEL_SHEET + "' found.");
    return null;
  }

  private List<AttrColumnDescription> importEmployeeBillings(final ExcelImport<EmployeeBillingExcelRow> importer)
  {
    final ImportedSheet<EmployeeDO> importedSheet = new ImportedSheet<>();
    storage.addSheet(importedSheet);
    importedSheet.setName(NAME_OF_EXCEL_SHEET);
    importer.setNameRowIndex(ROW_INDEX_OF_COLUMN_NAMES);
    importer.setStartingRowIndex(ROW_INDEX_OF_COLUMN_NAMES + 1);

    // mapping from excel column name to the bean field name
    final Map<String, String> map = new HashMap<>();
    map.put("Id", "id");
    map.put(I18nHelper.getLocalizedMessage("fibu.employee.user"), "fullName");

    ExtendEmployeeDataEnum.getAllAttrColumnDescriptions().forEach(
        desc -> map.put(I18nHelper.getLocalizedMessage(desc.getI18nKey()), desc.getCombinedName()));
    importer.setColumnMapping(map);

    final List<AttrColumnDescription> attrColumnsInSheet = getAttrColumnsUsedInSheet(importer);

    final EmployeeBillingExcelRow[] rows = importer.convertToRows(EmployeeBillingExcelRow.class);
    for (final EmployeeBillingExcelRow row : rows) {
      final ImportedElement<EmployeeDO> element = convertRowToDo(attrColumnsInSheet, row);
      importedSheet.addElement(element);
    }

    return attrColumnsInSheet;
  }

  private List<AttrColumnDescription> getAttrColumnsUsedInSheet(ExcelImport<EmployeeBillingExcelRow> importer)
  {
    final List<String> columnNames = importer.getColumnNames();
    return ExtendEmployeeDataEnum
        .getAllAttrColumnDescriptions()
        .stream()
        .filter(desc -> columnNames.contains(I18nHelper.getLocalizedMessage(desc.getI18nKey())))
        .collect(Collectors.toList());
  }

  private ImportedElement<EmployeeDO> convertRowToDo(final List<AttrColumnDescription> attrColumnsInSheet, final EmployeeBillingExcelRow row)
  {
    final ImportedElement<EmployeeDO> element = new ImportedElementWithAttrs<>(storage.nextVal(), EmployeeDO.class, DIFF_PROPERTIES, attrColumnsInSheet,
        dateToSelectAttrRow, timeableService);
    EmployeeDO employee;
    if (row.getId() != null) {
      employee = employeeService.selectByPkDetached(row.getId());
      // validate ID and USER: make sure that full name has not changed
      if (!StringUtils.equals(row.getFullName(), employee.getUser().getFullname())) {
        element.putErrorProperty("user", row.getFullName());
      }
    } else {
      // this employee is just created to show it in the EmployeeBillingImportStoragePanel, it will never be imported to the DB
      employee = new EmployeeDO();
      element.putErrorProperty("id", row.getId());
    }
    element.setValue(employee);

    attrColumnsInSheet.forEach(
        desc -> getOrCreateAttrRowAndPutAttribute(employee, desc, row)
    );

    return element;
  }

  private void getOrCreateAttrRowAndPutAttribute(final EmployeeDO employee, final AttrColumnDescription colDesc, final EmployeeBillingExcelRow row)
  {
    EmployeeTimedDO attrRow = timeableService.getAttrRowForSameMonth(employee, colDesc.getGroupName(), dateToSelectAttrRow);

    if (attrRow == null) {
      attrRow = employeeService.addNewTimeAttributeRow(employee, colDesc.getGroupName());
      attrRow.setStartTime(dateToSelectAttrRow);
    }

    final Object fieldValue = PrivateBeanUtils.readField(row, colDesc.getCombinedName());
    attrRow.putAttribute(colDesc.getPropertyName(), fieldValue);
  }
}
