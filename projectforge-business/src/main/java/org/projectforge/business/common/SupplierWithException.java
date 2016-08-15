package org.projectforge.business.common;

@FunctionalInterface
public interface SupplierWithException<T, E extends Exception>
{
  public T get() throws E;
}
