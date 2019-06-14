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

package org.projectforge.plugins.eed.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.excel.ExcelImportException;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.utils.ImportStatus;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.excelimport.EmployeeBillingExcelImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

@Service
public class EmployeeBillingImportService
{
  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private TimeableService timeableService;

  private List<AttrColumnDescription> attrColumnsInSheet;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeBillingImportService.class);

  public ImportStorage<EmployeeDO> importData(final InputStream is, final String filename, final Date dateToSelectAttrRow) throws IOException
  {
    //checkLoggeinUserRight(accessChecker);
    final ImportStorage<EmployeeDO> storage = new ImportStorage<>();
    storage.setFilename(filename);
    final EmployeeBillingExcelImporter importer = new EmployeeBillingExcelImporter(employeeService, timeableService, storage, dateToSelectAttrRow);
    try {
      attrColumnsInSheet = importer.doImport(is);
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
    final ImportedSheet<EmployeeDO> sheet = (ImportedSheet<EmployeeDO>) storage.getNamedSheet(sheetName);
    Validate.notNull(sheet);

    for (final ImportedElement<EmployeeDO> el : sheet.getElements()) {
      final EmployeeDO employee = el.getValue();
      final Integer id = employee.getId();
      final EmployeeDO dbEmployee = (id != null) ? employeeService.selectByPkDetached(id) : null;
      if (dbEmployee != null) {
        el.setOldValue(dbEmployee);
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
    final int num = commit((ImportedSheet<EmployeeDO>) sheet);
    sheet.setNumberOfCommittedElements(num);
  }

  public List<AttrColumnDescription> getAttrColumnsInSheet()
  {
    return attrColumnsInSheet;
  }

  private int commit(final ImportedSheet<EmployeeDO> sheet)
  {
    final List<EmployeeDO> employeesToUpdate = sheet
        .getElements()
        .stream()
        .filter(ImportedElement::isSelected)
        .map(ImportedElement::getValue)
        .collect(Collectors.toList());

    employeesToUpdate.forEach(employeeService::update);

    return employeesToUpdate.size();
  }
}
