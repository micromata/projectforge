package org.projectforge.web.rest;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestUserFilter.class);

  @Override
  public Response toResponse(UnrecognizedPropertyException exception) {
    log.error("Bad request: " + exception.getMessage());
    return Response
            .status(Response.Status.BAD_REQUEST)
            .entity("'" + exception.getPropertyName() + "' is an unrecognized field.")
            .type(MediaType.TEXT_PLAIN)
            .build();
  }
}
