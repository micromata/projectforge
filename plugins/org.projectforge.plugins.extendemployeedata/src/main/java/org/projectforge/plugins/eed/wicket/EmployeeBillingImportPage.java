package org.projectforge.plugins.eed.wicket;

import java.io.InputStream;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.excel.ExcelImportException;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.plugins.eed.EmployeeBillingImportDao;
import org.projectforge.web.core.importstorage.AbstractImportPage;

public class EmployeeBillingImportPage extends AbstractImportPage<EmployeeBillingImportForm>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeBillingImportPage.class);

  @SpringBean
  private EmployeeBillingImportDao employeeBillingImportDao;

  public EmployeeBillingImportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new EmployeeBillingImportForm(this);
    body.add(form);
    form.init();
  }

  void importAccountList()
  {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      try {
        final InputStream is = fileUpload.getInputStream();
        final String clientFileName = fileUpload.getClientFileName();
        setStorage(employeeBillingImportDao.importData(is, clientFileName));
      } catch (final Exception ex) {
        if (ex instanceof ExcelImportException) {
          error(translateParams((ExcelImportException) ex));
        }
        log.error(ex.getMessage(), ex);
        error("An error occurred (see log files for details): " + ex.getMessage());
        clear();
      }
    }
  }

  private String translateParams(ExcelImportException ex)
  {
    // TODO CT
    return getString("finance.datev.error1") + " " + ex.getRow() + " " +
        getString("finance.datev.error2") + " \"" + ex.getColumnname() + "\"";
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    employeeBillingImportDao.reconcile(getStorage(), sheetName);
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    employeeBillingImportDao.commit(getStorage(), sheetName);
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

  //  protected void showBusinessAssessment(final String sheetName)
  //  {
  //    final ImportedSheet<?> sheet = getStorage().getNamedSheet(sheetName);
  //    Validate.notNull(sheet);
  //    final List<BuchungssatzDO> list = new ArrayList<BuchungssatzDO>();
  //    for (final ImportedElement<?> element : sheet.getElements()) {
  //      final BuchungssatzDO satz = (BuchungssatzDO) element.getValue();
  //      list.add(satz);
  //    }
  //    final BusinessAssessment businessAssessment = new BusinessAssessment(
  //        AccountingConfig.getInstance().getBusinessAssessmentConfig(),
  //        (Integer) sheet.getProperty("year"), (Integer) sheet.getProperty("month"));
  //    form.setBusinessAssessment(businessAssessment);
  //    businessAssessment.setAccountRecords(list);
  //  }

  private void checkAccess()
  {
    // TODO CT
    //    accessChecker.checkLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE);
    //    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    // TODO CT
    return getString("fibu.datev.import");
  }
}
