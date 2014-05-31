package com.braunster.mymodule.app.objects;

import com.braunster.mymodule.app.interfaces.ListInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by itzik on 5/13/2014.
 */
public class TList<E> implements ListInterface<E>, Iterable<E>, Iterator<E>  {
    private int counter = -1;
    private List<E> children = new ArrayList<E>();

    public TList(){}

    public TList(E... attrs) {
        for (E a : attrs) children.add(a);
    }

    @Override
    public void add(E obj) {
        children.add(obj);
    }

    @Override
    public E get(int pos) {
        if (pos >= size())
            return null;

        return children.get(pos);
    }

    @Override
    public E getFirst() {
        if (children.size() > 0)
            return children.get(0);
        else return null;
    }

    @Override
    public E getNext() {
        counter++;
        return children.get(counter);
    }

    @Override
    public boolean hasNext() {
        return counter == -1 ? children.size() > 0 : counter + 1 < children.size();
    }

    @Override
    public E next() {
        counter++;
        return children.get(counter);
    }

    @Override
    public void remove() {
        children.remove(counter);
    }

    @Override
    public int size() {
        return children.size();
    }

    @Override
    public List<E> asList() {
        return children;
    }

    @Override
    public void add(E... data) {
        for (E d : data)
            children.add(d);
    }

    @Override
    public void resetCounting() {
        counter = -1;
    }

    @Override
    public Iterator iterator() {
        return children.iterator();
    }

}
