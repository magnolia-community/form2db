package de.marvinkerkhoff.form2db.action;

import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import de.marvinkerkhoff.form2db.excel.ExcelCreator;
import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.vaadin.integration.jcr.AbstractJcrNodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static info.magnolia.jcr.util.NodeUtil.getName;

/**
 * Exports nodes to excel.
 *
 * @author marvink2
 */
public class ExcelExportAction extends AbstractAction<ExcelExportActionDefinition> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelExportAction.class);

    private AbstractJcrNodeAdapter nodeItemToEdit;
    private File file;

    @Inject
    protected ExcelExportAction(ExcelExportActionDefinition definition, AbstractJcrNodeAdapter nodeItemToEdit) {
        super(definition);
        this.nodeItemToEdit = nodeItemToEdit;
    }

    public void execute() throws ActionExecutionException {
        try {
            ExcelCreator excel = new ExcelCreator(nodeItemToEdit.getJcrItem());
            file = excel.getFile();
            openFileInBlankWindow(getExcelFileName() + ".xlsx");
        } catch (Exception e) {
            LOGGER.error("Error executing excel export action.", e);
        }
    }

    private String getExcelFileName() throws RepositoryException {
        String sheetName = nodeItemToEdit.getNodeName();
        final Node jcrItem = nodeItemToEdit.getJcrItem();
        if (jcrItem != null) {
            final Node parentNode = jcrItem.getParent();
            if (parentNode != null) {
                sheetName = getName(parentNode);
            }
        }
        return sheetName;
    }

    private void openFileInBlankWindow(String fileName) {
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
        resource.setMIMEType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resource.setCacheTime(0);

        Page.getCurrent().open(resource, "", true);
    }

    private class DeleteOnCloseFileInputStream extends FileInputStream {
        private File file;

        DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (file.exists() && !file.delete()) {
                LOGGER.warn("Could not delete temporary export file {}", file.getAbsolutePath());
            }
        }
    }
}