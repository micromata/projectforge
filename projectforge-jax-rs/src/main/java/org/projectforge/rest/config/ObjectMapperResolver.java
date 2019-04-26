package org.projectforge.rest.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.util.Date;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
  private final ObjectMapper mapper;

  public ObjectMapperResolver() {
    mapper = new ObjectMapper();
    //mapper.enable(SerializationFeature.INDENT_OUTPUT);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Date.class, new MyDateDeserializer());
    mapper.registerModule(module);
    // Deserialization should fail on unknown properties due to stronger validation checks of the rest api.
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }
}
