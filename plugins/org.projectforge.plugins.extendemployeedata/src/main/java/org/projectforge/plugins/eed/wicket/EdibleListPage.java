package org.projectforge.plugins.eed.wicket;

import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.IListPage;

public class EdibleListPage extends AbstractStandardFormPage
    implements ISelectCallerPage, IListPage<EmployeeDO, EmployeeDao>
{
  private static final long serialVersionUID = -7157440416517271655L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EdibleListPage.class);

  @SpringBean
  private transient EmployeeDao employeeDao;

  private final EdibleListForm form;

  public EdibleListPage(final PageParameters parameters)
  {
    super(parameters);
    form = new EdibleListForm(this);
    body.add(form);
    form.init();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.title.listcare");
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

  @Override
  public List<EmployeeDO> getList()
  {
    return employeeDao.internalLoadAll();
  }

  @Override
  public EmployeeDao getBaseDao()
  {
    return employeeDao;
  }

}
