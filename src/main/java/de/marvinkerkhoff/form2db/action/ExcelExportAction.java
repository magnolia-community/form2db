package de.marvinkerkhoff.form2db.action;

import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.api.location.LocationController;
import info.magnolia.ui.vaadin.integration.jcr.AbstractJcrNodeAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;

import de.marvinkerkhoff.form2db.excel.ExcelCreater;


/**
 * Exports deadlink-reports to excel.
 * 
 * @author marvink2
 */
@SuppressWarnings("serial")
public class ExcelExportAction extends AbstractAction<ExcelExportActionDefinition> {

    private AbstractJcrNodeAdapter nodeItemToEdit;
    private File file;

    @Inject
    protected ExcelExportAction(ExcelExportActionDefinition definition, AbstractJcrNodeAdapter nodeItemToEdit, LocationController locationController) {
        super(definition);
        this.nodeItemToEdit = nodeItemToEdit;
    }

    public void execute() throws ActionExecutionException {
        try {
            ExcelCreater excel = new ExcelCreater(nodeItemToEdit.getJcrItem());
            file = excel.getFile();
            openFileInBlankWindow(nodeItemToEdit.getNodeName() + ".xlsx", "xlsx");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void openFileInBlankWindow(String fileName, String mimeType) {
        
        StreamResource.StreamSource source = new StreamResource.StreamSource() {
            public InputStream getStream() {
                try {
                    return new DeleteOnCloseFileInputStream(file);
                } catch (IOException e) {
                    return null;
                }
            }
        };
        StreamResource resource = new StreamResource(source, fileName);
        resource.setCacheTime(-1);
        resource.getStream().setParameter("Content-Disposition", "attachment; filename=" + fileName + "\"");
        resource.setMIMEType(mimeType);
        resource.setCacheTime(0);
          
        Page.getCurrent().open(resource, "", true);
    }

    private class DeleteOnCloseFileInputStream extends FileInputStream {
        private File file;
        private final Logger log = LoggerFactory.getLogger(DeleteOnCloseFileInputStream.class);

        public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (file.exists() && !file.delete()) {
                log.warn("Could not delete temporary export file {}", file.getAbsolutePath());
            }
        }

    }

}