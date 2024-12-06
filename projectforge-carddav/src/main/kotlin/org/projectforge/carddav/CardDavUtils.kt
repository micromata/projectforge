package org.projectforge.carddav

import org.projectforge.carddav.model.Contact
import java.util.*

internal object CardDavUtils {
    fun getETag(contact: Contact): String {
        val lastUpdated = contact.lastUpdated ?: Date()
        return lastUpdated.time.toString()
    }
}
