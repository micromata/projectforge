package org.projectforge.plugins.eed.wicket;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.service.EmployeeBillingImportService;
import org.projectforge.web.core.importstorage.AbstractImportPage;

public class EmployeeBillingImportPage extends AbstractImportPage<EmployeeBillingImportForm>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeBillingImportPage.class);

  @SpringBean
  private EmployeeBillingImportService employeeBillingImportService;

  public EmployeeBillingImportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new EmployeeBillingImportForm(this);
    body.add(form);
    form.init();
    clear(); // reset state of the page, clear the import storage
  }

  List<AttrColumnDescription> getAttrColumnsInSheet()
  {
    return employeeBillingImportService.getAttrColumnsInSheet();
  }

  @Override
  protected void clear()
  {
    super.clear();
    form.setDateDropDownsEnabled(true);
  }

  boolean doImport(final Date dateToSelectAttrRow)
  {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      final Boolean success = doImportWithExcelExceptionHandling(() -> {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        setStorage(employeeBillingImportService.importData(is, clientFileName, dateToSelectAttrRow));
        return true;
      });
      return Boolean.TRUE.equals(success);
    }
    return false;
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    employeeBillingImportService.reconcile(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    employeeBillingImportService.commit(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected void selectAll(final String sheetName)
  {
    checkAccess();
    super.selectAll(sheetName);
  }

  @Override
  protected void select(final String sheetName, final int number)
  {
    checkAccess();
    super.select(sheetName, number);
  }

  @Override
  protected void deselectAll(final String sheetName)
  {
    checkAccess();
    super.deselectAll(sheetName);
  }

  private void checkAccess()
  {
    accessChecker.checkLoggedInUserRight(UserRightId.FIBU_EMPLOYEE, UserRightValue.READWRITE);
    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.import.title");
  }

}
