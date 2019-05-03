package org.projectforge.rest.task

import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.rest.dto.Task
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/task")
class TaskRest()
    : AbstractStandardRest<TaskDO, Task, TaskDao, TaskFilter>(TaskDao::class.java, TaskFilter::class.java, "task.title") {
    init {
        useDTO = true
    }

    @Autowired
    private lateinit var taskDao: AddressbookDao

    override fun transformDO(obj: TaskDO): Task {
        val task = Task()
        task.copyFrom(obj)
        return task
    }

    override fun transformDTO(dto: Task): TaskDO {
        val taskDO = TaskDO()
        dto.copyTo(taskDO)
        return taskDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TaskDO) {
        /* if (StringUtils.isAllBlank(obj.name, obj.firstName, obj.organization)) {
             validationErrors.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
         }*/
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: TaskDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "parentTask", "title", "status", "priority", "responsibleUser", "shortDescription", "reference", "description")
        layout.add(UIRow().add(UICol().add(UIInput("protectTimesheetsUntil", lc, dataType = UIDataType.DATE))))

        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
