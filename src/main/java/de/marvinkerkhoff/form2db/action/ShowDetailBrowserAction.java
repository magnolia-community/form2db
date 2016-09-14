package de.marvinkerkhoff.form2db.action;

import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.api.location.LocationController;
import info.magnolia.ui.contentapp.detail.DetailLocation;
import info.magnolia.ui.contentapp.detail.DetailView;
import info.magnolia.ui.vaadin.integration.contentconnector.ContentConnector;
import info.magnolia.ui.vaadin.integration.jcr.AbstractJcrNodeAdapter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays the current selected report.
 *
 * @author marvink2
 */
public class ShowDetailBrowserAction extends AbstractAction<ShowDetailBrowserActionDefinition> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AbstractJcrNodeAdapter nodeItemToEdit;
    private LocationController locationController;
    private ContentConnector contentConnector;

    @Inject
    protected ShowDetailBrowserAction(ShowDetailBrowserActionDefinition definition, AbstractJcrNodeAdapter nodeItemToEdit, LocationController locationController, ContentConnector contentConnector) {
        super(definition);
        this.nodeItemToEdit = nodeItemToEdit;
        this.locationController = locationController;
        this.contentConnector = contentConnector;
    }

    @Override
    public void execute() throws ActionExecutionException {
        try {
            Object itemId = contentConnector.getItemId(nodeItemToEdit);
            if (!contentConnector.canHandleItem(itemId)) {
                log.warn("ShowDetailBrowserAction requested for a node type definition {}. Current node type is {}. No action will be performed.", getDefinition(), String.valueOf(itemId));
                return;
            }

            final String path = contentConnector.getItemUrlFragment(itemId);
            DetailLocation location = new DetailLocation(getDefinition().getAppName(), getDefinition().getSubAppId(), DetailView.ViewType.EDIT, path, "");
            locationController.goTo(location);

        } catch (Exception e) {
            throw new ActionExecutionException("Could not execute ShowDetailBrowserAction: ", e);
        }
    }

}