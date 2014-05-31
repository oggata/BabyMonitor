package com.braunster.mymodule.app.objects;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by itzik on 5/18/2014.
 */
public class Session {
    private SecureRandom random = new SecureRandom();

    private String id;

    public Session(){
        id = nextSessionId();
    }

    public String getId() {
        return id;
    }

    private String nextSessionId() {
        return new BigInteger(130, random).toString(32);
    }

    public void close(){
        id = null;
    }
}
