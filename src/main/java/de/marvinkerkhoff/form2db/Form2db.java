/**
 * This file Copyright (c) 2010-2015 Magnolia International
 * Ltd.  (http://www.magnolia-cms.com). All rights reserved.
 * <p>
 * <p>
 * This file is dual-licensed under both the Magnolia
 * Network Agreement and the GNU General Public License.
 * You may elect to use one or the other of these licenses.
 * <p>
 * This file is distributed in the hope that it will be
 * useful, but AS-IS and WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, or NONINFRINGEMENT.
 * Redistribution, except as permitted by whichever of the GPL
 * or MNA you select, is prohibited.
 * <p>
 * 1. For the GPL license (GPL), you can redistribute and/or
 * modify this file under the terms of the GNU General
 * Public License, Version 3, as published by the Free Software
 * Foundation.  You should have received a copy of the GNU
 * General Public License, Version 3 along with this program;
 * if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * <p>
 * 2. For the Magnolia Network Agreement (MNA), this file
 * and the accompanying materials are made available under the
 * terms of the MNA which accompanies this distribution, and
 * is available at http://www.magnolia-cms.com/mna.html
 * <p>
 * Any modifications to this file must keep this entire header
 * intact.
 */
package de.marvinkerkhoff.form2db;

import info.magnolia.module.ModuleLifecycle;
import info.magnolia.module.ModuleLifecycleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module Descriptor.
 */
public class Form2db implements ModuleLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Form2db.class);
    public static final String WORKSPACE = "form2db";
    public static final String NT_FORM = "mgnl:formNode";
    public static final String NT_FORM_ENTRY = "mgnl:formEntryNode";

    private boolean flatStructure = true;

    public boolean isFlatStructure() {
        return flatStructure;
    }

    public void setFlatStructure(final boolean flatStructure) {
        this.flatStructure = flatStructure;
    }

    @Override
    public void start(final ModuleLifecycleContext moduleLifecycleContext) {
        LOGGER.info("Start form2db module ...");
    }

    @Override
    public void stop(final ModuleLifecycleContext moduleLifecycleContext) {
        LOGGER.info("Stop form2db module ...");
    }
}
