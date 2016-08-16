package org.projectforge.plugins.eed.wicket;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.plugins.eed.service.LBExporterService;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

public class ExportDataPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportDataPage.class);

  private final ExportDataForm form;

  @SpringBean
  private EmployeeDao employeeDao;

  @SpringBean
  private LBExporterService exporterService;

  public ExportDataPage(final PageParameters parameters)
  {
    super(parameters);
    form = new ExportDataForm(this);
    body.add(form);
    form.init();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.export.title");
  }

  @Override
  public void select(String property, Object selectedValue)
  {

  }

  @Override
  public void unselect(String property)
  {

  }

  @Override
  public void cancelSelection(String property)
  {

  }

  public void exportData()
  {
    log.info("Export data for LB");
    List<EmployeeDO> employeeList = employeeDao.internalLoadAll();
    final String filename = "Liste-PF-"
        + form.getSelectedMonth() + "-" + form.getSelectedYear()
        + ".xls";
    Calendar cal = new GregorianCalendar(form.getSelectedYear(), form.getSelectedMonth() - 1, 1);
    byte[] xls = exporterService.getExcel(employeeList, cal);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

}
