package de.marvinkerkhoff.form2db.excel;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import de.marvinkerkhoff.form2db.Form2db;
import de.marvinkerkhoff.form2db.jcr.JcrSearchUtils;
import info.magnolia.cms.core.Path;
import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.objectfactory.Components;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.ImmutableSortedSet.orderedBy;
import static com.google.common.collect.Lists.newArrayList;
import static de.marvinkerkhoff.form2db.processors.Form2dbProcessor.CREATED_PROPERTY_NAME;
import static info.magnolia.jcr.util.PropertyUtil.getPropertyOrNull;
import static info.magnolia.repository.RepositoryConstants.WEBSITE;

/**
 * Creates an excel file.
 */
public class ExcelCreater {

    private static final Logger log = LoggerFactory.getLogger(ExcelCreater.class);

    private static final int HEADER_ROW_NUMBER = 0;
    private static final Collator COLLATOR = Collator.getInstance(Locale.GERMAN);

    private final File file = File.createTempFile("excel-form2db", ".xlsx", Path.getTempDirectory());
    private CellStyle headerStyle;
    private Form2db form2db;

    public ExcelCreater(Node rootnode) throws RepositoryException, IOException {

        Workbook wb = new XSSFWorkbook();
        initHeaderStyle(wb);

        Sheet sheet = wb.createSheet(rootnode.getName());
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);

