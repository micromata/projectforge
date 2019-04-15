package org.projectforge.rest.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectMapperResolver.class);

  private final ObjectMapper mapper;

  public ObjectMapperResolver() {
    mapper = new ObjectMapper();
    //mapper.enable(SerializationFeature.INDENT_OUTPUT);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Date.class, new MyDateDeserializer());
    mapper.registerModule(module);
    // Deserialization should fail on unknown properties due to stronger validation checks of the rest api.
    // mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }


  public class MyDateDeserializer extends JsonDeserializer<Date> {
    // 2003-01-25 08:39:30.000
    String[][] patterns = new String[][]{
            {"yyyy-MM-dd HH:mm:ss.SSS", "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}"},
            {"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z"}
    };

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      String date = jsonParser.getText();
      if (StringUtils.isBlank(date)) {
        return null;
      }
      try {
        for (String[] pattern : patterns) {
          if (date.matches(pattern[1])) {
            return new SimpleDateFormat(pattern[0]).parse(date);
          }
        }
        log.error("Unsupported date format: " + date);
        return null;
      } catch (ParseException ex) {
        log.error(ex.getMessage(), ex);
        return null;
      }
    }
  }
}