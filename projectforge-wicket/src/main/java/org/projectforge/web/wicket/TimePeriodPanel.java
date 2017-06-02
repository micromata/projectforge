package org.projectforge.web.wicket;

import java.util.Date;

import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

// TODO CT: use FormComponentPanel?
public class TimePeriodPanel extends Panel
{
  private final DatePanel startDate;

  private final DatePanel endDate;

  public TimePeriodPanel(final String id, final IModel<Date> startDateModel, final IModel<Date> endDateModel, final AbstractListPage<?, ?, ?> caller)
  {
    super(id);

    startDate = new DatePanel("startDate", startDateModel, DatePanelSettings.get().withSelectPeriodMode(true));
    add(startDate);

    endDate = new DatePanel("endDate", endDateModel, DatePanelSettings.get().withSelectPeriodMode(true));
    add(endDate);

    // clear button
    final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID)
    {
      @Override
      public void onSubmit()
      {
        startDateModel.setObject(null);
        endDateModel.setObject(null);
        startDate.markModelAsChanged();
        endDate.markModelAsChanged();
        caller.refresh();
      }
    };
    unselectPeriodLink.setDefaultFormProcessing(false);

    final IconLinkPanel unselectLinkPanel = new IconLinkPanel("unselectLink", IconType.REMOVE_SIGN,
        new ResourceModel("calendar.tooltip.unselectPeriod"), unselectPeriodLink);
    unselectLinkPanel.setColor(CSSColor.RED);
    add(unselectLinkPanel);

    // quick select buttons
    final QuickSelectPanel quickSelectPanel = new QuickSelectPanel("quickSelect", caller, "quickSelect", startDate);
    add(quickSelectPanel);
    quickSelectPanel.init();

    // calendar week
    add(new DivTextPanel("calendarWeek",
        LambdaModel.of(() -> WicketUtils.getCalendarWeeks(this, startDateModel.getObject(), endDateModel.getObject()))
    ));

    // html comment
    add(new HtmlCommentPanel("htmlComment",
        LambdaModel.of(() -> WicketUtils.getUTCDates(startDateModel.getObject(), endDateModel.getObject()))
    ));
  }

  public DatePanel getStartDatePanel()
  {
    return startDate;
  }

  public DatePanel getEndDatePanel()
  {
    return endDate;
  }

}
