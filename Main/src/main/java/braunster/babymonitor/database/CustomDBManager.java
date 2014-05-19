/*
package braunster.babymonitor.database;

import android.content.Context;
import android.util.Log;

import com.barunster.arduinocar.custom_controllers_obj.CustomButton;
import com.barunster.arduinocar.custom_controllers_obj.CustomCommand;
import com.barunster.arduinocar.custom_controllers_obj.CustomController;

import java.util.ArrayList;
import java.util.List;

*/
/**
 * Created by itzik on 3/14/14.
 *//*

public class CustomDBManager {

    private static final String TAG = CustomDBManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static CustomDBManager instance;

    private CustomCommandsDataSource customCommandsDataSource;
    private OnControllerDataChanged onControllerDataChanged;

    public static CustomDBManager getInstance(){
        if (instance == null)
            throw new NullPointerException("Custom DB Manager is not initialized");

        return instance;
    }
    private List<CustomButton> customButtonList = new ArrayList<CustomButton>();

    public CustomDBManager(Context context){
        if (instance != null)
            throw new ExceptionInInitializerError("Custom DB already has an instance");

        customButtonsDataSource = new CustomButtonsDataSource(context);
        controllersDataSource = new ControllersDataSource(context);
        customCommandsDataSource = new CustomCommandsDataSource(context);

        instance = this;
    }

    */
/* Controller *//*

    public CustomController getControllerById(long id ){

        customButtonList = new ArrayList<CustomButton>();

        if (DEBUG)
            Log.i(TAG, "Getting controller by id, Id: " + id);

        CustomController customController = controllersDataSource.getControllerById(id);

        for (CustomButton btn : customButtonsDataSource.getButtonsByControllerId(id)) {
            // Adding the command of the button if has any.
            if (getCommandByButtonId((int) btn.getId()) != null) {
                btn.setCustomCommand(getCommandByButtonId( (int) btn.getId()) );

                if (DEBUG)
                    Log.i(TAG, "Button has command");
            }
            // Adding the button tot the list
            customButtonList.add(btn);
        }

        // Setting the button list to the controller.
        if (customController != null)
        {
            customController.setButtons(customButtonList);
        }

        return customController;
    }

    public List<CustomController> getAllControllers(){
        List<CustomController> list = new ArrayList<CustomController>();
        List<CustomController> tmpList;

        tmpList = controllersDataSource.getAllControllers();

        for (CustomController controller : tmpList)
        {
            list.add(getControllerById(controller.getId()));
        }

        return list;
    }

    public List<String> controllersToStringList(List<CustomController> list){
        List<String> result = new ArrayList<String>();

        for (CustomController controller : list)
        {
            result.add(controller.getName());
        }

        return result;
    }

    public long addController(CustomController customController){
        return controllersDataSource.addController(customController);
    }

    public void deleteControllerById(long id) {
        controllersDataSource.deleteControllerById(id);
    }

    */
/* Button *//*

    public CustomButton getButtonById(long buttonId) {
        return customButtonsDataSource.getButtonByButtonId(buttonId);
    }

    public long addButton(CustomButton customButton){
        return customButtonsDataSource.addButton(customButton);
    }

    public boolean deleteButtonById(long id){
        boolean isDeleted = customButtonsDataSource.deleteButtonById(id);
        customCommandsDataSource.deleteCommandByButtonId(id);
        dispatchControllerChangedEvent();
        return isDeleted;
    }

    public int updateButtonById(CustomButton customButton){
        int affectedRows= customButtonsDataSource.updateButtonById(customButton);
        dispatchControllerChangedEvent();
        return affectedRows;
    }

    */
/* Command *//*

    public CustomCommand getCommandByButtonId(int buttonId) {
        return customCommandsDataSource.getCommandByButtonId(buttonId);
    }

    public void addCommand(CustomCommand customCommand) {
        customCommandsDataSource.addCommand(customCommand);
        dispatchControllerChangedEvent();
    }

    public interface OnControllerDataChanged{
        public void onChanged();
    }

    public void setOnControllerDateChanged(OnControllerDataChanged onControllerDataChanged) {
        this.onControllerDataChanged = onControllerDataChanged;
    }

    private void dispatchControllerChangedEvent(){
        if (onControllerDataChanged != null)
            onControllerDataChanged.onChanged();
    }
}
*/
