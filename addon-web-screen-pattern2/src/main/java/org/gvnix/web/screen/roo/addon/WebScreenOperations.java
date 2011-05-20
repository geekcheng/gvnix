/*
 * gvNIX. Spring Roo based RAD tool for Conselleria d'Infraestructures
 * i Transport - Generalitat Valenciana
 * Copyright (C) 2010, 2011 CIT - Generalitat Valenciana
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
package org.gvnix.web.screen.roo.addon;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface of web MVC screen patterns operations
 * 
 * @author Jose Manuel Vivó (jmvivo at disid dot com) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 * 
 * @since 0.8
 */
public interface WebScreenOperations {

    /**
     * Indicate commands should be available
     * 
     * @return true if it should be available, otherwise false
     */
    boolean isCommandAvailable();

    /**
     * <p>
     * Adds a pattern to a web MVC controller.
     * </p>
     * 
     * <p>
     * Adds to target controller @GvNIXPattern annotation if it's needed.
     * </p>
     * 
     * <p>
     * Adds a new string pattern description to @GvNIXPattern.
     * </p>
     * 
     * @param controllerClass
     *            The controller to apply the pattern to
     * @param name
     *            Identification to use for this pattern
     * @param pattern
     *            The pattern to apply
     */
    void addPattern(JavaType controllerClass, JavaSymbolName name,
            WebPattern pattern);
}
