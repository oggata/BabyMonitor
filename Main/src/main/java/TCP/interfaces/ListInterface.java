package TCP.interfaces;

import java.util.List;

/**
 * Created by itzik on 5/13/2014.
 */
public interface ListInterface<E> {
    public void add(E obj);
    public E get(int pos);
    /** Get the first child of this list without changing the counter that controls the next() method.*/
    public E getFirst();
    public E getNext();
    public boolean hasNext();
    public int size();
    public List<E> asList();
    public void add(E... data);
    public void resetCounting();
}
