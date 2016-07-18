/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.orga;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.orga.PostType;
import org.projectforge.business.orga.PostausgangDO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class PostausgangEditForm extends AbstractEditForm<PostausgangDO, PostausgangEditPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PostausgangEditForm.class);

  public PostausgangEditForm(final PostausgangEditPage parentPage, final PostausgangDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Date
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("date"));
      final DatePanel datumPanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "datum"), DatePanelSettings.get()
          .withTargetType(java.sql.Date.class).withSelectProperty("datum"));
      datumPanel.setRequired(true);
      datumPanel.add(new AbstractValidator<Date>() {
        @Override
        protected void onValidate(final IValidatable<Date> validatable)
        {
          final Date value = validatable.getValue();
          if (value == null) {
            return;
          }
          final DayHolder today = new DayHolder();
          final DayHolder date = new DayHolder(value);
          if (today.before(date) == true) { // No dates in the future accepted.
            validatable.error(new ValidationError().addMessageKey("error.dateInFuture"));
          }
        }
      });
      fs.add(datumPanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Status drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.post.type"));
      final LabelValueChoiceRenderer<PostType> typeChoiceRenderer = new LabelValueChoiceRenderer<PostType>(fs, PostType.values());
      final DropDownChoice<PostType> typeChoice = new DropDownChoice<PostType>(fs.getDropDownChoiceId(), new PropertyModel<PostType>(data,
          "type"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
      typeChoice.setNullValid(false);
      typeChoice.setRequired(true);
      fs.add(typeChoice);
    }
    gridBuilder.newGridPanel();
    {
      // Receiver
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.postausgang.empfaenger"));
      final PFAutoCompleteMaxLengthTextField empfaengerTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "empfaenger")) {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("empfaenger", input);
        }
      };
      empfaengerTextField.withMatchContains(true).withMinChars(2).withFocus(true);
      empfaengerTextField.setRequired(true);
      WicketUtils.setStrong(empfaengerTextField);
      fs.add(empfaengerTextField);
    }
    {
      // Person
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.postausgang.person"));
      final PFAutoCompleteMaxLengthTextField personTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "person")) {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("person", input);
        }
      };
      personTextField.withMatchContains(true).withMinChars(2);
      fs.add(personTextField);
    }
    {
      // Content
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.post.inhalt"));
      final PFAutoCompleteMaxLengthTextField inhaltTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "inhalt")) {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("inhalt", input);
        }
      };
      inhaltTextField.withMatchContains(true).withMinChars(2);
      inhaltTextField.setRequired(true);
      fs.add(inhaltTextField);
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "bemerkung")), true);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
