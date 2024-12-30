/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.todo;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.TaskTreeHelper;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.FlatDisplayHistoryEntry;
import org.projectforge.framework.persistence.history.FlatHistoryFormatService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class ToDoDao extends BaseDao<ToDoDO> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ToDoDao.class);

    private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"reporter.username", "reporter.firstname",
            "reporter.lastname",
            "assignee.username", "assignee.firstname", "assignee.lastname", "task.title", "task.taskpath", "group.name"};

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlatHistoryFormatService flatHistoryFormatService;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private SendMail sendMail;

    @Autowired
    private ConfigurationService configurationService;

    private transient TaskTree taskTree;

    private final ToDoCache toDoCache = new ToDoCache(this);

    public ToDoDao() {
        super(ToDoDO.class);
        userRightId = TodoPluginUserRightId.PLUGIN_TODO;
    }

    @Override
    public String[] getAdditionalSearchFields() {
        return ADDITIONAL_SEARCH_FIELDS;
    }

    @Override
    public List<ToDoDO> select(final BaseSearchFilter filter) {
        final ToDoFilter myFilter;
        if (filter instanceof ToDoFilter) {
            myFilter = (ToDoFilter) filter;
        } else {
            myFilter = new ToDoFilter(filter);
        }
        final QueryFilter queryFilter = new QueryFilter(myFilter);
        final Collection<ToDoStatus> col = new ArrayList<>(5);
        final String searchString = myFilter.getSearchString();
        if (myFilter.isOnlyRecent()) {
            final PFUserDO assignee = new PFUserDO();
            assignee.setId(ThreadLocalUserContext.getLoggedInUserId());
            queryFilter.add(QueryFilter.eq("assignee", assignee));
            myFilter.setSearchString(""); // Delete search string for ignoring it.
            queryFilter.add(QueryFilter.eq("recent", true));
        } else {
            if (myFilter.isOpened()) {
                col.add(ToDoStatus.OPENED);
            }
            if (myFilter.isClosed()) {
                col.add(ToDoStatus.CLOSED);
            }
            if (myFilter.isPostponed()) {
                col.add(ToDoStatus.POSTPONED);
            }
            if (myFilter.isReopened()) {
                col.add(ToDoStatus.RE_OPENED);
            }
            if (myFilter.isInprogress()) {
                col.add(ToDoStatus.IN_PROGRESS);
            }
            if (col.size() > 0) {
                queryFilter.add(QueryFilter.isIn("status", col));
            }
            if (myFilter.getTaskId() != null) {
                final TaskNode node = getTaskTree().getTaskNodeById(myFilter.getTaskId());
                final List<Long> taskIds = node.getDescendantIds();
                taskIds.add(node.getId());
                queryFilter.add(QueryFilter.isIn("task.id", taskIds));
            }
            if (myFilter.getAssigneeId() != null) {
                final PFUserDO assignee = new PFUserDO();
                assignee.setId(myFilter.getAssigneeId());
                queryFilter.add(QueryFilter.eq("assignee", assignee));
            }
            if (myFilter.getReporterId() != null) {
                final PFUserDO reporter = new PFUserDO();
                reporter.setId(myFilter.getReporterId());
                queryFilter.add(QueryFilter.eq("reporter", reporter));
            }
        }
        queryFilter.addOrder(SortProperty.desc("created"));
        final List<ToDoDO> list = select(queryFilter);
        myFilter.setSearchString(searchString); // Restore search string.
        return list;
    }

    /**
     * Sends an e-mail to the project manager if exists and is not equals to the logged in user.
     */
    public void sendNotification(final ToDoDO todo, final String requestUrl) {
        if (!configurationService.isSendMailConfigured()) {
            // Can't send e-mail because no send mail is configured.
            return;
        }
        final Map<String, Object> data = new HashMap<>();
        data.put("todo", todo);
        data.put("requestUrl", requestUrl);
        final List<FlatDisplayHistoryEntry> history = flatHistoryFormatService.selectHistoryEntriesAndConvert(this, todo);
        final List<FlatDisplayHistoryEntry> list = new ArrayList<>();
        int i = 0;
        for (final FlatDisplayHistoryEntry entry : history) {
            list.add(entry);
            if (++i >= 10) {
                break;
            }
        }
        data.put("history", list);
        final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
        final Long userId = user.getId();
        final Long assigneeId = todo.getAssigneeId();
        final Long reporterId = todo.getReporterId();
        if (assigneeId != null && !userId.equals(assigneeId)) {
            sendNotification(todo.getAssignee(), todo, data, true);
        }
        if (reporterId != null && !userId.equals(reporterId) && !reporterId.equals(assigneeId)) {
            sendNotification(todo.getReporter(), todo, data, true);
        }
        if (userId != assigneeId && userId != reporterId && !hasUserSelectAccess(user, todo, false)) {
            // User is neither reporter nor assignee, so send e-mail (in the case the user has no read access anymore).
            sendNotification(ThreadLocalUserContext.getLoggedInUser(), todo, data, false);
        }
    }

    private void sendNotification(final PFUserDO recipient, final ToDoDO toDo, final Map<String, Object> data,
                                  final boolean checkAccess) {
        if (checkAccess && !hasUserSelectAccess(recipient, toDo, false)) {
            log.info("Recipient '"
                    + recipient.getFullname()
                    + "' (id="
                    + recipient.getId()
                    + ") of the notification has no select access to the todo entry: "
                    + toDo);
            return;
        }
        final Mail msg = new Mail();
        msg.setTo(recipient);
        final StringBuilder subject = new StringBuilder();
        final ToDoStatus status = toDo.getStatus();
        if (status != null && status != ToDoStatus.OPENED) {
            subject.append("[").append(I18nHelper.getLocalizedMessage(recipient, "plugins.todo.status")).append(": ")
                    .append(I18nHelper.getLocalizedMessage(recipient, status.getI18nKey())).append("] ");
        }
        subject.append(I18nHelper.getLocalizedMessage(recipient, "plugins.todo.todo")).append(": ");
        subject.append(toDo.getSubject());
        msg.setProjectForgeSubject(subject.toString());
        final String content = sendMail.renderGroovyTemplate(msg,
                "mail/todoChangeNotification.html",
                data,
                I18nHelper.getLocalizedMessage("plugins.todo.todo"),
                recipient);
        msg.setContent(content);
        msg.setContentType(Mail.CONTENTTYPE_HTML);
        sendMail.send(msg, null, null);
    }

    @Override
    public void onInsert(final ToDoDO obj) {
        if (!Objects.equals(ThreadLocalUserContext.getLoggedInUserId(), obj.getAssigneeId())) {
            // To-do is changed by other user than assignee, so set recent flag for this to-do for the assignee.
            obj.setRecent(true);
        }
    }

    @Override
    public void onUpdate(final ToDoDO obj, final ToDoDO dbObj) {
        if (!Objects.equals(ThreadLocalUserContext.getLoggedInUserId(), obj.getAssigneeId())) {
            // To-do is changed by other user than assignee, so set recent flag for this to-do for the assignee.
            final ToDoDO copyOfDBObj = new ToDoDO();
            copyOfDBObj.copyValuesFrom(dbObj, "deleted");
            if (copyOfDBObj.copyValuesFrom(obj, "deleted") == EntityCopyStatus.MAJOR) {
                // Modifications done:
                obj.setRecent(true);
            }
        }
    }

    @Override
    public void afterInsertOrModify(final ToDoDO obj, final OperationType operationType) {
        toDoCache.setExpired(); // Force reload of the menu item counters for open to-do entrie.
    }

    public void setAssignee(final ToDoDO todo, final Long userId) {
        final PFUserDO user = userDao.findOrLoad(userId);
        todo.setAssignee(user);
    }

    public void setReporter(final ToDoDO todo, final Long userId) {
        final PFUserDO user = userDao.findOrLoad(userId);
        todo.setReporter(user);
    }

    public void setTask(final ToDoDO todo, final Long taskId) {
        final TaskDO task = getTaskTree().getTaskById(taskId);
        todo.setTask(task);
    }

    public void setGroup(final ToDoDO todo, final Long groupId) {
        final GroupDO group = groupDao.findOrLoad(groupId);
        todo.setGroup(group);
    }

    /**
     * Get the number of open to-do entries for the given user. Entries are open (in this context) when they're not
     * deleted or closed. <br/>
     * The result is cached (therefore you can call this method very often).
     *
     * @param userId If null then the current logged in user is assumed.
     * @return Number of open to-do entries.
     */
    public int getOpenToDoEntries(Long userId) {
        if (userId == null) {
            userId = ThreadLocalUserContext.getLoggedInUserId();
        }
        return toDoCache.getOpenToDoEntries(userId);
    }

    /**
     * Called by ToDoCache to get the number of open entries for the given users.
     *
     * @param userId
     * @return Number of open to-do entries.
     */
    int internalGetOpenEntries(final Long userId) {
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            return jdbc.queryForObject("SELECT COUNT(*) FROM T_PLUGIN_TODO"
                    + " where assignee_fk="
                    + userId
                    + " and recent=true and deleted=false", Integer.class);
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            return 0;
        }
    }

    @Override
    public ToDoDO newInstance() {
        return new ToDoDO();
    }

    private TaskTree getTaskTree() {
        if (taskTree == null) {
            taskTree = TaskTreeHelper.getTaskTree();
        }
        return taskTree;
    }
}
