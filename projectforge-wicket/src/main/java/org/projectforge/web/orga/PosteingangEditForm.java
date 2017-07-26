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
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.orga.PostType;
import org.projectforge.business.orga.PosteingangDO;
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

public class PosteingangEditForm extends AbstractEditForm<PosteingangDO, PosteingangEditPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PosteingangEditForm.class);

  public PosteingangEditForm(final PosteingangEditPage parentPage, final PosteingangDO data)
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
      datumPanel.add((IValidator<Date>) validatable -> {
        final Date value = validatable.getValue();

        final DayHolder today = new DayHolder();
        final DayHolder date = new DayHolder(value);
        if (today.before(date) == true) { // No dates in the future accepted.
          error(new ValidationError().addKey("error.dateInFuture"));
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
      // Sender
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.posteingang.absender"));
      final PFAutoCompleteMaxLengthTextField absenderTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "absender"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return getBaseDao().getAutocompletion("absender", input);
        }
      };
      absenderTextField.withMatchContains(true).withMinChars(2).withFocus(true);
      absenderTextField.setRequired(true);
      WicketUtils.setStrong(absenderTextField);
      fs.add(absenderTextField);
    }
    {
      // Person
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("orga.posteingang.person"));
      final PFAutoCompleteMaxLengthTextField personTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "person"))
      {
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
          new PropertyModel<String>(data, "inhalt"))
      {
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
      // Content
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
