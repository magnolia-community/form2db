package de.marvinkerkhoff.form2db.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.marvinkerkhoff.form2db.jcr.JcrSearchUtils;
import info.magnolia.cms.core.Path;
import info.magnolia.cms.util.QueryUtil;

/**
 * Creates an excel file.
 */
public class ExcelCreater {
    private static final Logger log = LoggerFactory.getLogger(ExcelCreater.class);

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
            sheet = getAllFormEntrys(sheet, rootnode);
        } else {
            sheet = getSingleFormEntry(sheet, rootnode);
        }

        wb.write(out);
        IOUtils.closeQuietly(out);
    }

    public File getFile() {
        return file;
    }

    private Sheet getAllFormEntrys(Sheet sheet, Node rootnode) throws ValueFormatException, RepositoryException {

        final ArrayList<String> sortOrderOfFormFields = determineSortOrderOfFormFields(rootnode);

        int count = 0;
        Row row = sheet.createRow(count);

        // First row: set label for each column
        int propCount = 0;
        for (String propertyname : sortOrderOfFormFields) {
            row.createCell(propCount).setCellValue(propertyname);
            propCount++;
        }

        // Following rows: set relevant cells for each form submission present
        // in JCR
        NodeIterator itr = rootnode.getNodes();
        while (itr.hasNext()) {
            Node entry = (Node) itr.next();
            count++;
            row = sheet.createRow(count);
            propCount = 0;
            for (String propertyname : sortOrderOfFormFields) {
                if (entry.hasProperty(propertyname)) {
                    row.createCell(propCount).setCellValue(entry.getProperty(propertyname).getString());
                }
                propCount++;
            }

        }
        return sheet;
    }

    private Sheet getSingleFormEntry(Sheet sheet, Node rootnode) throws RepositoryException {
        int count = 0;
        Row row = sheet.createRow(count);

        PropertyIterator entrys = rootnode.getProperties();

        int propCount = 0;

        while (entrys.hasNext()) {
            Property prop = (Property) entrys.next();
            if (!prop.getName().contains("jcr:")) {
                row.createCell(propCount).setCellValue(prop.getString());
                propCount++;
            }
        }
        count++;
        return sheet;
    }

    private ArrayList<String> determineSortOrderOfFormFields(Node formNodeInForm2Db) throws RepositoryException {

        ArrayList<String> sortOrderOfFormFieldsCleanedUp = new ArrayList<String>();

        // Get root website name & form name
        final String rootWebsiteName = formNodeInForm2Db.getParent().getName();
        final String formName = formNodeInForm2Db.getName();

        // Get form node in website with that name
        final Node formNodeInWebsiteWorkspace = getFormNodeForNameAndWebsite(rootWebsiteName, formName);

        // Get sort order of nodes from relevant web page (website workspace)
        final ArrayList<String> sortOrderOfFormField = getSortOrderOfFormFieldsFromWebPage(formNodeInWebsiteWorkspace);

        // Get property names for current form in our Form2Db workspace
        final ArrayList<String> propertynames = getFieldNamesSavedInForm2Db(formNodeInForm2Db);

        // Only sort properties that are present in BOTH the Website workspace
        // && Form2Db workspace
        if (sortOrderOfFormField != null) {
            for (String formFieldName : sortOrderOfFormField) {
                if (propertynames.contains(formFieldName) && !sortOrderOfFormFieldsCleanedUp.contains(formFieldName)) {
                    sortOrderOfFormFieldsCleanedUp.add(formFieldName);
                }
            }
        }

        // Add up at the end the other properties that are only present in the
        // Form2Db workspace
        if (propertynames != null) {
            for (String propName : propertynames) {
                if (!sortOrderOfFormFieldsCleanedUp.contains(propName)) {
                    sortOrderOfFormFieldsCleanedUp.add(propName);
                }
            }
        }

        return sortOrderOfFormFieldsCleanedUp;
    }

    // Get property names for current form in our Form2Db workspace
    private ArrayList<String> getFieldNamesSavedInForm2Db(Node formNodeInForm2Db) throws RepositoryException {
        if (formNodeInForm2Db == null) {
            return null;
        }
        ArrayList<String> propertyNames = new ArrayList<String>();
        NodeIterator propitr = formNodeInForm2Db.getNodes();
        while (propitr.hasNext()) {
            PropertyIterator entrys = ((Node) propitr.next()).getProperties();
            while (entrys.hasNext()) {
                Property prop = (Property) entrys.next();
                String propName = prop.getName();
                if (!propertyNames.contains(propName) && !propName.contains("jcr:")) {
                    propertyNames.add(propName);
                }
            }
        }
        return propertyNames;
    }

    private Node getFormNodeForNameAndWebsite(String rootWebsiteName, String formName) {
        if (rootWebsiteName != null && formName != null) {
            String query = "SELECT p.* FROM [nt:base] AS p WHERE ISDESCENDANTNODE('/" + rootWebsiteName
                    + "') AND (p.[formName] <> '' AND p.[formName] = '" + formName + "')";
            NodeIterator fieldsWrappersNodeIterator = null;
            try {
                fieldsWrappersNodeIterator = QueryUtil.search("website", query, javax.jcr.query.Query.JCR_SQL2);
            } catch (RepositoryException e) {
                log.info("Problem when trying to search for form '" + formName + "' starting from '/" + rootWebsiteName
                        + "' in the website workspace.");
                e.printStackTrace();
            }
            List<Node> sortedFieldsWrappersNodes = IteratorUtils.toList(fieldsWrappersNodeIterator);
            if (sortedFieldsWrappersNodes == null || sortedFieldsWrappersNodes.isEmpty()) {
                log.error("No result when searching for form '" + formName + "' starting from '/" + rootWebsiteName
                        + "' in the website workspace.");
            } else {
                if (sortedFieldsWrappersNodes.size() > 1) {
                    log.error("Too many results when searching for form '" + formName + "' starting from '/"
                            + rootWebsiteName + "' in the website workspace.");
                    log.error("This form name must be unique for that website!");
                    return null;
                }
                return sortedFieldsWrappersNodes.get(0);
            }
        }
        return null;
    }

    /*
     * Get sort order of form fields param: form node identifier in website
     * workspace
     * 
     * Must be done via Java - because there is no way to find natural order via
     * JCR SQL2 Read more:
     * http://stackoverflow.com/questions/16980029/jcr-sql2-result-query-order-
     * as-in-jcr-browser
     */
    private ArrayList<String> getSortOrderOfFormFieldsFromWebPage(Node formNode) {

        if (formNode == null) {
            return null;
        }

        String formPath = "";
        try {
            formPath = formNode.getPath();
        } catch (RepositoryException e1) {
            e1.printStackTrace();
        }

        /*
         * Search nodes called "fields"
         */
        ArrayList<String> nameConditions = new ArrayList<String>();
        nameConditions.add("fields");
        ArrayList<Node> sortedFormFieldsWrapperNodes = JcrSearchUtils.searchRecursivelyNameMatchVal(formNode,
                nameConditions, null);

        /*
         * Search nodes having properties "controlName" Note: a node could have
         * a property "controlName" & still not be a form field (i.e. it a group
         * field) TODO: add some checkbox to say
         * "show in form2db & excel export"
         */
        ArrayList<String> propertyPresConditions = new ArrayList<String>();
        propertyPresConditions.add("controlName");
        ArrayList<Node> sortedFormFieldsNodes = new ArrayList<Node>();
        for (Node formFieldWrap : sortedFormFieldsWrapperNodes) {
            ArrayList<Node> currSortedFormFields = JcrSearchUtils.searchRecursivelyPropPres(formFieldWrap,
                    propertyPresConditions, null);
            if (currSortedFormFields != null) {
                sortedFormFieldsNodes.addAll(currSortedFormFields);
            }
        }

        ArrayList<String> sortOrderOfFormFields = new ArrayList<String>();
        for (Node currentField : sortedFormFieldsNodes) {
            String currentFieldControlName;
            try {
                currentFieldControlName = currentField.hasProperty("controlName")
                        ? currentField.getProperty("controlName").getString() : null;
            } catch (RepositoryException e) {
                currentFieldControlName = null;
                log.info("Form2Db issue when trying to get field 'controlName' in form: " + formPath);
            }
            if (currentFieldControlName != null) {
                sortOrderOfFormFields.add(currentFieldControlName);
            }
        }

        return sortOrderOfFormFields;
    }

}
