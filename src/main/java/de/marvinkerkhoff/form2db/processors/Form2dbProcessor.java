package de.marvinkerkhoff.form2db.processors;

import info.magnolia.cms.beans.config.MIMEMapping;
import info.magnolia.cms.beans.runtime.Document;
import info.magnolia.cms.beans.runtime.MultipartForm;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.jcr.util.SessionUtil;
import info.magnolia.module.form.processors.AbstractFormProcessor;
import info.magnolia.module.form.processors.FormProcessorFailedException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry; 

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Form2dbProcessor extends AbstractFormProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(Form2dbProcessor.class);

	@Override
	protected void internalProcess(Node content, Map<String, Object> parameters)
			throws FormProcessorFailedException {
		
		String formName = PropertyUtil.getString(content,"formName");
		if (formName == null) {
			log.error("please add a formName to save your data");
		} else {
			try {
								
				Node existingPage = SessionUtil.getNode("form2db", "/"+content.getAncestor(1).getName());
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
					        Node fileNode = newForm.addNode (attachment.getValue().getFile().getName(), "nt:file");
					        //create the mandatory child node - jcr:content
					        Node resNode = fileNode.addNode ("jcr:content", "nt:resource");			
					        String mimeType = MIMEMapping.getMIMETypeOrDefault(attachment.getValue().getExtension());					        
					        PropertyUtil.setProperty(resNode, "jcr:data", new FileInputStream (attachment.getValue().getFile()));
					        PropertyUtil.setProperty(resNode, "jcr:mimeType", mimeType);
						}
						
					}									
				} else {
					
					NodeIterator itr = existingPage.getNode(formName).getNodes();
					
					String name = "";
			        while (itr.hasNext()) {
			            Node entry = (Node) itr.next();
			            Integer tet = (Integer) Integer.parseInt(entry.getName())+1;
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
					        Node fileNode = entry.addNode (attachment.getValue().getFile().getName(), "nt:file");
					        //create the mandatory child node - jcr:content
					        Node resNode = fileNode.addNode ("jcr:content", "nt:resource");			
					        String mimeType = MIMEMapping.getMIMETypeOrDefault(attachment.getValue().getExtension());					        
					        PropertyUtil.setProperty(resNode, "jcr:data", new FileInputStream (attachment.getValue().getFile()));
					        PropertyUtil.setProperty(resNode, "jcr:mimeType", mimeType);
						}
						
					}
				}
				
				existingPage.getSession().save();
			} catch (Exception e) {
						
			}
		}
		
	}
	
	private Map<String, Document> getAttachments() { 
	  
    	// get any possible attachment    
    	if(MgnlContext.getPostedForm() != null) {    
    		MultipartForm form = MgnlContext.getPostedForm();    
    		return form.getDocuments();    
		}    
		    
		return null;    
	}

}
