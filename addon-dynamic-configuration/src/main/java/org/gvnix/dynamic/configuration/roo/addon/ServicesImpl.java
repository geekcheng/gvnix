/*
 * gvNIX. Spring Roo based RAD tool for Conselleria d'Infraestructures
 * i Transport - Generalitat Valenciana
 * Copyright (C) 2010 CIT - Generalitat Valenciana
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gvnix.dynamic.configuration.roo.addon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.gvnix.dynamic.configuration.roo.addon.config.DefaultDynamicConfiguration;
import org.gvnix.dynamic.configuration.roo.addon.config.DynamicConfiguration;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynComponent;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynConfiguration;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynPropertyList;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Manage components of dynamic configurations.
 * 
 * @author Mario Martínez Sánchez ( mmartinez at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component
@Service
@Reference(name="components", strategy=ReferenceStrategy.LOOKUP, policy=ReferencePolicy.DYNAMIC, referenceInterface=DefaultDynamicConfiguration.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class ServicesImpl implements Services {
  
  private static final Logger logger = HandlerUtils.getLogger(ServicesImpl.class);

  private ComponentContext context;
  
  protected void activate(ComponentContext context) {
    this.context = context;
  }
  
  /**
   * Get all the dynamic configuration components.
   * 
   * @return Dynamic configuration components
   */
  private Set<Object> getComponents() {
    
    return getSet("components");
  }
  
  /**
   * Get named configuration components.
   * 
   * @param <T> Components type
   * @param name Components name
   * @return Components
   */
  @SuppressWarnings("unchecked")
  private <T> Set<T> getSet(String name) {
    
    Set<T> result = new HashSet<T>();
    Object[] objs = context.locateServices(name);
    if (objs != null) {
      for (Object o : objs) {
        
        result.add((T) o);
      }
    }
    
    return result;
  }
  
  /**
   * {@inheritDoc}
   */
  public DynConfiguration getActiveConfiguration() {

    // Variable to store active dynamic configuration
    DynConfiguration dynConf = new DynConfiguration();
    dynConf.setActive(Boolean.TRUE);
    
    // Iterate all dynamic configurations components registered
    for (Object o : getComponents()) {
      try {
        
        // If component has not DynamicConfiguration annotation, ignore it
        Class<? extends Object> c = o.getClass();
        DynamicConfiguration a = c.getAnnotation(DynamicConfiguration.class);
        if (a == null) {

          continue;
        }
        
        // Invoke the read method of all components to get its properties
        Method m = (Method) c.getMethod("read", new Class[0]);
        DynPropertyList dynProps = (DynPropertyList) m.invoke(o, new Object[0]);

        // Create a dynamic configuration object with component and properties
        DynComponent dynComp = new DynComponent(c.getName(), a.name(), dynProps);
        dynConf.addComponent(dynComp);
      }
      catch (NoSuchMethodException nsme) {

        logger.log(Level.SEVERE,
            "No read method on dynamic configuration class", nsme);
      }
      catch (InvocationTargetException ite) {

        logger.log(Level.SEVERE,
            "Cannot invoke read method on dynamic configuration class", ite);
      }
      catch (IllegalAccessException iae) {

        logger.log(Level.SEVERE,
            "Cannot access read method on dynamic configuration class", iae);
      }
    }

    return dynConf;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public void setActiveConfiguration(DynConfiguration dynConf) {

    // Iterate all dynamic configurations components registered
    for (Object o : getComponents()) {
      try {
        
        // If component has not DynamicConfiguration annotation, ignore it
        Class<? extends Object> c = o.getClass();
        DynamicConfiguration a = c.getAnnotation(DynamicConfiguration.class);
        if (a == null) {

          continue;
        }
        
        // Invoke the read method of all components to get its properties
        for (DynComponent dynComp : dynConf.getComponents()) {

          if (c.getName().equals(dynComp.getId())) {

            Class[] t = new Class[1];
            t[0] = DynPropertyList.class;
            Method m = (Method) c.getMethod("write", t);
            Object[] args = new Object[1];
            args[0] = dynComp.getProperties();
            m.invoke(o, args);
          }
        }
      }
      catch (NoSuchMethodException nsme) {

        logger.log(Level.SEVERE,
            "No write method on dynamic configuration class", nsme);
      }
      catch (InvocationTargetException ite) {

        logger.log(Level.SEVERE,
            "Cannot invoke write method on dynamic configuration class", ite);
      }
      catch (IllegalAccessException iae) {

        logger.log(Level.SEVERE,
            "Cannot access write method on dynamic configuration class", iae);
      }
    }
  }

}