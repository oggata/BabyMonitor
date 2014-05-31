package com.braunster.mymodule.app.interfaces;


import com.braunster.mymodule.app.xml.objects.XmlTag;

/**
 * Created by itzik on 3/3/14.
 */
public interface IncomingDataListener {

    public void onStringDateReceived(String data);
    public void onParedXmlReady(XmlTag xmlTag);

}
