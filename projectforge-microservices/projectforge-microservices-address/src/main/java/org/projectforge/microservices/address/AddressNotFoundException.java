package org.projectforge.microservices.address;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Allow the controller to return a 404 if an address is not found by simply throwing this exception.
 * The @ResponseStatus causes Spring MVC to return a 404 instead of the usual 500.
 * 
 * @author Florian Blumenstein
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AddressNotFoundException extends RuntimeException
{

  private static final long serialVersionUID = 1L;

  public AddressNotFoundException(String addressNumber)
  {
    super("No such address: " + addressNumber);
  }
}
