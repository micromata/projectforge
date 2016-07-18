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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.persistence.jpa.impl.LuceneServiceImpl;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class LuceneConsoleForm extends AbstractStandardForm<LuceneConsoleForm, LuceneConsolePage>
{
  private static final long serialVersionUID = 7999342246756382887L;

  private String fieldList;
  private String sql;
  private String entityIndexDescription = "";
  private String resultString = "";
  private LuceneServiceImpl luceneService;
  private Class<?> entityClass;

  public LuceneConsoleForm(final LuceneConsolePage parentPage, LuceneServiceImpl luceneService)
  {
    super(parentPage);
    this.luceneService = luceneService;
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    final DropDownChoice<Class<?>> entdd;
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("Classes");
      Set<Class<?>> lclass = luceneService.getSearchClasses();

      IChoiceRenderer<Class<?>> renderer = new IChoiceRenderer<Class<?>>()
      {
        @Override
        public Object getDisplayValue(Class<?> object)
        {
          return object.getSimpleName();
        }

        @Override
        public String getIdValue(Class<?> object, int index)
        {
          return object.getName();
        }
      };
      Map<String, Class<?>> smptoClass = new TreeMap<>();
      new ArrayList<>(lclass).stream().map((e) -> smptoClass.put(e.getSimpleName(), e)).collect(Collectors.toList());

      List<Class<?>> clsList = new ArrayList<>(smptoClass.values());

      if (clsList.isEmpty() == false) {
        entityClass = clsList.get(0);
      }
      entdd = new DropDownChoice<Class<?>>(
          fs.getDropDownChoiceId(), new PropertyModel<Class<?>>(this, "entityClass"),
          clsList, renderer);
      entdd.setNullValid(false).setRequired(true).setOutputMarkupId(true);

      fs.add(entdd);
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>())
      {
        @Override
        public final void onSubmit()
        {
          luceneService.reindex(entityClass);
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(fs.newChildId(), button, "reindex",
          SingleButtonPanel.DANGER);
      fs.add(buttonPanel);
    }

    final MaxLengthTextArea descr;
    {
      FieldsetPanel fs = gridBuilder.newFieldset("Description");

      descr = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(this, "entityIndexDescription"),
          10000);
      descr.setOutputMarkupId(true);
      descr.add(AttributeModifier.append("style", "width: 100%; height: 5em;"));
      fs.add(descr);
    }
    entdd.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(AjaxRequestTarget target)
      {
        entityIndexDescription = luceneService.getIndexDescription(entityClass);
        target.add(descr);
      }
    });
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("Lucene");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(this, "fieldList")));

      final MaxLengthTextArea sqlTextArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
          new PropertyModel<String>(this, "sql"), 10000);
      sqlTextArea.setOutputMarkupId(true);
      sqlTextArea.add(AttributeModifier.append("style", "width: 100%; height: 5em;"));
      fs.add(sqlTextArea);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("").suppressLabelForWarning();
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>())
      {
        @Override
        public final void onSubmit()
        {
          parentPage.excecuteLucene(true, entityClass, sql, fieldList);
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(fs.newChildId(), button, "executeLucene",
          SingleButtonPanel.DANGER);
      fs.add(buttonPanel);
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("").suppressLabelForWarning();
      final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>())
      {
        @Override
        public final void onSubmit()
        {
          parentPage.excecuteLucene(false, entityClass, sql, fieldList);
        }
      };
      final SingleButtonPanel buttonPanel = new SingleButtonPanel(fs.newChildId(), button, "executeHIbernate",
          SingleButtonPanel.DANGER);
      fs.add(buttonPanel);
    }
    gridBuilder.newGridPanel();
    final DivPanel section = gridBuilder.getPanel();
    final DivTextPanel resultPanel = new DivTextPanel(section.newChildId(), new Model<String>()
    {
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
