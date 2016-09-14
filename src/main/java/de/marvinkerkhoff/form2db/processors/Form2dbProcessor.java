package de.marvinkerkhoff.form2db.processors;

import info.magnolia.cms.beans.config.MIMEMapping;
import info.magnolia.cms.beans.runtime.Document;
import info.magnolia.cms.beans.runtime.MultipartForm;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.jcr.util.SessionUtil;
import info.magnolia.module.form.processors.AbstractFormProcessor;
import info.magnolia.module.form.processors.FormProcessorFailedException;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a form and stores it in the database.
 */
public class Form2dbProcessor extends AbstractFormProcessor {

    private static final Logger log = LoggerFactory.getLogger(Form2dbProcessor.class);

    @Override
    protected void internalProcess(Node content, Map<String, Object> parameters)
            throws FormProcessorFailedException {

        String formName = PropertyUtil.getString(content, "formName");
        if (formName == null) {
            log.error("please add a formName to save your data");
        } else {
            Node existingPage = null;
            try {

                existingPage = SessionUtil.getNode("form2db", "/" + content.getAncestor(1).getName());
                if (existingPage == null) {
                    existingPage = SessionUtil.getNode("form2db", "/").addNode(content.getAncestor(1).getName(), "mgnl:folder");
                }

                if (existingPage.hasNode(formName) == false) {

                    Node newForm = existingPage.addNode(formName, "mgnl:formNode").addNode("0", "mgnl:formEntryNode");

                    for (String key : parameters.keySet()) {
                        PropertyUtil.setProperty(newForm, key, parameters.get(key));
                    }

                    PropertyUtil.setProperty(newForm, "created", new Date());

                    if (getAttachments() != null) {

                        for (Map.Entry<String, Document> attachment : getAttachments().entrySet()) {
                            //create the file node - see section 6.7.22.6 of the spec
                            String filename = attachment.getValue().getFile().getName();
                            Node fileNode = newForm.addNode(filename, "mgnl:asset");
                            //create the mandatory child node - jcr:content
                            Node resNode = fileNode.addNode("jcr:content", "mgnl:resource");
                            String mimeType = MIMEMapping.getMIMETypeOrDefault(attachment.getValue().getExtension());
                            PropertyUtil.setProperty(resNode, "jcr:data", new FileInputStream(attachment.getValue().getFile()));
                            PropertyUtil.setProperty(resNode, "fileName", filename);
                            PropertyUtil.setProperty(resNode, "extension", filename.split("\\.(?=[^\\.]+$)")[1]);
                            PropertyUtil.setProperty(resNode, "jcr:mimeType", mimeType);
                        }

                    }
                } else {

                    NodeIterator itr = existingPage.getNode(formName).getNodes();

                    String name = "";
                    while (itr.hasNext()) {
                        Node entry = (Node) itr.next();
                        Integer tet = (Integer) Integer.parseInt(entry.getName()) + 1;
                        name = tet.toString();
                    }

                    Node entry = existingPage.getNode(formName).addNode(name, "mgnl:formEntryNode");
                    for (String key : parameters.keySet()) {
                        PropertyUtil.setProperty(entry, key, parameters.get(key));
                    }

                    PropertyUtil.setProperty(entry, "created", new Date());

                    if (getAttachments() != null) {

                        for (Map.Entry<String, Document> attachment : getAttachments().entrySet()) {
                            //create the file node - see section 6.7.22.6 of the spec
                            String filename = attachment.getValue().getFile().getName();
                            Node fileNode = entry.addNode(filename, "mgnl:asset");
                            //create the mandatory child node - jcr:content
                            Node resNode = fileNode.addNode("jcr:content", "mgnl:resource");
                            String mimeType = MIMEMapping.getMIMETypeOrDefault(attachment.getValue().getExtension());
                            PropertyUtil.setProperty(resNode, "jcr:data", new FileInputStream(attachment.getValue().getFile()));
                            PropertyUtil.setProperty(resNode, "fileName", filename);
                            PropertyUtil.setProperty(resNode, "extension", filename.split("\\.(?=[^\\.]+$)")[1]);
                            PropertyUtil.setProperty(resNode, "jcr:mimeType", mimeType);
                        }

                    }
                }

                existingPage.getSession().save();
            } catch (Exception e) {
                log.error("The form could not be saved in the form2db repository. Check your rights! ");
                e.printStackTrace();
            } finally {
                if (existingPage instanceof Node) {
                    try {
                        existingPage.getSession().save();
                    } catch (AccessDeniedException e) {
                        log.error("The form could not be saved in the form2db repository. Check your rights! ");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private Map<String, Document> getAttachments() {

        // get any possible attachment
        if (MgnlContext.getPostedForm() != null) {
            MultipartForm form = MgnlContext.getPostedForm();
            return form.getDocuments();
        }

        return null;
    }

}
