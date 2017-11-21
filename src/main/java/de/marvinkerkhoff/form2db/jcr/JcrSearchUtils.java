package de.marvinkerkhoff.form2db.jcr;

import info.magnolia.jcr.util.NodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static info.magnolia.jcr.util.NodeUtil.asList;
import static info.magnolia.jcr.util.NodeUtil.getName;
import static info.magnolia.jcr.util.PropertyUtil.getPropertyOrNull;

/**
 * SearchUtils for JCR.
 */
public final class JcrSearchUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrSearchUtils.class);

    private JcrSearchUtils() {
    }

    /**
     * Recursive iteration of nodes beginning by root node for node with one of the given name.
     *
     * @param rootNode root node
     * @param nameConditions name conditions
     */
    public static List<Node> searchRecursivelyNameMatchVal(Node rootNode, List<String> nameConditions) {
        List<Node> searchResults = new ArrayList<>();
        for (Node currentSubNode : getNodes(rootNode)) {
            for (String nameCond : nameConditions) {
                if (StringUtils.equals(nameCond, getName(currentSubNode))) {
                    searchResults.add(currentSubNode);
                }
            }

            searchResults.addAll(searchRecursivelyNameMatchVal(currentSubNode, nameConditions));
        }
        return searchResults;
    }

    private static List<Node> getNodes(Node node) {
        List<Node> nodes = Collections.emptyList();
        try {
            nodes = asList(NodeUtil.getNodes(node));
        } catch (RepositoryException e) {
            LOGGER.error("Recursive search in JCR tree via JCR API failed (node name matching value)", e);
        }
        return nodes;
    }

    /**
     * Recursive iteration of nodes beginning by root node for nodes having given properties.
     *
     * @param rootNode root node
     * @param propertyPresConditions property conditions
     */
    public static List<Node> searchRecursivelyPropPres(Node rootNode, List<String> propertyPresConditions) {
        List<Node> searchResults = new ArrayList<>();
        List<Node> nodes = getNodes(rootNode);

        for (Node subNode : nodes) {
            addSubNodeIfHasAllRequiredProperties(propertyPresConditions, searchResults, subNode);
            searchResults.addAll(searchRecursivelyPropPres(subNode, propertyPresConditions));
        }
        return searchResults;
    }

    private static void addSubNodeIfHasAllRequiredProperties(final List<String> propertyPresConditions, final List<Node> searchResults, final Node currentSubNode) {
        boolean hasAllRequiredProperties = true;

        for (String propertyName : propertyPresConditions) {
            if (getPropertyOrNull(currentSubNode, propertyName) == null) {
                hasAllRequiredProperties = false;
                break;
            }
        }
        if (hasAllRequiredProperties) {
            searchResults.add(currentSubNode);
        }
    }
}
