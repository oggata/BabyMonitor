package TCP.xml.objects;

import org.xmlpull.v1.XmlPullParser;

import TCP.objects.TList;

/**
 * Created by itzik on 5/12/2014.
 */
public class XmlTag {

    private String name, text;
    private TList<XmlTag> children = new TList<XmlTag>();
    private XmlTag parent;
    private boolean ended = false;
    private TList<XmlAttr> attributes = new TList<XmlAttr>();

    public static XmlTag newInstance(){
        return new XmlTag();
    }

    /* Constructors*/
    private XmlTag(){}

    private XmlTag(String name, String text){
        this.name = name;
        this.text = text;
    }

    private XmlTag(String name){
        this.name = name;
    }

    private XmlTag(String name, TList<XmlAttr> attributes){
        this.name = name;
        this.attributes = attributes;
    }

    private XmlTag(String name, String text, TList<XmlAttr> attributes){
        this.name = name;
        this.text = text;
        this.attributes = attributes;
    }

    /* Get Tag */
    public static XmlTag getTag(String name, String text){
        return new XmlTag(name, text);
    }

    public static XmlTag getTag(String name, TList<XmlAttr> attributes){
        return new XmlTag(name, attributes);
    }

    /* Add Data*/
    public static XmlTag getTag(String name, String text, TList<XmlAttr> attributes){
        return new XmlTag(name, text, attributes);
    }

    public void addChild(XmlTag tag){
        tag.setParent(this);
        children.add(tag);
    }

    public void addAttribute(String name, String value){
        attributes.add(new XmlAttr(attributes.size(), name, value));
    }

    public void addAttribute(XmlAttr attribute){
        attribute.setIndex(attributes.size());
        attributes.add(attribute);
    }

    /* Getters And Setters*/

    public TList<XmlTag> getChildren() {
        return children;
    }

    public void setChildren(TList<XmlTag> children) {
        this.children = children;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public void setAttributes(XmlPullParser parser) {
        for (int i = 0 ; i < parser.getAttributeCount() ; i++)
            attributes.add(new XmlAttr(i, parser.getAttributeName(i), parser.getAttributeValue(i)));
    }

    public void setParent(XmlTag parent) {
        this.parent = parent;
    }

    public XmlTag getParent() {
        return parent;
    }

    public boolean hasParent(){
        return parent != null;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public boolean isEnded() {
        return ended;
    }

    public TList<XmlAttr> getAttributes() {
        return attributes;
    }

    public boolean hasText(){
        return text != null;
    }

    public void setAttributes(TList<XmlAttr> attributes) {
        this.attributes = attributes;
    }

    public XmlAttr getAttr(String name){
        for (XmlAttr attr : attributes)
            if (name.equals(attr.getName()))
                return attr;

        return null;
    }

    public String getAttrValue(String name){
        for (XmlAttr attr : attributes)
            if (name.equals(attr.getName()))
                return attr.getValue();

        return null;
    }

}
