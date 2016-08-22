package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.SingleButtonPanel;

public class EmployeeGeneralValueConfigurationForm
    extends AbstractEditForm<EmployeeGeneralValueDO, EmployeeGeneralValueConfigurationPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(EmployeeGeneralValueConfigurationForm.class);

  public EmployeeGeneralValueConfigurationForm(EmployeeGeneralValueConfigurationPage parentPage,
      EmployeeGeneralValueDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}