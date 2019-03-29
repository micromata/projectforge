package org.projectforge.menu.builder

import org.projectforge.framework.persistence.user.entities.PFUserDO

class MenuCreatorContext(val user: PFUserDO,
                         val translate : Boolean = true)