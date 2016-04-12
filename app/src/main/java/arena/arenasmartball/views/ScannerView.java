package arena.arenasmartball.views;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.SmartBallScanner;

/**
 * Custom View for showing and connecting to scan results.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class ScannerView extends View// implements SmartBallScanner.SmartBallScannerListener
{
    // ViewUpdater for regulating redraws
    private ViewUpdater viewUpdater;

    // The Set of ScannerViewListeners attached to this ScannerView
    private Set<ScannerViewListener> listeners;

    // List of ScanResults
    private final ArrayList<ScanResultWrapper> scanResults;
    // The ScanResult being dragged currently
    private ScanResultWrapper draggedResult;
    // The connected ScanResult
    private ScanResultWrapper connectedResult;
    // Whether the user is moving the connected result
    private boolean moveConnected;

    // The radius of the background circle
    private float radius;

    // The radius of the central circle
    private float innerRadius;

    // Radius for drawing ScanResults
    private float scanResultRadius;

    // Directional offset for the view
    private float rotOffset;

    // Paint used for drawing
    private static final Paint PAINT;

    static
    {
        PAINT = new Paint();
        PAINT.setAntiAlias(true);
    }

    // Width of the donut
    private float donutWidth;
    // Default donut thickness
    private static final float DEF_DONUT_THICKNESS = 64.0f;

    // Donut color
    private int donutColor;
    // Default donut color
    private static final int DEF_DONUT_COLOR = 0xFFCCCCDD;

    // Text size
    private float textSize;
    // Default text size
    private static final float DEF_TEXT_SIZE = 32.0f;

    // SmartBall drawable
    private static Bitmap smartBallBmp;

    // Log tag String
    private static final String TAG = "ScannerView";

    // Bundle keys
    private static final String SCAN_RESULTS_KEY = "arena.arenasmartball.views.scanResults";
//    private static final String DRAGGED_RESULT_KEY = "arena.arenasmartball.views.draggedResult";
    private static final String CONNECTED_RESULT_KEY = "arena.arenasmartball.views.connectedResult";
//    private static final String MOVE_CONNECTED_KEY = "arena.arenasmartball.views.moveConnected";
    private static final String ROT_OFFSET_KEY = "arena.arenasmartball.views.rotOffset";

    /**
     * Required Constructor.
     * @param context The parent Context
     */
    public ScannerView(Context context)
    {
        super(context);

        scanResults = new ArrayList<>();
        listeners = new HashSet<>();
        init(context, null);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     */
    public ScannerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        scanResults = new ArrayList<>();
        listeners = new HashSet<>();
        init(context, attrs);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     * @param defStyle The default style
     */
    public ScannerView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        scanResults = new ArrayList<>();
        listeners = new HashSet<>();
        init(context, attrs);
    }

    /*
     * Prevents redundancies in the three Constructors.
     */
    private void init(Context context, AttributeSet attrs)
    {
        viewUpdater = new ViewUpdater(this);
        viewUpdater.start();

        rotOffset = 0.0f;

        draggedResult = null;

        if (smartBallBmp == null)
        {
            smartBallBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.smartball_image);
        }

        if (attrs != null)
        {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.ScannerView, 0, 0);

            try
            {
                donutWidth = a.getDimension(R.styleable.ScannerView_donutThickness, DEF_DONUT_THICKNESS);
                donutColor = a.getColor(R.styleable.ScannerView_donutColor, DEF_DONUT_COLOR);
                textSize = a.getDimension(R.styleable.ScannerView_textSize, DEF_TEXT_SIZE);
            }
            finally
            {
                a.recycle();
            }
        }
    }

    /**
     * Adds a ScannerViewListener to this ScannerView.
     * @param listener The ScannerViewListener to add
     */
    public void addScannerViewListener(ScannerViewListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Removes a ScannerViewListener from this ScannerView.
     * @param listener The ScannerViewListener to remove
     */
    public void removeScannerViewListener(ScannerViewListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Called when the user selects a ScanResult with which to connect.
     * @param scanResult The ScanResult
     */
    public void connect(ScanResult scanResult)
    {
        Log.d(TAG, "Connect to " + scanResult);

        for (ScannerViewListener listener: listeners)
            listener.onConnectTo(scanResult);
    }

    /**
     * Called when the user indicates he wishes to disconnect from a ScanResult.
     * @param scanResult The ScanResult from which to disconnect
     */
    public void disconnect(ScanResult scanResult)
    {
        Log.d(TAG, "Disconnect from " + scanResult);

        for (ScannerViewListener listener: listeners)
            listener.onDisconnectFrom(scanResult);
    }

    /**
     * Loads state from a Bundle.
     * @param bundle The Bundle
     */
    public void load(Bundle bundle)
    {
        Log.d(TAG, bundle.toString());

        scanResults.clear();
        ArrayList<Parcelable> list = bundle.getParcelableArrayList(SCAN_RESULTS_KEY);
        if (list != null)
        for (Parcelable p: list)
        {
            scanResults.add((ScanResultWrapper) p);
        }

//        draggedResult = bundle.getParcelable(DRAGGED_RESULT_KEY);
        connectedResult = bundle.getParcelable(CONNECTED_RESULT_KEY);
//        moveConnected = bundle.getBoolean(MOVE_CONNECTED_KEY);
        rotOffset = bundle.getFloat(ROT_OFFSET_KEY);
    }

    /**
     * Saves the current state to the Bundle
     * @param bundle The Bundle
     */
    public void save(Bundle bundle)
    {
        if (draggedResult != null)
        {
            insertResult(draggedResult);
            draggedResult = null;
        }

        moveConnected = false;

        bundle.putParcelableArrayList(SCAN_RESULTS_KEY, scanResults);
//        bundle.putParcelable(DRAGGED_RESULT_KEY, draggedResult);
        bundle.putParcelable(CONNECTED_RESULT_KEY, connectedResult);
//        bundle.putBoolean(MOVE_CONNECTED_KEY, moveConnected);
        bundle.putFloat(ROT_OFFSET_KEY, rotOffset);
    }

    /**
     * Called when the view is rendered.
     * @param canvas The Canvas on which to draw
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // Draw the Background
        drawBackground(canvas);

        // Update ScanResultWrappers
        float num = (float) scanResults.size();
        float dir = 0.0f;//270.0f;
        float x, y;

        synchronized (scanResults)
        {
            ScanResultWrapper result;

            for (int i = 0; i < scanResults.size(); ++i)
            {
                result = scanResults.get(i);

                // Calculate the destination position of the ScanResultWrapper
                x = (float) Math.cos((dir + rotOffset) * Utils.DEG_2_RAD) * radius;
                y = (float) Math.sin((dir + rotOffset) * Utils.DEG_2_RAD) * radius;

                result.setPosition(getWidth() / 2 + x, getHeight() / 2 + y);

                dir += 360.0f / num;

                if (result.update(canvas))
                    viewUpdater.redraw(false);
            }
        }

        // Update the connectedResult
        if (connectedResult != null && connectedResult.update(canvas))
            viewUpdater.redraw(false);

        // Update the draggedResult
        if (draggedResult != null && draggedResult.update(canvas))
            viewUpdater.redraw(false);

        // Get ready to update again if necessary
        viewUpdater.notifyLock();
    }

    /**
     * Called when this View is resized.
     * @param w The new width
     * @param h The new height
     * @param oldW The old width
     * @param oldH The old height
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        super.onSizeChanged(w, h, oldW, oldH);

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();

        radius = (Math.min(width, height) - donutWidth) / 2;
        innerRadius = (radius - donutWidth) / 2;
        scanResultRadius = Math.min(donutWidth, innerRadius) * 0.5f;

        if (connectedResult != null)
            connectedResult.forcePosition(getWidth() / 2, getHeight() / 2);

        synchronized (scanResults)
        {
            float x, y, dir = 0.0f;
            int num = scanResults.size();
            ScanResultWrapper result;

            for (int i = 0; i < scanResults.size(); ++i)
            {
                result = scanResults.get(i);

                // Calculate the destination position of the ScanResultWrapper
                x = (float) Math.cos((dir + rotOffset) * Utils.DEG_2_RAD) * radius;
                y = (float) Math.sin((dir + rotOffset) * Utils.DEG_2_RAD) * radius;

                result.forcePosition(getWidth() / 2 + x, getHeight() / 2 + y);
                result.isCarried = false;

                dir += 360.0f / num;
            }
        }

        viewUpdater.redraw(true);
    }

    /**
     * Adds a ScanResult to this ScannerView.
     * @param result The ScanResult to add
     */
    public void addScanResult(ScanResult result)
    {
        addResult(new ScanResultWrapper(getWidth() / 2, getHeight() / 2, result), -1);

        viewUpdater.redraw(true);
    }

    /**
     * Removes a ScanResult from this ScannerView.
     * @param result The ScanResult to remove
     */
    public void removeScanResult(ScanResult result)
    {
        for (int i = scanResults.size() - 1; i >= 0; --i)
        {
            if (scanResults.get(i).SCAN_RESULT.equals(result))
            {
                removeResult(i);
                break;
            }
        }

        if (draggedResult != null && draggedResult.SCAN_RESULT.equals(result))
            draggedResult = null;

        if (connectedResult != null && connectedResult.SCAN_RESULT.equals(result))
        {
            Log.w(TAG, "Lost ScanResult for the connected ball!");
            disconnect(connectedResult.SCAN_RESULT);
            connectedResult = null;
        }

        viewUpdater.redraw(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        boolean r = false;

        switch (e.getActionMasked())
        {
            // Initial finger press
            case MotionEvent.ACTION_DOWN:
                if (draggedResult == null)
                {
                    // Find if a point is pressed
                    ScanResultWrapper result;
                    for (int i = 0; i < scanResults.size() && draggedResult == null; ++i)
                    {
                        result = scanResults.get(i);
                        if (result.containsPoint(e.getX(), e.getY()))
                        {
                            draggedResult = removeResult(i);
                        }
                    }

                    // Remove the point from the list
                    if (draggedResult != null)
                    {
                        draggedResult.pickup();
                        r = true;
                        viewUpdater.redraw(true);
                    }
                    else if (connectedResult != null && connectedResult.containsPoint(e.getX(), e.getY()))
                    {
                        connectedResult.pickup();
                        moveConnected = true;
                        viewUpdater.redraw(true);
                        r = true;
                    }
                }
                break;

            // Final finger release
            case MotionEvent.ACTION_UP:
                if (draggedResult != null)
                {
                    // Check for drops in connect region
                    if (isInConnectRegion(draggedResult))
                    {
                        // Check for displacing existing connections
                        if (connectedResult != null)
                        {
                            disconnect(connectedResult.SCAN_RESULT);
                            insertResult(connectedResult);
                        }

                        // Set the new connectedResult
                        connectedResult = draggedResult;
                        connectedResult.setPosition(getWidth() / 2, getHeight() / 2);
                        connect(connectedResult.SCAN_RESULT);
                    }
                    else
                    {
                        // Drop off on periphery
                        insertResult(draggedResult);
                    }

                    draggedResult.drop();
                    draggedResult = null;
                    viewUpdater.redraw(true);
                }
                else if (connectedResult != null)
                {
                    // User has let go of the connectedResult. Either wants to disconnect or stay connected
                    if (moveConnected)
                    {
                        if (isInConnectRegion(connectedResult))
                        {
                            // Stay connected
                            connectedResult.setPosition(getWidth() / 2, getHeight() / 2);
                            connectedResult.drop();
                        }
                        else
                        {
                            // Disconnect
                            disconnect(connectedResult.SCAN_RESULT);
                            insertResult(connectedResult);
                            connectedResult.drop();
                            connectedResult = null;
                            viewUpdater.redraw(true);
                        }
                    }

                    moveConnected = false;
                }
                break;

            // Finger move
            case MotionEvent.ACTION_MOVE:
                // Move the dragged point
                if (draggedResult != null)
                {
                    draggedResult.forcePosition(e.getX(), e.getY());
                    r = true;
                    viewUpdater.redraw(true);
                }
                else if (connectedResult != null && moveConnected)
                {
                    connectedResult.forcePosition(e.getX(), e.getY());
                    r = true;
                    viewUpdater.redraw(true);
                }
                break;
        }

        return r;
    }

    /*
     * Adds a ScanResultWrapper.
     */
    private void addResult(ScanResultWrapper result, int idx)
    {
        rotOffset = 180.0f / (scanResults.size() + 1);

        synchronized (scanResults)
        {
            if (idx == -1)
                scanResults.add(result);
            else
                scanResults.add(idx, result);
        }
    }

//    /*
//     * Removes a ScanResultWrapper.
//     */
//    private void removeResult(ScanResultWrapper result)
//    {
//        synchronized (scanResults)
//        {
//            scanResults.remove(result);
//        }
//
//        rotOffset = 180.0f / scanResults.size();
//    }

    /*
     * Removes and returns the ScanResultWrapper at the specified index.
     */
    private ScanResultWrapper removeResult(int idx)
    {
        rotOffset = 180.0f / (scanResults.size() - 1);

        synchronized (scanResults)
        {
            return scanResults.remove(idx);
        }
    }

    /*
     * Inserts a ScanResultWrapper into the list.
     */
    private void insertResult(ScanResultWrapper draggedResult)
    {
        // Otherwise place somewhere on the periphery
        if (scanResults.size() > 1)
        {
            // Try to insert it such that it is in the right spot
            float min1, min2 = min1 = Float.MAX_VALUE;
            float dist;
            int idx1 = 0, idx2 = 1;
            ScanResultWrapper result;

            for (int i = 0; i < scanResults.size(); ++i)
            {
                result = scanResults.get(i);
                dist = Utils.distanceSquared(draggedResult.x, draggedResult.y, result.x, result.y);

                if (dist < min1)
                {
                    min2 = min1;
                    idx2 = idx1;

                    min1 = dist;
                    idx1 = i;
                }
                else if (dist < min2)
                {
                    min2 = dist;
                    idx2 = i;
                }
            }

            if (Math.abs(idx1 - idx2) == scanResults.size() - 1)
                addResult(draggedResult, 0);
            else
                addResult(draggedResult, Math.max(idx1, idx2));
        }
        else
        {
            addResult(draggedResult, -1);
        }
    }

    private boolean isInConnectRegion(ScanResultWrapper scanResult)
    {
        return (Utils.distanceSquared(getWidth() / 2, getHeight() / 2, scanResult.x, scanResult.y)
                < innerRadius * innerRadius + scanResultRadius * scanResultRadius);
    }

    /*
     * Draws the background.
     */
    private void drawBackground(Canvas canvas)
    {
        // Draw the main circle
        PAINT.setColor(donutColor);
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(donutWidth);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, PAINT);
        PAINT.setStyle(Paint.Style.FILL);

        // Draw the inner circle
        PAINT.setColor(donutColor);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerRadius, PAINT);
    }

    /**
     * Wrapper for ScanResults.
     */
    private class ScanResultWrapper implements Parcelable
    {
        /** The wrapped ScanResult */
        public final ScanResult SCAN_RESULT;

        // Position and destination position
        private float x, y, dstX, dstY;

        // Whether this ScanResultWrapper is being moved
        private boolean isCarried;

        // Rect for drawing the SmartBall icon
        private final Rect RECT;

        // Background color, based on rssi
        private int color;

        // Move amount for moving towards the destination point
        private static final float MOVE_AMT = 0.1f;

        // Distance when considered at the destination
        private static final float ZERO_DIST = 4.0f;

        /**
         * Creator for ScanResultWrappers.
         */
        @SuppressWarnings("unused")
        public final Parcelable.Creator<ScanResultWrapper> CREATOR
                = new Parcelable.Creator<ScanResultWrapper>() {
            public ScanResultWrapper createFromParcel(Parcel in) {
                return new ScanResultWrapper(in);
            }

            public ScanResultWrapper[] newArray(int size) {
                return new ScanResultWrapper[size];
            }
        };


        /**
         * Creates a ScanResultWrapper for the specified ScanResult.
         * @param x The x position
         * @param y The y position
         * @param scanResult The ScanResult
         */
        public ScanResultWrapper(float x, float y, @NonNull ScanResult scanResult)
        {
            SCAN_RESULT = scanResult;
            RECT = new Rect();

            this.x = x;
            this.y = y;

            int level = WifiManager.calculateSignalLevel(scanResult.getRssi(), 8);
            color = 0xFF0000 >> level;
        }

        /*
         * Creates a ScanResultWrapper from a Parcel.
         */
        private ScanResultWrapper(Parcel in)
        {
            SCAN_RESULT = in.readParcelable(ScanResult.class.getClassLoader());
            RECT = new Rect();

            float[] data = new float[4];
            in.readFloatArray(data);
            x = data[0];
            y = data[1];
            dstX = data[2];
            dstY = data[3];
            color = in.readInt();
        }

        @Override
        public boolean equals(Object other)
        {
            return other instanceof ScanResultWrapper
                    && ((ScanResultWrapper) other).SCAN_RESULT.equals(SCAN_RESULT);
        }

        /**
         * Updates this ScanResultWrapper.
         * @param canvas The Canvas on which to draw
         * @return True to indicate that further calls to update are needed or false if otherwise
         */
        public boolean update(Canvas canvas)
        {
            final float r = scanResultRadius * (isCarried ? 1.5f: 1.0f);

            PAINT.setColor(0x88000000 | (color & 0x00FFFFFF));
            canvas.drawCircle(x, y, r * 1.1f, PAINT);

            RECT.set((int) (x - r * 0.9f), (int) (y - r * 0.9f), (int) (x + r * 0.9f), (int) (y + r * 0.9f));

            canvas.drawBitmap(smartBallBmp, null, RECT, null);

            // Draw the device name
            PAINT.setColor(0xFF000000);
            PAINT.setTextAlign(Paint.Align.CENTER);
            PAINT.setTextSize(textSize * (isCarried ? 1.5f: 1.0f));
            canvas.drawText(toString(), x, y - r * 1.25f, PAINT);

            return updatePosition();
        }

        /**
         * Called when this ScanResultWrapper is picked up.
         */
        public void pickup()
        {
            isCarried = true;
        }

        /**
         * Called when this ScanResultWrapper is dropped.
         */
        public void drop()
        {
            isCarried = false;
        }

        /**
         * Forces the position of this ScannerView.
         * @param x The new x position
         * @param y The new y position
         */
        public void forcePosition(float x, float y)
        {
            this.x = x;
            this.y = y;

            dstX = x;
            dstY = y;
        }

        /**
         * Sets the next position for this ScannerView to move to.
         * @param x The new x position
         * @param y The new y position
         */
        public void setPosition(float x, float y)
        {
            dstX = x;
            dstY = y;
        }

        /**
         * Tests whether this ScannerView contains the specified point.
         * @param x The points x position
         * @param y The points y position
         * @return True if this ScanResultWrapper contains the specified point
         */
        public boolean containsPoint(float x, float y)
        {
            return Utils.distanceSquared(this.x, this.y, x, y) < scanResultRadius * scanResultRadius;
        }

//        /**
//         * Tests whether this ScanResultWrapper has assumed its position on the edge of the circle.
//         * @param cx The center x position
//         * @param cy The center y position
//         * @param scanResultRadius The scanResultRadius of the circle
//         * @return Whether this ScanResultWrapper is on the edge of the circle
//         */
//        public boolean isPeripheral(float cx, float cy, float scanResultRadius)
//        {
//            return Utils.distanceSquared(x, y, cx, cy)
//                    > (scanResultRadius + ScanResultWrapper.scanResultRadius) * (scanResultRadius + ScanResultWrapper.scanResultRadius);
//        }

//        /**
//         * Returns the peripheryness of this ScanResultWrapper.
//         * @param cx The center x position
//         * @param cy The center y position
//         * @param scanResultRadius The scanResultRadius of the circle
//         * @return A value in the range [0, 1ish] where 1 indicates being on the periphery
//         */
//        public float getPeripheralFactor(float cx, float cy, float scanResultRadius)
//        {
//            return Utils.distance(x, y, cx, cy) / scanResultRadius;
//        }

        /**
         * Describe the kinds of special objects contained in this Parcelable's
         * marshalled representation.
         *
         * @return a bitmask indicating the set of special object types marshalled
         * by the Parcelable.
         */
        @Override
        public int describeContents()
        {
            return 0;
        }

        /**
         * Flatten this object in to a Parcel.
         *
         * @param dest  The Parcel in which the object should be written.
         * @param flags Additional flags about how the object should be written.
         */
        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeParcelable(SCAN_RESULT, 0);
            dest.writeFloatArray(new float[] {x, y, dstX, dstY});
            dest.writeInt(color);
        }

        @Override
        public String toString()
        {
            if (SCAN_RESULT.getDevice() == null)
                return "Unknown Device";
            else
                return SCAN_RESULT.getDevice().getName();
        }

        /*
         * Updates the position of this ScanResultWrapper
         */
        private boolean updatePosition()
        {
            x = MOVE_AMT * dstX + (1.0f - MOVE_AMT) * x;
            y = MOVE_AMT * dstY + (1.0f - MOVE_AMT) * y;

            if (Utils.distanceSquared(x, y, dstX, dstY) < ZERO_DIST)
            {
                x = dstX;
                y = dstY;
                return false;
            }
            else
                return true;
        }
    }

    /**
     * Interface for listening for events triggered by the ScannerView.
     */
    public interface ScannerViewListener
    {
        /**
         * Called when the user wants to connect to a ScanResult.
         * @param result The ScanResult to connect to
         */
        void onConnectTo(ScanResult result);

        /**
         * Called when the user wants to disconnect from a ScanResult.
         * @param result The ScanResult to disconnect from
         */
        void onDisconnectFrom(ScanResult result);
    }
}
