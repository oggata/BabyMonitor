package TCP;

/**
 * Created by itzik on 3/3/14.
 */
public class XmlMessage {

    // TODO change class to be more flex so it could be added to the libary at the end
    private String batteryPercentage;

    public XmlMessage(String batteryPercentage){
        this.batteryPercentage = batteryPercentage;
    }

    public String getBatteryPercentage() {
        return batteryPercentage;
    }
}
