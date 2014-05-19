package braunster.babymonitor.objects;

/**
 * Created by itzik on 5/18/2014.
 */
public class Call {
    private String name, number, text, date;
    private long id;

    public Call(long id, String... data){
        this.id = id;
        name = data[0];
        number = data[1];

        if (data.length > 2)
            text = data[2];
        else return;

        if (data.length > 3)
            date = data[3];

    }

    public Call(String... data){
        name = data[0];
        number = data[1];

        if (data.length > 2)
            text = data[2];
        else return;

        if (data.length > 3)
            date = data[3];
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }
}
