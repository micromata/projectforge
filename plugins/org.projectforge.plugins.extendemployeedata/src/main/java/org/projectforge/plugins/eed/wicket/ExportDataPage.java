package org.projectforge.plugins.eed.wicket;

import java.util.Date;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class ExportDataPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportDataPage.class);

  @SpringBean
  private transient TimesheetDao timesheetDao;

  private final ExportDataForm form;

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
    return getString("plugins.eed.title");
  }

  @Override
  public void select(String property, Object selectedValue)
  {
    if (property.startsWith("quickSelect.") == true) {
      final Date date = (Date) selectedValue;

      form.getTimePeriod().setFromDate(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      }
      form.getTimePeriod().setToDate(dateHolder.getDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
    }
  }

  @Override
  public void unselect(String property)
  {

  }

  @Override
  public void cancelSelection(String property)
  {

  }

}
