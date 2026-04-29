package com.softquant.backend.metrics.shared.powerdesigner;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class PowerDesignerXmlSupport {

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("signature=\"([^\"]+)\"");

    public ParsedDocument parse(String xml, String expectedSignature) {
        if (xml == null || xml.isBlank()) {
            throw new PowerDesignerXmlException("PowerDesigner XML content is empty");
        }

        String signature = extractSignature(xml);
        if (expectedSignature != null && !expectedSignature.isBlank()
                && !Objects.equals(expectedSignature, signature)) {
            throw new PowerDesignerXmlException("Unexpected PowerDesigner signature: expected "
                    + expectedSignature + " but got " + signature);
        }

        Document document = parseDocument(xml);
        Element root = document.getDocumentElement();
        if (root == null) {
            throw new PowerDesignerXmlException("PowerDesigner XML root element is missing");
        }

        ObjectRegistry objectRegistry = ObjectRegistry.from(root);
        return new ParsedDocument(signature, root, objectRegistry);
    }

    private String extractSignature(String xml) {
        Matcher matcher = SIGNATURE_PATTERN.matcher(xml);
        if (!matcher.find()) {
            throw new PowerDesignerXmlException("Missing PowerDesigner processing instruction signature");
        }
        return matcher.group(1);
    }

    private Document parseDocument(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new PowerDesignerXmlException("Failed to parse PowerDesigner XML: " + ex.getMessage(), ex);
        }
    }

    public static final class ParsedDocument {
        private final String signature;
        private final Element root;
        private final ObjectRegistry objectRegistry;
        private final RefResolver refResolver;

        ParsedDocument(String signature, Element root, ObjectRegistry objectRegistry) {
            this.signature = signature;
            this.root = root;
            this.objectRegistry = objectRegistry;
            this.refResolver = new RefResolver(objectRegistry);
        }

        public String signature() {
            return signature;
        }

        public Element root() {
            return root;
        }

        public ObjectRegistry objectRegistry() {
            return objectRegistry;
        }

        public RefResolver refResolver() {
            return refResolver;
        }

        public List<Element> descendants(String localName) {
            List<Element> elements = new ArrayList<>();
            NodeList nodes = root.getElementsByTagNameNS("*", localName);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element) {
                    elements.add(element);
                }
            }
            return elements;
        }

        public List<Element> children(Element parent, String localName) {
            List<Element> children = new ArrayList<>();
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element && localName.equals(element.getLocalName())) {
                    children.add(element);
                }
            }
            return children;
        }

        public Element requireChild(Element parent, String localName) {
            List<Element> matches = children(parent, localName);
            if (matches.isEmpty()) {
                throw new PowerDesignerXmlException("Missing child '" + localName + "' under " + describe(parent));
            }
            return matches.get(0);
        }

        public String optionalText(Element parent, String localName) {
            List<Element> matches = children(parent, localName);
            if (matches.isEmpty()) {
                return null;
            }
            String text = matches.get(0).getTextContent();
            if (text == null) {
                return null;
            }
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        public String requiredText(Element parent, String localName) {
            String text = optionalText(parent, localName);
            if (text == null) {
                throw new PowerDesignerXmlException("Missing text node '" + localName + "' under " + describe(parent));
            }
            return text;
        }

        public String normalizeStereotype(Element element, Map<String, String> allowedAliases) {
            String stereotype = optionalText(element, "Stereotype");
            if (stereotype == null) {
                return "";
            }

            String normalizedKey = toAliasKey(stereotype);
            String normalized = allowedAliases.get(normalizedKey);
            if (normalized == null) {
                throw new PowerDesignerXmlException("Unknown stereotype '" + stereotype + "' at " + describe(element));
            }
            return normalized;
        }

        public String describe(Element element) {
            String id = element.getAttribute("Id");
            String name = optionalText(element, "Name");
            String code = optionalText(element, "Code");
            String label = name != null ? name : code;
            return element.getLocalName()
                    + "(Id=" + (id == null || id.isBlank() ? "?" : id)
                    + (label == null ? "" : ", Name=" + label)
                    + ")";
        }

        public static String toAliasKey(String value) {
            return value.trim()
                    .replaceAll("[^A-Za-z0-9]+", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "")
                    .toUpperCase(Locale.ROOT);
        }
    }

    public static final class ObjectRegistry {
        private final Map<String, Element> objectsById;
        private final Map<String, Element> objectsByObjectId;

        private ObjectRegistry(Map<String, Element> objectsById, Map<String, Element> objectsByObjectId) {
            this.objectsById = objectsById;
            this.objectsByObjectId = objectsByObjectId;
        }

        static ObjectRegistry from(Element root) {
            Map<String, Element> objectsById = new LinkedHashMap<>();
            Map<String, Element> objectsByObjectId = new LinkedHashMap<>();
            registerRecursive(root, objectsById, objectsByObjectId);
            return new ObjectRegistry(objectsById, objectsByObjectId);
        }

        private static void registerRecursive(Element element, Map<String, Element> objectsById,
                                              Map<String, Element> objectsByObjectId) {
            String id = element.getAttribute("Id");
            if (id != null && !id.isBlank()) {
                registerUnique(objectsById, id, element, "XML Id");
            }

            String objectId = findDirectChildText(element, "ObjectID");
            if (objectId != null) {
                registerUnique(objectsByObjectId, objectId, element, "ObjectID");
            }

            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element child) {
                    registerRecursive(child, objectsById, objectsByObjectId);
                }
            }
        }

        private static void registerUnique(Map<String, Element> registry, String key, Element element, String type) {
            Element previous = registry.putIfAbsent(key, element);
            if (previous != null && previous != element) {
                throw new PowerDesignerXmlException("Duplicate " + type + " '" + key + "' detected");
            }
        }

        private static String findDirectChildText(Element element, String localName) {
            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element child && localName.equals(child.getLocalName())) {
                    String text = child.getTextContent();
                    if (text != null) {
                        String trimmed = text.trim();
                        if (!trimmed.isEmpty()) {
                            return trimmed;
                        }
                    }
                }
            }
            return null;
        }

        public int size() {
            return objectsById.size();
        }

        public Element requireById(String id) {
            Element element = objectsById.get(id);
            if (element == null) {
                throw new PowerDesignerXmlException("Unresolved XML Id reference: " + id);
            }
            return element;
        }

        public Element findByObjectId(String objectId) {
            return objectsByObjectId.get(objectId);
        }
    }

    public static final class RefResolver {
        private final ObjectRegistry objectRegistry;

        RefResolver(ObjectRegistry objectRegistry) {
            this.objectRegistry = objectRegistry;
        }

        public Element resolveRequired(Element refElement) {
            return resolveRequired(refElement, null);
        }

        public Element resolveRequired(Element refElement, String expectedLocalName) {
            String refId = refElement.getAttribute("Ref");
            if (refId == null || refId.isBlank()) {
                throw new PowerDesignerXmlException("Missing Ref attribute on " + refElement.getLocalName());
            }

            Element resolved = objectRegistry.requireById(refId);
            if (expectedLocalName != null && !expectedLocalName.equals(resolved.getLocalName())) {
                throw new PowerDesignerXmlException("Reference '" + refId + "' expected "
                        + expectedLocalName + " but resolved to " + resolved.getLocalName());
            }
            return resolved;
        }

        public Element resolveRequiredId(String refId, String expectedLocalName, String context) {
            Element resolved = objectRegistry.requireById(refId);
            if (expectedLocalName != null && !expectedLocalName.equals(resolved.getLocalName())) {
                throw new PowerDesignerXmlException("Reference '" + refId + "' in " + context + " expected "
                        + expectedLocalName + " but resolved to " + resolved.getLocalName());
            }
            return resolved;
        }
    }

    public static final class PowerDesignerXmlException extends RuntimeException {
        public PowerDesignerXmlException(String message) {
            super(message);
        }

        public PowerDesignerXmlException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
