package org.projectforge.framework.persistence.history;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;

/**
 * Convert to string for search.
 * 
 * TODO RK will not be loaded, althoug service is registered.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class ProjectForgeDefaultFieldBridge implements BridgeProvider
{
  private static FieldBridge defaultStringBridge = new ToStringFieldBridge();

  @Override
  public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext)
  {

    Class<?> basicClasses[] = {
        //        BigDecimal.class,
        //        Integer.class
    };
    for (Class<?> cls : basicClasses) {
      if (cls.isAssignableFrom(bridgeProviderContext.getReturnType()) == true) {
        return defaultStringBridge;
      }
    }

    return null;
  }

}
