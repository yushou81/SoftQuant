package com.softquant.backend.metrics.common.dto;

public class ClassMetrics {

    private String className;
    private String superClassName;
    private int wmc;
    private int dit;
    private int noc;
    private int cbo;
    private int rfc;
    private int lcom;
    private int cs;
    private int npa;
    private int noo;
    private int noa;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public int getWmc() {
        return wmc;
    }

    public void setWmc(int wmc) {
        this.wmc = wmc;
    }

    public int getDit() {
        return dit;
    }

    public void setDit(int dit) {
        this.dit = dit;
    }

    public int getNoc() {
        return noc;
    }

    public void setNoc(int noc) {
        this.noc = noc;
    }

    public int getCbo() {
        return cbo;
    }

    public void setCbo(int cbo) {
        this.cbo = cbo;
    }

    public int getRfc() {
        return rfc;
    }

    public void setRfc(int rfc) {
        this.rfc = rfc;
    }

    public int getLcom() {
        return lcom;
    }

    public void setLcom(int lcom) {
        this.lcom = lcom;
    }

    public int getCs() {
        return cs;
    }

    public void setCs(int cs) {
        this.cs = cs;
    }

    public int getNpa() {
        return npa;
    }

    public void setNpa(int npa) {
        this.npa = npa;
    }

    public int getNoo() {
        return noo;
    }

    public void setNoo(int noo) {
        this.noo = noo;
    }

    public int getNoa() {
        return noa;
    }

    public void setNoa(int noa) {
        this.noa = noa;
    }
}
