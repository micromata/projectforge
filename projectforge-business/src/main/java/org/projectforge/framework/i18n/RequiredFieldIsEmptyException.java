package org.projectforge.framework.i18n;

public class RequiredFieldIsEmptyException extends UserException
{
  public RequiredFieldIsEmptyException(final String i18nKeyOfMissingField)
  {
    super("validation.error.fieldRequired", new MessageParam(i18nKeyOfMissingField, MessageParamType.I18N_KEY));
  }
}
