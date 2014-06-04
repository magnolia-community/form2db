package de.marvinkerkhoff.form2db.excel;

import info.magnolia.cms.core.Path;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelCreater {

	final private File file = File.createTempFile("excel-form2db", ".xlsx", Path.getTempDirectory());
    private FileOutputStream out = new FileOutputStream(file);

    public ExcelCreater(Node rootnode) throws Exception {
        Workbook wb;

        wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet(rootnode.getName());

        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        
        if (rootnode.getPrimaryNodeType().getName().equals("mgnl:formNode")) {
        	sheet = getAllFormEntrys(sheet,rootnode);
        } else {
        	sheet = getSingleFormEntry(sheet,rootnode);
        }

        wb.write(out);
        IOUtils.closeQuietly(out);
    }

    public File getFile() {
        return file;
    }
    
    private Sheet getAllFormEntrys(Sheet sheet, Node rootnode) throws ValueFormatException, RepositoryException {
    	NodeIterator propitr = rootnode.getNodes();
        
        Set<String> propertynames = new HashSet<String>();
        
        while (propitr.hasNext()) {
        	PropertyIterator entrys = ((Node) propitr.next()).getProperties();
        	while(entrys.hasNext()) {  
            	Property prop = (Property) entrys.next();
            	if (!prop.getName().contains("jcr:")) {
                	propertynames.add(prop.getName());
            	}
            }
        }
        
        int count = 0;
        Row row = sheet.createRow(count);
        NodeIterator itr = rootnode.getNodes();
        
        int propCount = 0;   
        for(String propertyname : propertynames) { 
        	row.createCell(propCount).setCellValue(propertyname);
        	
        	propCount++;
        }
        
        while (itr.hasNext()) {
            Node entry = (Node) itr.next();
            count++;
            row = sheet.createRow(count);
                                    
            propCount = 0;   
            
            for(String propertyname : propertynames) { 
            	if (entry.hasProperty(propertyname)) {
            		row.createCell(propCount).setCellValue(entry.getProperty(propertyname).getString());
            	}	
            	propCount++;
            }

        }
		return sheet;    	
    }
    
    private Sheet getSingleFormEntry (Sheet sheet, Node rootnode) throws RepositoryException {
    	int count = 0;
    	Row row = sheet.createRow(count);
        
        PropertyIterator entrys = rootnode.getProperties();
        
        int propCount = 0;           
        
        while(entrys.hasNext()) {            	
        	Property prop = (Property) entrys.next();
        	if (!prop.getName().contains("jcr:")) {
        		row.createCell(propCount).setCellValue(prop.getString());
            	propCount++;
        	}
        }
        count++;
        return sheet;    	
    }
    
}
