package org.projectforge.rest

import com.google.gson.*
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.address.AddressbookDO
import org.projectforge.rest.json.JsonCreator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import java.lang.reflect.Type
import javax.ws.rs.Path

@Controller
@Path("addresses")
open class AddressRest() : AbstractDORest<AddressDO, AddressDao, AddressFilter>() {
    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

    companion object {
        init {
            JsonCreator.add(AddressbookDO::class.java, AddressbookDOSerializer())
        }
    }

    @Autowired
    open var addressDao: AddressDao? = null

    override fun getBaseDao(): AddressDao {
        return addressDao!!
    }

    override fun newBaseDO(): AddressDO {
        return AddressDO()
    }

    override fun getFilterClass(): Class<AddressFilter> {
        return AddressFilter::class.java
    }

    /**
     * Clone is supported by addresses.
     */
    override fun prepareClone(obj: AddressDO): Boolean {
        // TODO: Enter here the PersonalAddressDO stuff etc.
        return true
    }

    class AddressbookDOSerializer : JsonSerializer<org.projectforge.business.address.AddressbookDO> {
        @Synchronized
        override fun serialize(obj: org.projectforge.business.address.AddressbookDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
            if (obj == null) return null
            val result = JsonObject()
            result.add("id", JsonPrimitive(obj.id))
            result.add("title", JsonPrimitive(obj.title))
            return result
        }
    }
}