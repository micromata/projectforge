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

package org.projectforge.web.admin;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class SqlConsoleForm extends AbstractStandardForm<SqlConsoleForm, SqlConsolePage>
{
  private static final long serialVersionUID = 7999342246756382887L;

  private String sql;

  private String resultString = "";

  public SqlConsoleForm(final SqlConsolePage parentPage)
  {
    super(parentPage);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("SQL");
      final MaxLengthTextArea sqlTextArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(this, "sql"), 10000);
      sqlTextArea.add(AttributeModifier.append("style", "width: 100%; height: 5em;"));
      fs.add(sqlTextArea);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("").suppressLabelForWarning();
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>()) {
        @Override
        public final void onSubmit()
        {
          parentPage.excecute(sql);
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(fs.newChildId(), button, "execute", SingleButtonPanel.DANGER);
      fs.add(buttonPanel);
    }
    gridBuilder.newGridPanel();
    final DivPanel section = gridBuilder.getPanel();
    final DivTextPanel resultPanel = new DivTextPanel(section.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return resultString;
      }
    });
    resultPanel.getLabel().setEscapeModelStrings(false);
    section.add(resultPanel);
  }

  void setResultString(final String resultString)
  {
    this.resultString = resultString;
  }
}
