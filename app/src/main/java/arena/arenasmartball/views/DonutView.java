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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;

/**
 * Custom View for showing and connecting to scan results.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class DonutView extends View// implements SmartBallScanner.SmartBallScannerListener
{
    // ViewUpdater for regulating redraws
    private ViewUpdater viewUpdater;

    // The Set of ScannerViewListeners attached to this ScannerView
    private Set<DonutViewListener> listeners;

    // List of ScanResults
    private final HashMap<String, ScanResultWrapper> scanResults;
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

    /**
     * Required Constructor.
     * @param context The parent Context
     */
    public DonutView(Context context)
    {
        super(context);

        scanResults = new HashMap<>();
        listeners = new HashSet<>();
        init(context, null);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     */
    public DonutView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        scanResults = new HashMap<>();
        listeners = new HashSet<>();
        init(context, attrs);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     * @param defStyle The default style
     */
    public DonutView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        scanResults = new HashMap<>();
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
    public void addDonutViewListener(DonutViewListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Removes a ScannerViewListener from this ScannerView.
     * @param listener The ScannerViewListener to remove
     */
    public void removeDonutViewListener(DonutViewListener listener)
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

        for (DonutViewListener listener: listeners)
            listener.onConnectTo(scanResult);
    }

    /**
     * Called when the user indicates he wishes to disconnect from a ScanResult.
     * @param scanResult The ScanResult from which to disconnect
     */
    public void disconnect(ScanResult scanResult)
    {
        Log.d(TAG, "Disconnect from " + scanResult);

        for (DonutViewListener listener: listeners)
            listener.onDisconnectFrom(scanResult);
    }

    /**
     * Sets the Donut's ScanResults to those specified.
     * @param results The new collection of ScanResults
     */
    public void setScanResults(Collection<ScanResult> results)
    {
        Log.d(TAG, "setScanResults: " + results);

        for (ScanResult result: results)
        {
            if (draggedResult == null || !Utils.areEqual(result, draggedResult.SCAN_RESULT))
            {
                if (connectedResult == null || !Utils.areEqual(result, connectedResult.SCAN_RESULT))
                {
                    if (!scanResults.containsKey(result.getDevice().getAddress()))
                        addResult(new ScanResultWrapper(getWidth() / 2, getHeight() / 2,
                                getWidth() / 2, getHeight() / 2, result));
                }
            }
        }

        viewUpdater.redraw(true);
    }

    /**
     * Loads state from a Bundle.
     * @param bundle The Bundle
     */
    public void load(Bundle bundle)
    {
        scanResults.clear();
        ArrayList<Parcelable> list = bundle.getParcelableArrayList(SCAN_RESULTS_KEY);
        if (list != null)
        {
            Log.d(TAG, "Loading " + list.size() + " ScanResults");
            for (Parcelable p : list)
            {
                addResult((ScanResultWrapper) p);

                ((ScanResultWrapper) p).snapAtFirstChance = true;
            }
        }

//        draggedResult = bundle.getParcelable(DRAGGED_RESULT_KEY);
        connectedResult = bundle.getParcelable(CONNECTED_RESULT_KEY);
//        moveConnected = bundle.getBoolean(MOVE_CONNECTED_KEY);
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

        ArrayList<ScanResultWrapper> parcels = new ArrayList<>(scanResults.values());
        Log.d(TAG, "Saving " + parcels);

        bundle.putParcelableArrayList(SCAN_RESULTS_KEY, parcels);
        bundle.putParcelable(CONNECTED_RESULT_KEY, connectedResult);
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
//        Log.d(TAG, "Updating " + scanResults.size() + " scanResults");
        synchronized (scanResults)
        {
            // Calculate forces
            for (ScanResultWrapper result: scanResults.values())
            {
                result.calculateForces(scanResults.values(), draggedResult, radius);
            }

            // Apply forces
            for (ScanResultWrapper result: scanResults.values())
            {
                result.snapToDonut(false, radius);

                if (result.update(canvas, scanResultRadius, textSize))
                    viewUpdater.redraw(false);
            }
        }

        // Update the connectedResult
        if (connectedResult != null && connectedResult.update(canvas, scanResultRadius, textSize))
            viewUpdater.redraw(false);

        // Update the draggedResult
        if (draggedResult != null && draggedResult.update(canvas, scanResultRadius, textSize))
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

        radius = (Math.min(width, height) - donutWidth) / 2.0f;
        innerRadius = (radius - donutWidth) / 2.0f;
        scanResultRadius = Math.max(12, Math.min(donutWidth / 2, innerRadius));

        if (connectedResult != null)
            connectedResult.forcePosition(getWidth() / 2, getHeight() / 2);

        for (ScanResultWrapper result: scanResults.values())
        {
            result.snapToDonut(true, radius);
            result.cx = getWidth() / 2;
            result.cy = getHeight() / 2;
            result.isCarried = false;
        }

        viewUpdater.redraw(true);
    }

    /**
     * Adds a ScanResult to this ScannerView.
     * @param result The ScanResult to add
     */
    public void addScanResult(ScanResult result)
    {
        addResult(new ScanResultWrapper(getWidth() / 2, getHeight() / 2, getWidth() / 2, getHeight() / 2, result));
        Log.d(TAG, "Added new result");

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
                    for (ScanResultWrapper result: scanResults.values())
                    {
                        if (result.containsPoint(e.getX(), e.getY(), scanResultRadius))
                        {
                            draggedResult = result;
                        }
                    }

                    if (draggedResult != null)
                        scanResults.remove(getKey(draggedResult));

                    // Remove the point from the list
                    if (draggedResult != null)
                    {
                        draggedResult.pickup();
                        r = true;
                        viewUpdater.redraw(true);
                    }
                    else if (connectedResult != null && connectedResult.containsPoint(
                            e.getX(), e.getY(), scanResultRadius))
                    {
                        connectedResult.pickup();
                        moveConnected = true;
                        viewUpdater.redraw(true);
                        r = true;
                    }
                }

                if (r)
                    MainActivity.getCurrent().lockNavigationDrawer(true);

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

                    MainActivity.getCurrent().lockNavigationDrawer(false);
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

                            for (DonutViewListener listener: listeners)
                                listener.onReconnect();
                        }
                        else
                        {
                            // Disconnect
                            disconnect(connectedResult.SCAN_RESULT);
                            insertResult(connectedResult);
                            connectedResult.drop();
                            connectedResult = null;
                        }

                        viewUpdater.redraw(true);
                    }

                    moveConnected = false;

                    MainActivity.getCurrent().lockNavigationDrawer(false);
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
    private void addResult(ScanResultWrapper result)
    {
        synchronized (scanResults)
        {
            scanResults.put(getKey(result), result);
        }
    }

    /*
     * Returns a key for the specified device.
     */
    private String getKey(ScanResultWrapper result)
    {
        if (result.SCAN_RESULT.getDevice() == null)
            return Integer.toString(result.hashCode());
        else
            return result.SCAN_RESULT.getDevice().getAddress();
    }

    /*
     * Inserts a ScanResultWrapper into the list.
     */
    private void insertResult(ScanResultWrapper draggedResult)
    {
        draggedResult.dir = Utils.pointDirection(getWidth() / 2, draggedResult.y, draggedResult.x, getHeight() / 2);
        addResult(draggedResult);
    }

    /*
     * Tests whether a ScanResultWrapper is in the connect region.
     */
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
    private static class ScanResultWrapper implements Parcelable, Comparable<ScanResultWrapper>
    {
        /** The wrapped ScanResult */
        public final ScanResult SCAN_RESULT;

        // Position and destination position
        private float x, y, dstX, dstY;
        private float dir;
        private float fx, fy;
        private float cx, cy;

        private boolean snapAtFirstChance;

        // Whether this ScanResultWrapper is being moved
        private boolean isCarried;

        // Rect for drawing the SmartBall icon
        private final Rect RECT;

        // Background color, based on rssi
        private int color;

        // Move amount for moving towards the destination point
        private static final float MOVE_AMT = 0.1f;

        // Distance when considered at the destination
        private static final float ZERO_DIST = 1.0f;

        /**
         * Creator for ScanResultWrappers.
         */
        @SuppressWarnings("unused")
        public static final Creator<ScanResultWrapper> CREATOR
                = new Creator<DonutView.ScanResultWrapper>() {
            public DonutView.ScanResultWrapper createFromParcel(Parcel in) {
                return new DonutView.ScanResultWrapper(in);
            }

            public DonutView.ScanResultWrapper[] newArray(int size) {
                return new DonutView.ScanResultWrapper[size];
            }
        };


        /**
         * Creates a ScanResultWrapper for the specified ScanResult.
         * @param x The x position
         * @param y The y position
         * @param scanResult The ScanResult
         */
        public ScanResultWrapper(float x, float y, float cx, float cy, @NonNull ScanResult scanResult)
        {
            SCAN_RESULT = scanResult;
            RECT = new Rect();

            this.x = x + (float) (Math.random() - Math.random()) * 32;
            this.y = y + (float) (Math.random() - Math.random()) * 32;

            this.cx = cx;
            this.cy = cy;

            dir = Utils.pointDirection(cx, cy, this.x, this.y);

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

            float[] data = new float[7];
            in.readFloatArray(data);
            x = data[0];
            y = data[1];
            dstX = data[2];
            dstY = data[3];
            dir = data[4];
            cx = data[5];
            cy = data[6];
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
        public boolean update(Canvas canvas, float scanResultRadius, float textSize)
        {
            final float r = scanResultRadius * (isCarried ? 1.5f: 1.0f);

            if (snapAtFirstChance)
            {
                snapAtFirstChance = false;
                x = dstX;
                y = dstY;
            }

            PAINT.setColor(0x88000000 | (color & 0x00FFFFFF));
            canvas.drawCircle(x, y, r * 1.1f, PAINT);

            RECT.set((int) (x - r * 0.9f), (int) (y - r * 0.9f), (int) (x + r * 0.9f), (int) (y + r * 0.9f));

            canvas.drawBitmap(smartBallBmp, null, RECT, null);

            // Draw the device name
            PAINT.setColor(0xFF000000);
            PAINT.setTextAlign(Paint.Align.CENTER);
            PAINT.setTextSize(textSize * (isCarried ? 1.5f: 1.0f));
            canvas.drawText(toString(), x, y - r * 1.25f, PAINT);

            // Force vector
//            PAINT.setColor(0xFFFF0000);
//            PAINT.setStrokeWidth(5.0f);
//            canvas.drawLine(x, y, x + fx * 32, y + fy * 32, PAINT);

            return updatePosition();
        }

        /*
         * Calculates the repulsive force on this SoccerBall from the other SoccerBalls.
         */
        private void calculateForces(Collection<ScanResultWrapper> scanResults, ScanResultWrapper draggedResult, float radius)
        {
            fx = 0.0f;
            fy = 0.0f;

            for (ScanResultWrapper result: scanResults)
            {
                if (this != result)
                {
                    applyForce(result, radius);
                }
            }

            if (draggedResult != null)
                applyForce(draggedResult, radius);

            dir = Utils.pointDirection(cx, y + fy, x + fx, cy);

//            Log.d(TAG, "Direction: " + dir);
        }

        /*
         * Applies the repulsive force of the specified ScanResultWrapper.
         */
        private void applyForce(ScanResultWrapper result, float radius)
        {
            float tx, ty, mag;

            tx = x - result.x;
            ty = y - result.y;
            mag = (float) Math.sqrt(tx * tx + ty * ty);
            if (mag < 1.0f)
                mag = 1.0f;

            tx /= mag;
            ty /= mag;

            tx *= radius * 256.0f;
            ty *= radius * 256.0f;

            fx += tx;
            fy += ty;
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
        public boolean containsPoint(float x, float y, float scanResultRadius)
        {
            return Utils.distanceSquared(this.x, this.y, x, y) < scanResultRadius * scanResultRadius;
        }

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
            dest.writeFloatArray(new float[] {x, y, dstX, dstY, dir, cx, cy});
            dest.writeInt(color);
        }

        /**
         * Compares this object to the specified object to determine their relative
         * order.
         *
         * @param another the object to compare to this instance
         * @return The difference in directions
         */
        @Override
        public int compareTo(@NonNull ScanResultWrapper another)
        {
            return Math.round(Utils.angleDifference(dir, another.dir));
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
         * Makes this ScanResultWrapper move towards the Donut's edge.
         */
        private void snapToDonut(boolean force, float radius)
        {
            dstX = cx + (float) Math.cos(dir * Utils.DEG_2_RAD) * radius;
            dstY = cy + (float) Math.sin(dir * Utils.DEG_2_RAD) * radius;

            if (force)
            {
                x = dstX;
                y = dstY;
            }
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
    public interface DonutViewListener
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

        /**
         * Called when the user wants to onReconnect to the connected ScanResult.
         */
        void onReconnect();
    }
}