        renderSheet(rootnode, sheet);

        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
    }

    private void initHeaderStyle(final Workbook wb) {
        headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(font);
    }

    private void renderSheet(final Node rootnode, final Sheet sheet) throws RepositoryException {
        if (Form2db.NT_FORM.equals(rootnode.getPrimaryNodeType().getName())) {
            renderAllFormEntries(sheet, rootnode);
        } else {
            renderSingleFormEntry(sheet, rootnode);
        }
    }

    private void renderAllFormEntries(final Sheet sheet, final Node rootNode) throws RepositoryException {
        int rowCount = HEADER_ROW_NUMBER;
        final Collection<Node> nodes = NodeUtil.getCollectionFromNodeIterator(rootNode.getNodes());
        final List<String> propertyNames = getAllPropertyNames(rootNode, Lists.newArrayList(nodes));

        renderHeader(sheet, propertyNames);

        for (Node node : nodes) {
            Row row = sheet.createRow(++rowCount);
            renderCells(node, propertyNames, row);
        }
    }

    private void renderHeader(final Sheet sheet, final List<String> propertyNames) {
        int rowCount = HEADER_ROW_NUMBER;
        Row row = sheet.createRow(rowCount);

        int propCount = 0;
        for (String propertyName : propertyNames) {
            final Cell cell = row.createCell(propCount++);
            cell.setCellValue(propertyName);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Returns all property names of all given nodes.
     */
    private List<String> getAllPropertyNames(final Node rootnode, final List<Node> forms) throws RepositoryException {
        List<String> sortedPropertyNamesFromDialog = determineSortOrderOfFormFields(rootnode, forms);
        if (getForm2db().isSortHeaderByName()) {
            sortedPropertyNamesFromDialog = orderedBy(orderByPropertyNameAfterCreated().nullsLast()).addAll(sortedPropertyNamesFromDialog).build().asList();
        }
        return sortedPropertyNamesFromDialog;
    }

    /**
     * Ordering by name but {@link Form2dbProcessor.CREATED_PROPERTY_NAME} is always first.
     */
    private Ordering<String> orderByPropertyNameAfterCreated() {
        return new Ordering<String>() {
            @Override
            public int compare(@Nullable final String left, @Nullable final String right) {
                int returnValue = 0;
                if (CREATED_PROPERTY_NAME.equals(left)) {
                    returnValue = -1;
                } else if (CREATED_PROPERTY_NAME.equals(right)) {
                    returnValue = 2;
                } else if (left != null && right != null) {
                    returnValue = COLLATOR.compare(left, right);
                }
                return returnValue;
            }
        };
    }

    private void renderSingleFormEntry(final Sheet sheet, final Node node) throws RepositoryException {

        final List<String> propertyNames = getAllPropertyNames(node, Lists.newArrayList(node));
        renderHeader(sheet, propertyNames);

        Row row = sheet.createRow(HEADER_ROW_NUMBER + 1);
        renderCells(node, propertyNames, row);
    }

    private void renderCells(final Node node, final List<String> propertyNames, final Row row) throws RepositoryException {
        int propCount = 0;
        for (String propertyname : propertyNames) {
            String value = StringUtils.EMPTY;
            if (node.hasProperty(propertyname)) {
                final Property propertyOrNull = getPropertyOrNull(node, propertyname);
                if (propertyOrNull != null) {
                    value = propertyOrNull.getString();
                }
            }
            row.createCell(propCount++).setCellValue(value);
        }
    }

    public File getFile() {
        return file;
    }

    private List<String> determineSortOrderOfFormFields(Node formNodeInForm2Db, List<Node> forms) throws RepositoryException {

        // Get root website name & form name
        final String rootWebsiteName = formNodeInForm2Db.getParent().getName();
        final String formName = formNodeInForm2Db.getName();

        // Get form node in website with that name
        final Node formNodeInWebsiteWorkspace = getFormNodeForNameAndWebsite(rootWebsiteName, formName);

        // Get sort order of nodes from relevant web page (website workspace)
        final List<String> sortOrderOfFormField = getSortOrderOfFormFieldsFromWebPage(formNodeInWebsiteWorkspace);

        // Get property names for current form in our Form2Db workspace
        final List<String> propertynames = getFieldNamesSavedInForm2Db(formNodeInForm2Db, forms);

        return getSortOrderOfFormFieldsCleanedUp(sortOrderOfFormField, propertynames);
    }

    private List<String> getSortOrderOfFormFieldsCleanedUp(final List<String> sortOrderOfFormField, final List<String> propertynames) {
        List<String> sortOrderOfFormFieldsCleanedUp = newArrayList();

        if (CollectionUtils.isNotEmpty(sortOrderOfFormField)) {
            // Only sort properties that are present in BOTH the Website workspace
            // && Form2Db workspace
            for (String formFieldName : sortOrderOfFormField) {
                if (propertynames.contains(formFieldName) && !sortOrderOfFormFieldsCleanedUp.contains(formFieldName)) {
                    sortOrderOfFormFieldsCleanedUp.add(formFieldName);
                }
            }

            // Add up at the end the other properties that are only present in the
            // Form2Db workspace
            for (String propName : propertynames) {
                if (!sortOrderOfFormFieldsCleanedUp.contains(propName)) {
                    sortOrderOfFormFieldsCleanedUp.add(propName);
                }
            }
        } else {
            sortOrderOfFormFieldsCleanedUp.addAll(propertynames);
        }

        return sortOrderOfFormFieldsCleanedUp;
    }

    // Get property names for current form in our Form2Db workspace
    private List<String> getFieldNamesSavedInForm2Db(Node formNodeInForm2Db, List<Node> forms) throws RepositoryException {
        if (formNodeInForm2Db == null) {
            return newArrayList();
        }
        List<String> propertyNames = new ArrayList<>();
        for (Node node : forms) {
            PropertyIterator entrys = node.getProperties();
            while (entrys.hasNext()) {
                Property prop = (Property) entrys.next();
                String propName = prop.getName();
                if (!propertyNames.contains(propName) && !propName.contains("jcr:") &&  !prop.getName().contains("mgnl:")) {
                    propertyNames.add(propName);
                }
            }
        }
        return propertyNames;
    }

    private Node getFormNodeForNameAndWebsite(String rootWebsiteName, String formName) {

        if (rootWebsiteName != null && formName != null) {
            String query = "SELECT p.* FROM [nt:base] AS p WHERE ISDESCENDANTNODE('/" + rootWebsiteName + "') AND (p.[formName] <> '' AND p.[formName] = '" + formName + "')";

            NodeIterator fieldsWrappersNodeIterator = null;

            try {
                fieldsWrappersNodeIterator = QueryUtil.search(WEBSITE, query, Query.JCR_SQL2);
            } catch (RepositoryException e) {
                log.error("Problem when trying to search for form '{}' starting from '/{}' in the website workspace.", formName, rootWebsiteName, e);
            }

            List<Node> sortedFieldsWrappersNodes = IteratorUtils.toList(fieldsWrappersNodeIterator);
            if (sortedFieldsWrappersNodes == null || sortedFieldsWrappersNodes.isEmpty()) {
                log.warn("No result when searching for form '{}' starting from '/{}' in the website workspace.", formName, rootWebsiteName);
            } else {
                if (sortedFieldsWrappersNodes.size() > 1) {
                    log.warn("Too many results when searching for form '{}' starting from '/{}' in the website workspace. This form name must be unique for that website!", formName, rootWebsiteName);
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
    private List<String> getSortOrderOfFormFieldsFromWebPage(Node formNode) {

        if (formNode == null) {
            return newArrayList();
        }

        String formPath = NodeUtil.getPathIfPossible(formNode);

        /*
         * Search nodes called "fields"
         */
        List<String> nameConditions = new ArrayList<>();
        nameConditions.add("fields");
        List<Node> sortedFormFieldsWrapperNodes = JcrSearchUtils.searchRecursivelyNameMatchVal(formNode, nameConditions, null);

        /*
         * Search nodes having properties "controlName" Note: a node could have
         * a property "controlName" & still not be a form field (i.e. it a group
         * field) TODO: add some checkbox to say
         * "show in form2db & excel export"
         */
        List<String> propertyPresConditions = new ArrayList<>();
        propertyPresConditions.add("controlName");
        List<Node> sortedFormFieldsNodes = new ArrayList<>();
        for (Node formFieldWrap : sortedFormFieldsWrapperNodes) {
            List<Node> currSortedFormFields = JcrSearchUtils.searchRecursivelyPropPres(formFieldWrap, propertyPresConditions, null);
            if (currSortedFormFields != null) {
                sortedFormFieldsNodes.addAll(currSortedFormFields);
            }
        }

        List<String> sortOrderOfFormFields = new ArrayList<>();
        for (Node currentField : sortedFormFieldsNodes) {
            String currentFieldControlName;
            try {
                currentFieldControlName = currentField.hasProperty("controlName") ? currentField.getProperty("controlName").getString() : null;
            } catch (RepositoryException e) {
                currentFieldControlName = null;
                log.error("Form2Db issue when trying to get field 'controlName' in form: {}", formPath, e);
            }
            if (currentFieldControlName != null) {
                sortOrderOfFormFields.add(currentFieldControlName);
            }
        }

        return sortOrderOfFormFields;
    }

    private Form2db getForm2db() {
        if (form2db == null) {
            form2db = Components.getComponent(Form2db.class);
        }
        return form2db;
    }

}
