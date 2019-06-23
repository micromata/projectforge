package org.projectforge.business.task

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import org.projectforge.favorites.AbstractFavorite

class TaskFavorite(
        name: String = "",
        id: Int = 0,
        @XStreamAsAttribute
        var taskId: Int? = null)
    : AbstractFavorite(name, id)
