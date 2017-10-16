package de.marvinkerkhoff.form2db.processors;

import static de.marvinkerkhoff.form2db.Form2db.NT_FORM;
import static de.marvinkerkhoff.form2db.Form2db.NT_FORM_ENTRY;
import static info.magnolia.cms.beans.config.MIMEMapping.getMIMETypeOrDefault;
import static info.magnolia.cms.core.Path.getUniqueLabel;
import static info.magnolia.cms.core.Path.getValidatedLabel;
import static info.magnolia.context.MgnlContext.doInSystemContext;
import static info.magnolia.jcr.util.NodeUtil.createPath;
import static info.magnolia.jcr.util.PropertyUtil.getBoolean;
import static info.magnolia.jcr.util.PropertyUtil.getString;
import static info.magnolia.jcr.util.PropertyUtil.setProperty;
import static java.text.DateFormat.MEDIUM;
import static java.util.Locale.GERMAN;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import de.marvinkerkhoff.form2db.Form2db;
import info.magnolia.cms.beans.runtime.Document;
import info.magnolia.context.MgnlContext;
import info.magnolia.dam.jcr.AssetNodeTypes.Asset;
import info.magnolia.dam.jcr.AssetNodeTypes.AssetResource;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.module.form.processors.AbstractFormProcessor;
import info.magnolia.module.form.processors.FormProcessorFailedException;
import info.magnolia.objectfactory.Components;
import info.magnolia.templating.functions.TemplatingFunctions;

/**
 * Processes a form and stores it in the database.
 */
public class Form2dbProcessor extends AbstractFormProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Form2dbProcessor.class);

    private TemplatingFunctions templatingFunctions;
    private Form2db form2db;

    @Override
    protected void internalProcess(Node componentNode, Map<String, Object> parameters)
            throws FormProcessorFailedException {
        try {
            boolean saveToJcr = getBoolean(componentNode, "saveToJcr", false);
            Node page = getTemplatingFunctions().page(componentNode);
            if (saveToJcr && page != null) {
                String pagePath = page.getPath();
                String formNodePath = addOrGetBaseStructure(pagePath, getString(componentNode, "formName", "formName").trim());

                if (formNodePath != null) {
                    createFormEntry(parameters, formNodePath);
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("The form could not be saved in the form2db repository.", e);
        }
    }

    private void createFormEntry(final Map<String, Object> parameters, final String formNodePath)
            throws RepositoryException {
        final Map<String, Document> docs = MgnlContext.getPostedForm().getDocuments();

        doInSystemContext(new MgnlContext.Op<Boolean, RepositoryException>() {
            @Override
            public Boolean exec() throws RepositoryException {
                Session jcrSession = MgnlContext.getJCRSession(Form2db.WORKSPACE);
                Date now = new Date();
                String entryName = SimpleDateFormat.getDateTimeInstance(MEDIUM, MEDIUM, GERMAN).format(now);
                Node formNode = jcrSession.getNode(formNodePath);
                entryName = getUniqueLabel(formNode, getValidatedLabel(entryName));
                Node entry = formNode.addNode(entryName, NT_FORM_ENTRY);
                setProperty(entry, "created", now);
                storeFields(entry, parameters);
                if (docs != null) {
                    storeAttachments(entry, docs);
                }
                jcrSession.save();
                return true;
            }
        });
    }

    private void storeAttachments(final Node entry, Map<String, Document> docs) throws RepositoryException {

        if (docs != null) {
            for (Map.Entry<String, Document> attachment : docs.entrySet()) {
                try {
                    String filename = attachment.getValue().getFile().getName();
                    Node fileNode = entry.addNode(filename, Asset.NAME);
                    Node resNode = fileNode.addNode(AssetResource.RESOURCE_NAME, AssetResource.NAME);
                    String mimeType = getMIMETypeOrDefault(attachment.getValue().getExtension());
                    setProperty(resNode, AssetResource.DATA, new FileInputStream(attachment.getValue().getFile()));
                    setProperty(resNode, "fileName", filename);
                    setProperty(resNode, "extension", substringAfterLast(filename, "."));
                    setProperty(resNode, "jcr:mimeType", mimeType);
                } catch (FileNotFoundException e) {
                    LOGGER.error("Error accessing file. Skip this attachment.", e);
                }
            }
        }
    }

    private void storeFields(final Node entry, final Map<String, Object> parameters) throws RepositoryException {
        for (String key : parameters.keySet()) {
            Object value = parameters.get(key);
            if (value != null) {
                String propertyValue;
                if (value.getClass().isArray()) {
                    propertyValue = join((Object[]) value, ",");
                } else {
                    propertyValue = value.toString();
                }
                entry.setProperty(key, propertyValue);
            }
        }
    }

    private String addOrGetBaseStructure(final String pagePath, final String formName) throws RepositoryException {
        return doInSystemContext(new MgnlContext.Op<String, RepositoryException>() {
            @Override
            public String exec() throws RepositoryException {
                Node formNode = null;
                Session jcrSession = MgnlContext.getJCRSession(Form2db.WORKSPACE);

                String basePath = removeStart(pagePath, "/");
                if (useFlatStructure()) {
                    basePath = substringBefore(basePath, "/");
                }

                Node pageNode = createPath(jcrSession.getRootNode(), basePath, NodeTypes.Folder.NAME, true);
                if (pageNode != null) {
                    formNode = createPath(pageNode, getValidatedLabel(formName), NT_FORM, true);
                }
                return formNode != null ? formNode.getPath() : null;
            }
        });
    }

    protected TemplatingFunctions getTemplatingFunctions() {
        if (templatingFunctions == null) {
            templatingFunctions = Components.getComponent(TemplatingFunctions.class);
        }
        return templatingFunctions;
    }

    private boolean useFlatStructure() {
        if (form2db == null) {
            form2db = Components.getComponent(Form2db.class);
        }
        return form2db.isFlatStructure();
    }
}
