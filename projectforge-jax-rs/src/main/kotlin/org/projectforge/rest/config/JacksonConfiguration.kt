package org.projectforge.rest.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.rest.calendar.TeamCalDOSerializer
import org.projectforge.rest.json.*
import org.projectforge.ui.UIMultiSelect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate

@Configuration
open class JacksonConfiguration {

    @Bean
    open fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        val problemHandler = MyDeserializationProblemHandler()
        mapper.addHandler(problemHandler)
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        //mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        //mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        val module = SimpleModule()
        module.addSerializer(LocalDate::class.java, LocalDateSerializer())
        module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())

        module.addSerializer(java.util.Date::class.java, UtilDateSerializer())
        module.addDeserializer(java.util.Date::class.java, UtilDateDeserializer())

        module.addSerializer(java.sql.Date::class.java, SqlDateSerializer())
        module.addDeserializer(java.sql.Date::class.java, SqlDateDeserializer())

        module.addSerializer(Kost2DO::class.java, Kost2DOSerializer())
        module.addSerializer(PFUserDO::class.java, PFUserDOSerializer())
        module.addSerializer(TaskDO::class.java, TaskDOSerializer())
        module.addSerializer(TenantDO::class.java, TenantDOSerializer())

        // Calendar serializers
        module.addSerializer(TeamCalDO::class.java, TeamCalDOSerializer())

        // UI
        module.addSerializer(UIMultiSelect::class.java, UIMultiSelectTypeSerializer())

        mapper.registerModule(module);
        return mapper;
    }
}
