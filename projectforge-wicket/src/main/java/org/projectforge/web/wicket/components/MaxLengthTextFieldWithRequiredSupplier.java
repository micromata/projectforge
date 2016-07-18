package org.projectforge.web.wicket.components;

import org.apache.wicket.model.IModel;

import java.util.function.BooleanSupplier;

/**
 * This is a MaxLengthTextField where you can set a supplier whose result is returned in isRequired().
 */
public class MaxLengthTextFieldWithRequiredSupplier extends MaxLengthTextField
{
  private static final long serialVersionUID = 1L;

  private BooleanSupplier requiredSupplier;

  public MaxLengthTextFieldWithRequiredSupplier(String id, IModel<String> model)
  {
    super(id, model);
  }

  /**
   * Set a supplier whose result is returned in isRequired().
   */
  public void setRequiredSupplier(BooleanSupplier requiredSupplier)
  {
    this.requiredSupplier = requiredSupplier;
  }

  @Override
  public boolean isRequired()
  {
    return requiredSupplier != null && requiredSupplier.getAsBoolean();
  }
}
