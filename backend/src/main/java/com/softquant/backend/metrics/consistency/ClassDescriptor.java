package com.softquant.backend.metrics.consistency;

import java.util.HashSet;
import java.util.Set;

public class ClassDescriptor {

    private String className;
    private Set<String> methods = new HashSet<>();
    private Set<String> fields = new HashSet<>();
    private String superClass;
    private boolean fromDesign;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<String> getMethods() {
        return methods;
    }

    public void setMethods(Set<String> methods) {
        this.methods = methods;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public boolean isFromDesign() {
        return fromDesign;
    }

    public void setFromDesign(boolean fromDesign) {
        this.fromDesign = fromDesign;
    }
}
