package com.softquant.backend.metrics.consistency;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class XmiParser {

    public Map<String, ClassDescriptor> parse(String xml) {
        Map<String, ClassDescriptor> map = new HashMap<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            Map<String, String> idToName = new HashMap<>();
            NodeList classNodes = doc.getElementsByTagName("UML:Class");
            if (classNodes.getLength() == 0) {
                classNodes = doc.getElementsByTagName("packagedElement");
            }
            for (int i = 0; i < classNodes.getLength(); i++) {
                Element el = (Element) classNodes.item(i);
                String type = el.getAttribute("xmi:type");
                if (!type.isEmpty() && !type.equals("uml:Class") && !type.equals("uml:Interface")) {
                    continue;
                }
                String id = el.hasAttribute("xmi:id") ? el.getAttribute("xmi:id") : el.getAttribute("xmi.id");
                String name = el.hasAttribute("name") ? el.getAttribute("name") : el.getAttribute("xmi.id");
                if (name.isBlank()) continue;
                idToName.put(id, name);
                ClassDescriptor descriptor = map.computeIfAbsent(name, k -> {
                    ClassDescriptor d = new ClassDescriptor();
                    d.setClassName(k);
                    d.setFromDesign(true);
                    return d;
                });
                NodeList ops = el.getElementsByTagName("ownedOperation");
                if (ops.getLength() == 0) ops = el.getElementsByTagName("UML:Operation");
                for (int j = 0; j < ops.getLength(); j++) {
                    String opName = ((Element) ops.item(j)).getAttribute("name");
                    if (!opName.isBlank()) descriptor.getMethods().add(opName);
                }
                NodeList attrs = el.getElementsByTagName("ownedAttribute");
                if (attrs.getLength() == 0) attrs = el.getElementsByTagName("UML:Attribute");
                for (int j = 0; j < attrs.getLength(); j++) {
                    String attrName = ((Element) attrs.item(j)).getAttribute("name");
                    if (!attrName.isBlank()) descriptor.getFields().add(attrName);
                }
            }

            NodeList genNodes = doc.getElementsByTagName("generalization");
            if (genNodes.getLength() == 0) genNodes = doc.getElementsByTagName("UML:Generalization");
            for (int i = 0; i < genNodes.getLength(); i++) {
                Element el = (Element) genNodes.item(i);
                String childId = el.hasAttribute("specific") ? el.getAttribute("specific")
                        : el.getAttribute("subtype");
                String parentId = el.hasAttribute("general") ? el.getAttribute("general")
                        : el.getAttribute("supertype");
                String childName = idToName.get(childId);
                String parentName = idToName.get(parentId);
                if (childName != null && parentName != null) {
                    map.computeIfAbsent(childName, k -> {
                        ClassDescriptor d = new ClassDescriptor();
                        d.setClassName(k);
                        d.setFromDesign(true);
                        return d;
                    }).setSuperClass(parentName);
                }
            }
        } catch (Exception e) {
            // 解析失败返回空 map
        }
        return map;
    }
}
