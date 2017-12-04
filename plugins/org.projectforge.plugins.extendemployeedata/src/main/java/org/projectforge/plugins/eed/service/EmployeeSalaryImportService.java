package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.excel.ExcelImportException;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.api.EmployeeSalaryService;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.utils.ImportStatus;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.excelimport.EmployeeSalaryExcelImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeSalaryImportService
{
  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private EmployeeSalaryService employeeSalaryService;

  @Autowired
  private EmployeeConfigurationService employeeConfigService;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryImportService.class);

  public ImportStorage<EmployeeSalaryDO> importData(final InputStream is, final String filename, final Date dateToSelectAttrRow) throws IOException
  {
    //checkLoggeinUserRight(accessChecker);
    final ImportStorage<EmployeeSalaryDO> storage = new ImportStorage<>();
    storage.setFilename(filename);
    final EmployeeSalaryExcelImporter importer = new EmployeeSalaryExcelImporter(employeeService, employeeSalaryService, employeeConfigService, storage,
        dateToSelectAttrRow);
    try {
      importer.doImport(is);
    } catch (final ExcelImportException ex) {
      throw new UserException("common.import.excel.error", ex.getMessage(), ex.getRow(), ex.getColumnname());
    }
    return storage;
  }

  public void reconcile(final ImportStorage<?> storage, final String sheetName)
  {
    //    checkLoggeinUserRight(accessChecker);
    Validate.notNull(storage.getSheets());
    @SuppressWarnings("unchecked")
    final ImportedSheet<EmployeeSalaryDO> sheet = (ImportedSheet<EmployeeSalaryDO>) storage.getNamedSheet(sheetName);
    Validate.notNull(sheet);

    for (final ImportedElement<EmployeeSalaryDO> el : sheet.getElements()) {
      final EmployeeSalaryDO employeeSalary = el.getValue();
      if (employeeSalary != null) {
        final Integer id = employeeSalary.getId();
        final EmployeeSalaryDO dbEmployeeSalary = (id != null) ? employeeSalaryService.selectByPk(id) : null;
        if (dbEmployeeSalary != null) {
          el.setOldValue(dbEmployeeSalary);
        }
      }
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();

    sheet.setNumberOfCommittedElements(-1);
  }

  public void commit(final ImportStorage<?> storage, final String sheetName)
  {
    //    checkLoggeinUserRight(accessChecker);
    Validate.notNull(storage.getSheets());
    final ImportedSheet<?> sheet = storage.getNamedSheet(sheetName);
    Validate.notNull(sheet);
    if (sheet.getStatus() != ImportStatus.RECONCILED) {
      throw new UserException("common.import.action.commit.error.notReconciled");
    }
    @SuppressWarnings("unchecked")
    final int num = commit((ImportedSheet<EmployeeSalaryDO>) sheet);
    sheet.setNumberOfCommittedElements(num);
  }

  private int commit(final ImportedSheet<EmployeeSalaryDO> sheet)
  {
    final List<EmployeeSalaryDO> employeeSalariesToUpdate = sheet
        .getElements()
        .stream()
        .filter(ImportedElement::isSelected)
        .map(ImportedElement::getValue)
        .collect(Collectors.toList());

    employeeSalariesToUpdate.forEach(sal -> {
      //Correct view to db
      sal.setMonth(sal.getMonth() - 1);
      employeeSalaryService.saveOrUpdate(sal);
    });

    return employeeSalariesToUpdate.size();
  }

}
