package de.marvinkerkhoff.form2db.jcr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SearchUtils for JCR.
 */
public class JcrSearchUtils {

    private static final Logger log = LoggerFactory.getLogger(JcrSearchUtils.class);

    /*
     * Recursive search in JCR tree: node name matching value Parameters: -
     * node: node to start the search from - propertyValueConditions: properties
     * searched along with the value expected for it - searchResults: set this
     * to null when launching the search
     */
    public static ArrayList<Node> searchRecursivelyNameMatchVal(Node node, ArrayList<String> nameConditions,
            ArrayList<Node> searchResults) {
        if (searchResults == null) {
            searchResults = new ArrayList<Node>();
        }
        try {
            NodeIterator list = node.getNodes();

            while (list.hasNext()) {

                Node currentSubNode = list.nextNode();

                for (String nameCond : nameConditions) {
                    if (nameCond.equals(currentSubNode.getName())) {
                        searchResults.add(currentSubNode);
                    }
                }

                searchRecursivelyNameMatchVal(currentSubNode, nameConditions, searchResults);
            }

            return searchResults;
        } catch (RepositoryException rpe) {
            log.info("Recursive search in JCR tree via JCR API failed (node name matching value)");
        }
        return null;
    }

    /*
     * Recursive search in JCR tree: properties matching values Parameters: -
     * node: node to start the search from - propertyValueConditions: properties
     * searched along with the value expected for it - searchResults: set this
     * to null when launching the search
     */
    public static ArrayList<Node> searchRecursivelyPropMatchVal(Node node,
            HashMap<String, String> propertyValueConditions, ArrayList<Node> searchResults) {
        if (searchResults == null) {
            searchResults = new ArrayList<Node>();
        }
        try {
            NodeIterator list = node.getNodes();

            while (list.hasNext()) {

                Node currentSubNode = list.nextNode();
                Boolean hasAllRequiredPropsAndVals = true;

                for (Map.Entry<String, String> entry : propertyValueConditions.entrySet()) {
                    String propertyName = entry.getKey();
                    Object searchedValue = entry.getValue();
                    if (!currentSubNode.hasProperty(propertyName)
                            || !currentSubNode.getProperty(propertyName).getString().equals(searchedValue)) {
                        hasAllRequiredPropsAndVals = false;
                    }
                }
                if (hasAllRequiredPropsAndVals) {
                    searchResults.add(currentSubNode);
                }

                searchRecursivelyPropMatchVal(currentSubNode, propertyValueConditions, searchResults);
            }

            return searchResults;
        } catch (RepositoryException rpe) {
            log.info("Recursive search in JCR tree via JCR API failed (properties matching values)");
        }
        return null;
    }

    /*
     * Recursive search in JCR tree: required properties present Parameters: -
     * node: node to start the search from - propertyValueConditions: properties
     * searched along with the value expected for it - searchResults: set this
     * to null when launching the search
     */
    public static ArrayList<Node> searchRecursivelyPropPres(Node node, ArrayList<String> propertyPresConditions,
            ArrayList<Node> searchResults) {
        if (searchResults == null) {
            searchResults = new ArrayList<Node>();
        }
        try {
            NodeIterator list = node.getNodes();

            while (list.hasNext()) {

                Node currentSubNode = list.nextNode();
                Boolean hasAllRequiredProperties = true;

                for (String propertyName : propertyPresConditions) {
                    if (!currentSubNode.hasProperty(propertyName)) {
                        hasAllRequiredProperties = false;
                    }
                }
                if (hasAllRequiredProperties) {
                    searchResults.add(currentSubNode);
                }

                searchRecursivelyPropPres(currentSubNode, propertyPresConditions, searchResults);
            }

            return searchResults;
        } catch (RepositoryException rpe) {
            log.info("Recursive search in JCR tree via JCR API failed (required properties present)");
        }
        return null;
    }

}
