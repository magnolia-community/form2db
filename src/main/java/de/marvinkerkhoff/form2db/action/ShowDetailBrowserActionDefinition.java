package de.marvinkerkhoff.form2db.action;

import info.magnolia.ui.contentapp.detail.action.AbstractItemActionDefinition;

/**
 * Defines an action for display the current selected report.
 *
 * @author marvink2
 */
public class ShowDetailBrowserActionDefinition extends AbstractItemActionDefinition {

    public ShowDetailBrowserActionDefinition() {
        setImplementationClass(ShowDetailBrowserAction.class);
    }

}
