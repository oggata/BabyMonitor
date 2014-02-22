package braunster.babymonitor;

/**
 * Created by itzik on 2/20/14.
 */

        import android.graphics.drawable.ColorDrawable;
        import android.graphics.drawable.Drawable;
        import android.graphics.drawable.TransitionDrawable;

public class ColorTransaction extends TransitionDrawable {
    private int interval;
    public ColorTransaction(Drawable[] layers) {
        super(layers);
        interval = 500;
        initVars();
    }
    public ColorTransaction(Drawable[] layers, int interval) {
        super(layers);
        this.interval = interval;
        initVars();
    }
    private void initVars(){
        setCrossFadeEnabled(true);
        setId(0,0);
        setId(1,1);
    }
    public void changeColor(int color){
        setDrawableByLayerId(0, getDrawable(1));
        setDrawableByLayerId(1, new ColorDrawable(color));
        startTransition(interval);
    }
}
