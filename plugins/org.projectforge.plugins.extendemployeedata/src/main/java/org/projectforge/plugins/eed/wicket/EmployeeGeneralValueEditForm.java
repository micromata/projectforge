package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class EmployeeGeneralValueEditForm extends AbstractEditForm<EmployeeGeneralValueDO, EmployeeGeneralValueEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeGeneralValueEditForm.class);

  public EmployeeGeneralValueEditForm(EmployeeGeneralValueEditPage parentPage,
      EmployeeGeneralValueDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected void init()
  {
    super.init();
    final FieldsetPanel fs = gridBuilder.newFieldset(gridBuilder.getString("key"));
    final RequiredMaxLengthTextField key = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(data,
            "key"));
    key.setMarkupId("key").setOutputMarkupId(true);
    key.add(new IValidator<String>()
    {
      @Override public void validate(IValidatable<String> iValidatable)
      {
        final boolean[] isAlreadThere = { false };
        getParentPage().getBaseDao().internalGetList(new QueryFilter()).forEach(e -> {
          if (e.getKey().equals(iValidatable.getValue()))
          {
            isAlreadThere[0] = true;
          }
        });
        if(isAlreadThere[0] == true)
        {
          error(getString("plugins.eed.config.unique.error"));
          return;
        }
      }
    });
    WicketUtils.setStrong(key);
    fs.add(key);

    final FieldsetPanel fs2 = gridBuilder.newFieldset(gridBuilder.getString("value"));
    final RequiredMaxLengthTextField value = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(data,
            "value"));
    key.setMarkupId("value").setOutputMarkupId(true);
    WicketUtils.setStrong(value);
    fs2.add(value);

  }
}
