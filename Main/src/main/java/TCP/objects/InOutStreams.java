package TCP.objects;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by itzik on 5/11/2014.
 */
public class InOutStreams {

    private InputStream inputStream;
    private OutputStream outputStream;

    public InOutStreams(InputStream inputStream, OutputStream outputStream){
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void close() {
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
