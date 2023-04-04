package org.projectforge.rest.poll

import org.projectforge.business.scripting.I18n
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.ui.*
import javax.annotation.PostConstruct

class PollCreation {

    fun createPollForm() : UILayout{

        val lc = LayoutContext(PollDO::class.java)
        val layout = UILayout(I18n.getString("poll.create"))
        /*layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
        */

        layout.add(
                UIRow()
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4))
                            .add(lc, "title", "description", "location", "owner", "deadline")
                    ))
    return layout
    }
}