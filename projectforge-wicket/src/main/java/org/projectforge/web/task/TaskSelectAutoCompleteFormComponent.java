/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.task;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Johannes Unterstein
 */
public abstract class TaskSelectAutoCompleteFormComponent extends PFAutoCompleteTextField<TaskDO> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(TaskSelectAutoCompleteFormComponent.class);

    private static final String[] SEARCH_FIELDS = {"title", "taskpath"};

    private static final long serialVersionUID = 2278347191215880396L;

    private TaskDO taskDo;

    private boolean autocompleteOnlyTaskBookableForTimesheets;

    /**
     * @param id
     */
    public TaskSelectAutoCompleteFormComponent(final String id) {
        super(id, null);
        setModel(new PropertyModel<TaskDO>(this, "taskDo"));
        getSettings().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false);
        add(AttributeModifier.append("onkeypress", "if ( event.which == 13 ) { return false; }"));
        add(AttributeModifier.append("class", "mm_delayBlur"));
        add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 3681828654557441560L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // just update the model
            }
        });
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        // this panel should always start with an empty input field, therefore delete the current model
        taskDo = null;
    }

    /**
     * @see org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField#getChoices(java.lang.String)
     */
    @Override
    protected List<TaskDO> getChoices(final String input) {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields(SEARCH_FIELDS);
        filter.setSearchString(input);
        final List<TaskDO> list = WicketSupport.getTaskDao().select(filter);
        final List<TaskDO> choices = new ArrayList<TaskDO>();
        for (final TaskDO task : list) {
            if (autocompleteOnlyTaskBookableForTimesheets == false) {
                choices.add(task);
            } else {
                final TaskNode taskNode = TaskTree.getInstance().getTaskNodeById(task.getId());
                if (taskNode == null) {
                    log.error("Oups, task node with id '" + task.getId() + "' not found in taskTree.");
                } else if (taskNode.isBookableForTimesheets() == true) {
                    // Only add nodes which are bookable:
                    choices.add(task);
                }
            }
        }
        return choices;
    }

    @Override
    protected String formatValue(final TaskDO value) {
        if (value == null) {
            return "";
        }
        return "" + value.getId();
    }

    @Override
    protected String formatLabel(final TaskDO value) {
        if (value == null) {
            return "";
        }

        return createPath(value.getId());
    }

    /**
     * create path to root
     *
     * @return
     */
    private String createPath(final Long taskId) {
        final StringBuilder builder = new StringBuilder();
        final List<TaskNode> nodeList = TaskTree.getInstance().getPathToRoot(taskId);
        if (CollectionUtils.isEmpty(nodeList) == true) {
            return getString("task.path.rootTask");
        }
        final String pipeSeparator = " | ";
        String separator = "";
        for (final TaskNode node : nodeList) {
            builder.append(separator);
            builder.append(node.getTask().getTitle());
            separator = pipeSeparator;
        }
        return builder.toString();
    }

    protected void notifyChildren() {
        final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target.isPresent()) {
            onModelSelected(target.get(), taskDo);
        }
    }

    /**
     * Hook method which is called when the model is changed with a valid during an ajax call
     *
     * @param target
     * @param taskDo
     */
    protected abstract void onModelSelected(final AjaxRequestTarget target, TaskDO taskDo);

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <C> IConverter<C> getConverter(final Class<C> type) {
        return new IConverter() {
            private static final long serialVersionUID = -7729322118285105516L;

            @Override
            public Object convertToObject(final String value, final Locale locale) {
                if (StringUtils.isEmpty(value) == true) {
                    getModel().setObject(null);
                    notifyChildren();
                    return null;
                }
                try {
                    final TaskDO task = TaskTree.getInstance().getTaskById(Long.valueOf(value));
                    if (task == null) {
                        error(getString("timesheet.error.invalidTaskId"));
                        return null;
                    }
                    getModel().setObject(task);
                    notifyChildren();
                    return task;
                } catch (final NumberFormatException e) {
                    // just ignore the NumberFormatException, because this could happen during wrong inputs
                    return null;
                }
            }

            @Override
            public String convertToString(final Object value, final Locale locale) {
                if (value == null) {
                    return "";
                }
                final TaskDO task = (TaskDO) value;
                return task.getTitle();
            }
        };
    }

    /**
     * @param autocompleteOnlyTaskBookableForTimesheets the autocompleteOnlyTaskBookableForTimesheets to set
     * @return this for chaining.
     */
    void setAutocompleteOnlyTaskBookableForTimesheets(final boolean autocompleteOnlyTaskBookableForTimesheets) {
        this.autocompleteOnlyTaskBookableForTimesheets = autocompleteOnlyTaskBookableForTimesheets;
    }
}
