package de.marvinkerkhoff.form2db.excel;

import com.google.common.collect.Lists;
import de.marvinkerkhoff.form2db.Form2db;
import de.marvinkerkhoff.form2db.jcr.JcrSearchUtils;
import de.marvinkerkhoff.form2db.processors.Form2dbProcessor;
import info.magnolia.cms.core.Path;
import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.objectfactory.Components;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static info.magnolia.jcr.util.NodeUtil.asIterable;
import static info.magnolia.jcr.util.NodeUtil.asList;
import static info.magnolia.jcr.util.NodeUtil.getName;
import static info.magnolia.jcr.util.PropertyUtil.getString;
import static info.magnolia.repository.RepositoryConstants.WEBSITE;
import static java.util.Collections.singletonList;

/**
 * Creates an excel file.
 */
public class ExcelCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelCreator.class);

    private static final int HEADER_ROW_NUMBER = 0;
    private static final Collator COLLATOR = Collator.getInstance(Locale.GERMAN);

    private final File file = File.createTempFile("excel-form2db", ".xlsx", Path.getTempDirectory());
    private CellStyle headerStyle;
    private Form2db form2db;

    public ExcelCreator(Node rootNode) throws RepositoryException, IOException {
        Workbook wb = new XSSFWorkbook();
        initHeaderStyle(wb);

        Sheet sheet = wb.createSheet(getSheetName(rootNode));
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);

        renderSheet(rootNode, sheet);

        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
    }

    private String getSheetName(final Node rootNode) throws RepositoryException {
        String sheetName = getName(rootNode);
        Node parentNode = rootNode.getParent();
        if (parentNode != null) {
            sheetName = getName(parentNode);
        }
        return sheetName;
    }

    private void initHeaderStyle(final Workbook wb) {
        headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(font);
    }

    private void renderSheet(final Node rootNode, final Sheet sheet) throws RepositoryException {
        if (Form2db.NT_FORM.equals(rootNode.getPrimaryNodeType().getName())) {
            renderAllFormEntries(sheet, rootNode);
        } else {
            renderSingleFormEntry(sheet, rootNode);
        }
    }

    private void renderAllFormEntries(final Sheet sheet, final Node rootNode) throws RepositoryException {
        int rowCount = HEADER_ROW_NUMBER;
        final List<Node> nodes = asList(asIterable(rootNode.getNodes()));
        final List<String> propertyNames = determineSortOrderOfFormFields(rootNode, nodes);
        renderHeader(sheet, propertyNames);

        for (Node node : nodes) {
            Row row = sheet.createRow(++rowCount);
            renderCells(node, propertyNames, row);
        }
    }

    private void renderHeader(final Sheet sheet, final List<String> propertyNames) {
        Row row = sheet.createRow(HEADER_ROW_NUMBER);

        int propCount = 0;
        for (String propertyName : propertyNames) {
            final Cell cell = row.createCell(propCount++);
            cell.setCellValue(propertyName);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Ordering by name but {@link Form2dbProcessor#CREATED_PROPERTY_NAME} is always first.
     */
    private Comparator<String> createComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(@Nullable final String left, @Nullable final String right) {
                int returnValue = 0;
                if (Form2dbProcessor.CREATED_PROPERTY_NAME.equals(left)) {
                    returnValue = -1;
                } else if (Form2dbProcessor.CREATED_PROPERTY_NAME.equals(right)) {
                    returnValue = 1;
                } else if (left != null && right != null) {
                    returnValue = COLLATOR.compare(left, right);
                }
                return returnValue;
            }
        };
    }

    private void renderSingleFormEntry(final Sheet sheet, final Node node) throws RepositoryException {
        final List<String> propertyNames = determineSortOrderOfFormFields(node, Lists.newArrayList(node));
        renderHeader(sheet, propertyNames);

        Row row = sheet.createRow(HEADER_ROW_NUMBER + 1);
        renderCells(node, propertyNames, row);
    }

    private void renderCells(final Node node, final List<String> propertyNames, final Row row) throws RepositoryException {
        int propCount = 0;
        for (String propertyName : propertyNames) {
            String value = getString(node, propertyName, StringUtils.EMPTY);
            row.createCell(propCount++).setCellValue(value);
        }
    }

    public File getFile() {
        return file;
    }

    private List<String> determineSortOrderOfFormFields(Node formNodeInForm2Db, List<Node> forms) throws RepositoryException {
        List<String> sortOrderOfFormField = new ArrayList<>();

        // skip form field order detection, if sorting by field names
        if (!getForm2db().isSortHeaderByName()) {
            Node formNode = formNodeInForm2Db;
            if (formNode.isNodeType(Form2db.NT_FORM_ENTRY)) {
                formNode = formNode.getParent();
            }
            // Get root website name & form name
            final String rootWebsitePath = formNode.getParent().getPath();
            final String formName = formNode.getName();

            // Get form node in website with that name
            final Node formNodeInWebsiteWorkspace = getFormNodeForNameAndWebsite(rootWebsitePath, formName);

            // Get sort order of nodes from relevant web page (website workspace)
            sortOrderOfFormField = getSortOrderOfFormFieldsFromWebPage(formNodeInWebsiteWorkspace);
        }

        // Get property names for current form in our Form2Db workspace
        final List<String> propertyNames = getFieldNamesSavedInForm2Db(formNodeInForm2Db, forms);

        return getSortOrderOfFormFieldsCleanedUp(sortOrderOfFormField, propertyNames);
    }

    private List<String> getSortOrderOfFormFieldsCleanedUp(final List<String> sortOrderOfFormField, final List<String> propertyNames) {
        List<String> sortOrderOfFormFieldsCleanedUp = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(sortOrderOfFormField)) {
            // Only sort properties that are present in BOTH the Website workspace && Form2Db workspace
            for (String formFieldName : sortOrderOfFormField) {
                if (propertyNames.contains(formFieldName) && !sortOrderOfFormFieldsCleanedUp.contains(formFieldName)) {
                    sortOrderOfFormFieldsCleanedUp.add(formFieldName);
                }
            }

            // Add up at the end the other properties that are only present in the Form2Db workspace
            for (String propName : propertyNames) {
                if (!sortOrderOfFormFieldsCleanedUp.contains(propName)) {
                    if (Form2dbProcessor.CREATED_PROPERTY_NAME.equals(propName)) {
                        // put created property at first column
                        List<String> listWithCreated = new ArrayList<>(singletonList(propName));
                        listWithCreated.addAll(sortOrderOfFormFieldsCleanedUp);
                        sortOrderOfFormFieldsCleanedUp = listWithCreated;
                    } else {
                        sortOrderOfFormFieldsCleanedUp.add(propName);
                    }
                }
            }
        } else {
            sortOrderOfFormFieldsCleanedUp.addAll(propertyNames);
        }

        return sortOrderOfFormFieldsCleanedUp;
    }

    /**
     * Get property names for current form in our Form2Db workspace in alphabetical order.
     */
    private List<String> getFieldNamesSavedInForm2Db(Node formNodeInForm2Db, List<Node> forms) throws RepositoryException {
        List<String> propertyNames = new ArrayList<>();
        if (formNodeInForm2Db != null) {
            for (Node node : forms) {
                PropertyIterator entries = node.getProperties();
                while (entries.hasNext()) {
                    Property prop = (Property) entries.next();
                    String propName = prop.getName();
                    if (!propertyNames.contains(propName) && !propName.contains(NodeTypes.JCR_PREFIX) && !prop.getName().contains(NodeTypes.MGNL_PREFIX)) {
                        propertyNames.add(propName);
                    }
                }
            }
        }
        Collections.sort(propertyNames, createComparator());

        return propertyNames;
    }

    private Node getFormNodeForNameAndWebsite(String rootWebsitePath, String formName) {
        Node formNode = null;
        if (rootWebsitePath != null && formName != null) {
            String query = "SELECT p.* FROM [nt:base] AS p WHERE ISDESCENDANTNODE('" + rootWebsitePath + "') AND p.[formName] = '" + formName + "'";

            try {
                NodeIterator fieldsWrappersNodeIterator = QueryUtil.search(WEBSITE, query, Query.JCR_SQL2);
                List<Node> sortedFieldsWrappersNodes = asList(asIterable(fieldsWrappersNodeIterator));
                if (CollectionUtils.isEmpty(sortedFieldsWrappersNodes)) {
                    LOGGER.warn("No result when searching for form '{}' starting from '{}' in the website workspace.", formName, rootWebsitePath);
                } else if (sortedFieldsWrappersNodes.size() == 1) {
                    formNode = sortedFieldsWrappersNodes.get(0);
                } else {
                    LOGGER.warn("Too many results when searching for form '{}' starting from '{}' in the website workspace. This form name must be unique for that website!", formName, rootWebsitePath);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Problem when trying to search for form '{}' starting from '{}' in the website workspace.", formName, rootWebsitePath, e);
            }
        }
        return formNode;
    }

    /**
     * Get sort order of form fields param: form node identifier in website workspace.
     * <p>
     * Must be done via Java - because there is no way to find natural order via JCR SQL2 Read more:
     * http://stackoverflow.com/questions/16980029/jcr-sql2-result-query-order-as-in-jcr-browser
     */
    private List<String> getSortOrderOfFormFieldsFromWebPage(Node formNode) {
        List<String> sortOrderOfFormFields = new ArrayList<>();
        if (formNode != null) {
            //Search nodes called "fields"
            List<Node> sortedFormFieldsWrapperNodes = JcrSearchUtils.searchRecursivelyNameMatchVal(formNode, singletonList("fields"));

            // Search nodes having properties "controlName" Note: a node could have a property "controlName" & still not be a form field (i.e. it a group field)
            // TODO: add some checkbox to say "show in form2db & excel export"
            List<String> propertyPresConditions = singletonList("controlName");
            List<Node> sortedFormFieldsNodes = new ArrayList<>();
            for (Node formFieldWrap : sortedFormFieldsWrapperNodes) {
                List<Node> currSortedFormFields = JcrSearchUtils.searchRecursivelyPropPres(formFieldWrap, propertyPresConditions);
                if (currSortedFormFields != null) {
                    sortedFormFieldsNodes.addAll(currSortedFormFields);
                }
            }

            for (Node currentField : sortedFormFieldsNodes) {
                String currentFieldControlName = getString(currentField, "controlName");
                if (currentFieldControlName != null) {
                    sortOrderOfFormFields.add(currentFieldControlName);
                }
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
