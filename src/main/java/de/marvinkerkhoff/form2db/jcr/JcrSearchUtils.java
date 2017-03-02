package de.marvinkerkhoff.form2db.jcr;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static info.magnolia.jcr.util.NodeUtil.getCollectionFromNodeIterator;
import static info.magnolia.jcr.util.NodeUtil.getName;

/**
 * SearchUtils for JCR.
 */
public final class JcrSearchUtils {

    private static final Logger log = LoggerFactory.getLogger(JcrSearchUtils.class);

    private JcrSearchUtils() {
    }

    /*
     * Recursive search in JCR tree: node name matching value Parameters: -
     * node: node to start the search from - propertyValueConditions: properties
     * searched along with the value expected for it - searchResults: set this
     * to null when launching the search
     */
    public static List<Node> searchRecursivelyNameMatchVal(Node rootNode, List<String> nameConditions, List<Node> searchResults) {
        if (searchResults == null) {
            searchResults = newArrayList();
        }

        for (Node currentSubNode : getNodes(rootNode)) {

            for (String nameCond : nameConditions) {
                if (StringUtils.equals(nameCond, getName(currentSubNode))) {
                    searchResults.add(currentSubNode);
                }
            }

            searchRecursivelyNameMatchVal(currentSubNode, nameConditions, searchResults);
        }
        return searchResults;
    }

    private static Collection<Node> getNodes(Node node) {
        Collection<Node> nodes = Collections.emptyList();
        try {
            nodes = getCollectionFromNodeIterator(node.getNodes());
        } catch (RepositoryException e) {
            log.error("Recursive search in JCR tree via JCR API failed (node name matching value)", e);
        }
        return nodes;
    }

    /*
     * Recursive search in JCR tree: required properties present Parameters: -
     * node: node to start the search from - propertyValueConditions: properties
     * searched along with the value expected for it - searchResults: set this
     * to null when launching the search
     */
    public static List<Node> searchRecursivelyPropPres(Node node, List<String> propertyPresConditions, List<Node> searchResults) {
        if (searchResults == null) {
            searchResults = new ArrayList<>();
        }
        try {
            NodeIterator list = node.getNodes();

            while (list.hasNext()) {
                Node currentSubNode = list.nextNode();
                addSubNodeIfHasAllRequiredProperties(propertyPresConditions, searchResults, currentSubNode);
                searchRecursivelyPropPres(currentSubNode, propertyPresConditions, searchResults);
            }

        } catch (RepositoryException rpe) {
            log.error("Recursive search in JCR tree via JCR API failed (required properties present)", rpe);
        }
        return searchResults;
    }

    private static void addSubNodeIfHasAllRequiredProperties(final List<String> propertyPresConditions, final List<Node> searchResults, final Node currentSubNode) throws RepositoryException {
        boolean hasAllRequiredProperties = true;

        for (String propertyName : propertyPresConditions) {
            if (!currentSubNode.hasProperty(propertyName)) {
                hasAllRequiredProperties = false;
            }
        }
        if (hasAllRequiredProperties) {
            searchResults.add(currentSubNode);
        }
    }

}
