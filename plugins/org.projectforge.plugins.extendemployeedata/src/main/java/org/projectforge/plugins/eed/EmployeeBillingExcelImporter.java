package org.projectforge.plugins.eed;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.excel.ExcelImport;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.util.bean.PrivateBeanUtils;

public class EmployeeBillingExcelImporter
{
  private static final Logger log = Logger.getLogger(EmployeeBillingExcelRow.class);

  private static final String NAME_OF_EXCEL_SHEET = "employees";

  private static final int ROW_INDEX_OF_COLUMN_NAMES = 0;

  private static final String[] DIFF_PROPERTIES = { "staffNumber" }; // TODO CT

  private final EmployeeService employeeService;

  private final TimeableService<Integer, EmployeeTimedDO> timeableService;

  private final ImportStorage<EmployeeDO> storage;

  public EmployeeBillingExcelImporter(final EmployeeService employeeService, final TimeableService<Integer, EmployeeTimedDO> timeableService,
      final ImportStorage<EmployeeDO> storage)
  {
    this.employeeService = employeeService;
    this.timeableService = timeableService;
    this.storage = storage;
  }

  public void doImport(final InputStream is) throws IOException
  {
    final ExcelImport<EmployeeBillingExcelRow> importer = new ExcelImport<>(is);

    // search the sheet
    for (short idx = 0; idx < importer.getWorkbook().getNumberOfSheets(); idx++) {
      importer.setActiveSheet(idx);
      final String name = importer.getWorkbook().getSheetName(idx);
      if (NAME_OF_EXCEL_SHEET.equals(name)) {
        importer.setActiveSheet(idx);
        //        final HSSFSheet sheet = importer.getWorkbook().getSheetAt(idx);
        importEmployeeBillings(importer);
        return;
      }
    }
    log.error("Oups, no sheet named '" + NAME_OF_EXCEL_SHEET + "' found.");
  }

  private void importEmployeeBillings(final ExcelImport<EmployeeBillingExcelRow> importer)
  {
    final ImportedSheet<EmployeeDO> importedSheet = new ImportedSheet<>();
    storage.addSheet(importedSheet);
    importedSheet.setName(NAME_OF_EXCEL_SHEET);
    importer.setNameRowIndex(ROW_INDEX_OF_COLUMN_NAMES);
    importer.setStartingRowIndex(ROW_INDEX_OF_COLUMN_NAMES + 1);

    // mapping from excel column name to the bean field name
    final Map<String, String> map = new HashMap<>();
    map.put("Id", "id");
    map.put("Personalnummer", "staffNumber");

    ExtendEmployeeDataConstants.ATTR_FIELDS_TO_EDIT.forEach(
        desc -> map.put(I18nHelper.getLocalizedString(desc.getI18nKey()), desc.getCombinedName())
    );
    importer.setColumnMapping(map);

    final EmployeeBillingExcelRow[] rows = importer.convertToRows(EmployeeBillingExcelRow.class);
    for (final EmployeeBillingExcelRow row : rows) {
      final ImportedElement<EmployeeDO> element = convertRowToDo(row);
      importedSheet.addElement(element);
    }
  }

  private ImportedElement<EmployeeDO> convertRowToDo(final EmployeeBillingExcelRow row)
  {
    // TODO CT
    final Date dateToSelectAttrRow = Date.from(LocalDateTime.of(2016, 8, 1, 0, 0).toInstant(ZoneOffset.UTC));

    final ImportedElement<EmployeeDO> element = new ImportedElement<>(storage.nextVal(), EmployeeDO.class, DIFF_PROPERTIES);
    EmployeeDO employee;
    if (row.getId() != null) {
      employee = employeeService.selectByPkDetached(row.getId());
    } else {
      // this employee is just created to show it in the EmployeeBillingImportStoragePanel, it will never be imported to the DB
      employee = new EmployeeDO();
      element.putErrorProperty("id", row.getId());
    }
    element.setValue(employee);

    employee.setStaffNumber(row.getStaffNumber());

    ExtendEmployeeDataConstants.ATTR_FIELDS_TO_EDIT.forEach(
        desc -> getOrCreateAttrRowAndPutAttribute(employee, dateToSelectAttrRow, desc, row)
    );

    return element;
  }

  private void getOrCreateAttrRowAndPutAttribute(final EmployeeDO employee, final Date dateToSelectAttrRow, final AttrColumnDescription colDesc,
      final EmployeeBillingExcelRow row)
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
