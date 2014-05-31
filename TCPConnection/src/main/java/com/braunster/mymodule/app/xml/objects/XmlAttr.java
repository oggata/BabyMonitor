package com.braunster.mymodule.app.xml.objects;

/**
 * Created by itzik on 5/12/2014.
 */
public class XmlAttr {
    private String name, value;
    private int index;

    public XmlAttr(String name, String value){
        this.name = name;
        this.value = value;
    }

    public XmlAttr(int index, String name, String value){
        this.index = index;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getValueAsInt(){return Integer.parseInt(value);}

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
