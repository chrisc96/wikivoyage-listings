package org.wikivoyage.listings.input.template;

import de.fau.cs.osr.ptk.common.ast.AstStringNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtTemplateArgument;
import org.sweble.wikitext.parser.nodes.WtXmlComment;
import org.sweble.wikitext.parser.utils.WtRtDataPrinter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper around org.sweble.wikitext.parser.nodes.WtTemplate for easy work with wikitext template data
 */
public class TemplateNode {
    private WtTemplate node;
    private HashMap<String, String> namedArguments;
    private HashMap<String, String> namedArgumentsLowercase;
    private List<String> positionalArguments;

    private static final Log log = LogFactory.getLog(TemplateNode.class);

    private final TemplateToStringConverter[] templateParsers = {
        new FrenchPrixTemplateToStringConverter(),
        new RussianRoadTemplateToStringConverter(),
        new FrenchHoraireTemplateToStringConverter()
    };

    public TemplateNode(WtTemplate node)
    {
        this.node = node;
        parseArguments();
        initNamedArgumentsLowercase();
    }

    public String getName()
    {
        return convertToStringSimple(node.getName()).trim();
    }

    public String getNameLowercase()
    {
        return getName().toLowerCase();
    }

    public String getArgument(String name)
    {
        return namedArgumentsLowercase.get(name.toLowerCase());
    }

    public boolean hasArgument(String name)
    {
        return namedArgumentsLowercase.containsKey(name.toLowerCase());
    }

    public List<String> getPositionalArguments()
    {
        return positionalArguments;
    }

    public String getPositionalArg(int index, String defaultValue)
    {
        if (positionalArguments.size() <= index) {
            return defaultValue;
        } else {
            return positionalArguments.get(index);
        }
    }

    public String getPositionalArg(int index)
    {
        return positionalArguments.get(index);
    }

    public boolean isAbsentOrEmptyPositionalArg(int index)
    {
        return positionalArguments.size() <= index || positionalArguments.get(index).equals("");
    }

    /**
     * Parse template arguments into a dictionary
     */
    private void parseArguments() {
        namedArguments = new LinkedHashMap<>();
        positionalArguments = new LinkedList<>();

        for (WtNode templateArgumentsChildNode: node.getArgs()) {
            if (templateArgumentsChildNode instanceof WtTemplateArgument) {
                WtTemplateArgument templateArgument = (WtTemplateArgument) templateArgumentsChildNode;

                String name = convertWtNodeToString(templateArgument.getName()).trim();
                String value = convertWtNodeToString(templateArgument.getValue()).trim();
                if (name.equals("")) {
                    positionalArguments.add(value);
                } else {
                    namedArguments.put(name, value);
                }
            }
        }
    }

    /**
     * Initialize template arguments dictionary with lowercase keys
     */
    private void initNamedArgumentsLowercase() {
        namedArgumentsLowercase = new LinkedHashMap<>();

        for (String key: namedArguments.keySet()) {
            namedArgumentsLowercase.put(key.toLowerCase(), namedArguments.get(key));
        }
    }

    /**
     * Text conversion of WtNode object to string.
     */
    private String convertWtNodeToString(WtNode node)
    {
        if (node instanceof WtTemplate) {
            TemplateNode templateNode = new TemplateNode((WtTemplate) node);

            for (TemplateToStringConverter parser: templateParsers) {
                if (templateNode.getNameLowercase().equals(parser.getTemplateName())) {
                    return parser.convertToString(templateNode);
                }
            }

            log.debug("Template '" + templateNode.getName() + "' was not parsed");
            return WtRtDataPrinter.print(node);
        } else if (node instanceof WtXmlComment) {
            // HTML comments inside listings are ignored
            return "";
        } else if (node instanceof AstStringNode) {
            return ((AstStringNode) node).getContent().replaceAll("\\[\\[([^|\\]]*?\\||)([^|\\]]*?)\\]\\]", "$2");
        } else {
            String s = "";
            for (WtNode childNode: node) {
                s += convertWtNodeToString(childNode);
            }
            return s;
        }
    }

    /**
     * Convert simple wiki text node to string
     */
    private static String convertToStringSimple(WtNode node)
    {
        if (node instanceof AstStringNode) {
            // Handle simple string node - just get its contents
            AstStringNode stringNode = (AstStringNode) node;
            return stringNode.getContent();
        } else {
            // Handle compound node, that consists of several others
            String textResult = "";
            for (WtNode childNode: node) {
                textResult += convertToStringSimple(childNode);
            }
            return textResult;
        }
    }
}