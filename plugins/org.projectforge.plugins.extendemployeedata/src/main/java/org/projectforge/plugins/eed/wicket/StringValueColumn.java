package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.web.common.timeattr.AttrModel;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class StringValueColumn extends PropertyColumn<EmployeeGeneralValueDO,String>
{
  public StringValueColumn(IModel<String> displayModel, String propertyExpression)
  {
    super(displayModel, propertyExpression);
  }

  /**
   * Override this method if you want to have tool-tips.
   *
   * @return
   */
  public String getTooltip(final EmployeeGeneralValueDO object)
  {
    return null;
  }

  /**
   * Call CellItemListener. If a property model object is of type I18nEnum then the translation is automatically used.
   *
   * @see org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
   *      java.lang.String, org.apache.wicket.model.IModel)
   * @see CellItemListener#populateItem(Item, String, IModel)
   */
  @Override
  public void populateItem(final Item<ICellPopulator<EmployeeGeneralValueDO>> item, final String componentId, final IModel<EmployeeGeneralValueDO> rowModel)
  {
    PropertyModel attrModel = new PropertyModel<String>(rowModel.getObject(),
        "value");
    item.add(new InputPanel(componentId, new TextField<String>(InputPanel.WICKET_ID, attrModel)));
    /*if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }*/
  }
}
