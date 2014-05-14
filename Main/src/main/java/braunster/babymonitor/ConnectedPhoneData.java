package braunster.babymonitor;

/**
 * Created by itzik on 5/14/2014.
 */
public class ConnectedPhoneData {
    private int batteryLevel = -1;
    private String batteryStatus, number;

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public String getNumber() {
        return number;
    }
}
