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

package org.projectforge.web.wicket.bootstrap;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.props.PropUtils;
import org.projectforge.web.wicket.flowlayout.AbstractGridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FormHeadingPanel;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class GridBuilder extends AbstractGridBuilder<FieldsetPanel>
{
  private static final long serialVersionUID = 4323077384391963834L;

  public static final int MAX_LEVEL = 2;

  private final DivPanel mainContainer;

  private final DivPanel[] rowPanel = new DivPanel[MAX_LEVEL + 1];

  private final DivPanel[] gridPanel = new DivPanel[MAX_LEVEL + 1];

  private int currentLevel = 0;

  private int splitDepth = 1;

  // Counts the length of grid panels of current row. After reaching full length, a new row will be created automatically.
  private final int lengthCounter[] = new int[MAX_LEVEL + 1];

  private Set<String> rowsPanelHelperSet;

  public GridBuilder(final MarkupContainer parent, final String id)
  {
    this(parent, id, true);
  }

  public DivPanel getMainContainer()
  {
    return mainContainer;
  }

  /**
   * @param parent
   * @param id
   * @param fluid Default is true.
   */
  public GridBuilder(final MarkupContainer parent, final String id, final boolean fluid)
  {
    super();
    this.parent = parent;
    mainContainer = new DivPanel(id, fluid == true ? GridType.CONTAINER_FLUID : GridType.CONTAINER);
    if (parent != null) {
      parent.add(mainContainer);
    }
  }

  public GridBuilder newGridPanel(final GridType... gridTypes)
  {
    return newGridPanel(0, GridSize.SPAN12, gridTypes);
  }

  public GridBuilder newSplitPanel(final GridSize size, final GridType... gridTypes)
  {
    return newSplitPanel(size, false, gridTypes);
  }

  public GridBuilder newSplitPanel(final GridSize size, final boolean hasSubSplitPanel, GridType... gridTypes)
  {
    if (hasSubSplitPanel == true) {
      splitDepth = 2;
      if (gridTypes == null) {
        gridTypes = new GridType[] { GridType.HAS_CHILDS};
      } else {
        final GridType[] types = new GridType[gridTypes.length + 1];
        for (int i = 0; i < gridTypes.length; i++) {
          types[i] = gridTypes[i];
        }
        types[gridTypes.length] = GridType.HAS_CHILDS;
        gridTypes = types;
      }
    } else {
      splitDepth = 1;
    }
    newGridPanel(0, size, gridTypes);
    if (hasSubSplitPanel == true) {
      // Set the class attribute "row-has-childs":
      if (rowsPanelHelperSet == null) {
        rowsPanelHelperSet = new HashSet<String>();
        rowPanel[0].addCssClasses(GridType.ROW_HAS_CHILDS);
        rowsPanelHelperSet.add(rowPanel[0].getMarkupId());
      } else {
        if (rowsPanelHelperSet.contains(rowPanel[0].getMarkupId()) == false) {
          rowPanel[0].addCssClasses(GridType.ROW_HAS_CHILDS);
          rowsPanelHelperSet.add(rowPanel[0].getMarkupId());
        }
      }
    }
    return this;
  }

  public GridBuilder newSubSplitPanel(final GridSize size, final GridType... gridTypes)
  {
    if (splitDepth < 2) {
      throw new IllegalArgumentException("Dear developer: please call gridBuilder.newSplitPanel(GridSize, true, ...) first!");
    }
    return newGridPanel(1, size, gridTypes);
  }

  /**
   * Sets the gridPanel of the given level as current level. You can only set a lower level than current level, otherwise an exception will
   * be thrown.
   * @param level the currentLevel to set
   * @return this for chaining.
   */
  public GridBuilder setCurrentLevel(final int level)
  {
    if (level > this.currentLevel) {
      throw new IllegalArgumentException("You can only set a lower level than current level, current level is "
          + this.currentLevel
          + ", desired level is "
          + level);
    }
    if (level < 0) {
      throw new IllegalArgumentException("Level must be a positive value: " + level);
    }
    this.currentLevel = level;
    setNullPanel(level, level + 1);
    return this;
  }

  public GridBuilder clear()
  {
    lengthCounter[currentLevel] = 1000;
    return this;
  }

  /**
   * If row panel of this level doesn't exist it will be created.
   * @param level
   * @param size
   * @param gridTypes
   * @return this for chaining.
   */
  private GridBuilder newGridPanel(final int level, final GridSize size, final GridType... gridTypes)
  {
    validateGridPanelLevel(level);
    currentLevel = level;
    if (rowPanel[level] == null) {
      newRowPanel(level);
    }
    boolean firstPanelOfRow = false;
    if (lengthCounter[level] == 0) {
      firstPanelOfRow = true;
    }
    lengthCounter[level] += size.getLength();
    if (lengthCounter[level] > 12) {
      newRowPanel(level);
      lengthCounter[level] = size.getLength();
      firstPanelOfRow = true;
    } else {
      if (firstPanelOfRow == false && gridPanel[level] != null) {
        gridPanel[level].addCssClasses(GridType.HAS_SIBLINGS);
      }
    }
    final DivPanel divPanel = new DivPanel(rowPanel[level].newChildId(), size, gridTypes);
    if (firstPanelOfRow == false) {
      divPanel.addCssClasses(GridType.NOT_FIRST);
    } else {
      divPanel.addCssClasses(GridType.FIRST);
    }
    return addGridPanel(level, divPanel);
  }

  /**
   * If you use this method, the lengthCounter won't be incremented and you use to call setCurrentLevel(level -1) manually.
   * @param level
   * @param divPanel
   * @return
   */
  private GridBuilder addGridPanel(final int level, final DivPanel divPanel)
  {
    validateGridPanelLevel(level);
    currentLevel = level;
    if (rowPanel[level] == null) {
      newRowPanel(level);
    }
    gridPanel[level] = divPanel;
    rowPanel[level].add(divPanel);
    setNullPanel(level + 1, level + 1);
    return this;
  }

  private String newRowPanelId(final int level)
  {
    validateRowPanelLevel(level);
    if (level > 0) {
      return gridPanel[level - 1].newChildId();
    } else {
      return mainContainer.newChildId();
    }
  }

  private GridBuilder newRowPanel(final int level, final GridType... gridTypes)
  {
    validateRowPanelLevel(level);
    final DivPanel rowPanel = new DivPanel(newRowPanelId(level), GridType.ROW);
    rowPanel.addCssClasses(gridTypes);
    return addRowPanel(level, rowPanel);
  }

  private GridBuilder addRowPanel(final int level, final DivPanel rowPanel)
  {
    validateRowPanelLevel(level);
    this.rowPanel[level] = rowPanel;
    lengthCounter[level] = 0;
    if (level > 0) {
      gridPanel[level - 1].add(rowPanel);
    } else {
      mainContainer.add(rowPanel);
    }
    setNullPanel(level + 1, level);
    return this;
  }

  /**
   * @return new child id (Wicket id) of the current grid panel.
   */
  public String newRowId()
  {
    return rowPanel[currentLevel].newChildId();
  }

  /**
   * @return new child id (Wicket id) of the current row panel.
   */
  public String newGridPanelId()
  {
    return gridPanel[currentLevel].newChildId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#getPanel()
   */
  public DivPanel getPanel()
  {
    if (currentLevel == 0 && gridPanel[currentLevel] == null) {
      newGridPanel(0, GridSize.SPAN12);
    }
    return gridPanel[currentLevel];
  }

  public DivPanel getRowPanel()
  {
    return rowPanel[currentLevel];
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#newFormHeading(java.lang.String)
   */
  public FormHeadingPanel newFormHeading(final String label)
  {
    final FormHeadingPanel formHeading = new FormHeadingPanel(getPanel().newChildId(), label);
    getPanel().add(formHeading);
    return formHeading;
  }

  @SuppressWarnings("serial")
  public DivTextPanel newSecurityAdviceBox(final IModel<String> content)
  {
    final DivTextPanel hintBox = new DivTextPanel(getPanel().newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return "<h4>" + getString("securityAdvice") + "</h4>" + HtmlHelper.escapeHtml(content.getObject(), true);
      }
    });
    hintBox.getDiv().add(AttributeModifier.append("class", "alert alert-danger"));
    hintBox.getLabel().setEscapeModelStrings(false);
    getPanel().add(hintBox);
    return hintBox;
  }

  public RepeatingView newRepeatingView()
  {
    final RepeatingView repeater = new RepeatingView(getPanel().newChildId());
    getPanel().add(repeater);
    return repeater;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractGridBuilder#newFieldset(org.projectforge.web.wicket.flowlayout.FieldProperties)
   */
  @Override
  public FieldsetPanel newFieldset(final FieldProperties< ? > fieldProperties)
  {
    return new FieldsetPanel(getPanel(), fieldProperties);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#newFieldset(java.lang.String)
   */
  @Override
  public FieldsetPanel newFieldset(final Class< ? > clazz, final String property)
  {
    return new FieldsetPanel(getPanel(), getString(PropUtils.getI18nKey(clazz, property)));
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#newFieldset(java.lang.String)
   */
  @Override
  public FieldsetPanel newFieldset(final String label)
  {
    return new FieldsetPanel(getPanel(), label);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#newFieldset(java.lang.String, java.lang.String)
   */
  @Override
  public FieldsetPanel newFieldset(final String labelText, final String labelDescription)
  {
    return new FieldsetPanel(getPanel(), labelText, labelDescription);
  }

  private void validateRowPanelLevel(final int level)
  {
    if (level < 0 || level > MAX_LEVEL) {
      throw new IllegalArgumentException("Level '" + "' not supported. Value must be between 1 and " + MAX_LEVEL);
    }
    if (level > 0 && gridPanel[level - 1] == null) {
      throw new IllegalArgumentException("Can't add row panel of level '"
          + level
          + "'. Grid panel of level "
          + (level - 1)
          + " doesn't exist!");
    }
  }

  private void validateGridPanelLevel(final int level)
  {
    if (level < 0 || level > MAX_LEVEL) {
      throw new IllegalArgumentException("Level '" + level + "' not supported. Value must be between 0 and " + MAX_LEVEL);
    }
  }

  private void setNullPanel(final int rowFromLevel, final int gridFromLevel)
  {
    for (int i = rowFromLevel; i <= MAX_LEVEL; i++) {
      rowPanel[i] = null;
      lengthCounter[i] = 0;
    }
    for (int i = gridFromLevel; i <= MAX_LEVEL; i++) {
      gridPanel[i] = null;
    }
  }
}
