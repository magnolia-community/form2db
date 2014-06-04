package de.marvinkerkhoff.form2db.action;

import info.magnolia.ui.contentapp.detail.action.AbstractItemActionDefinition;

/**
 * Defines an action for export deadlink-reports to excel.
 * 
 * @author marvink2
 */
public class ExcelExportActionDefinition extends AbstractItemActionDefinition {

    public ExcelExportActionDefinition() {
        setImplementationClass(ExcelExportAction.class);
    }

}
