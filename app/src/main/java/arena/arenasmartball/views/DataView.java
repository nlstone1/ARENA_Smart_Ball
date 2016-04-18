package arena.arenasmartball.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.data.RawImpactData;
import arena.arenasmartball.data.Sample;

/**
 * View for showing impact data graphically.
 *
 * Created by Nathaniel on 4/17/2016.
 */
public class DataView extends View
{
    // Static Paint for painting
    private static final Paint PAINT = new Paint();

    // Plus and minus G extents for the graph
    private static final float G_RANGE = 5.0f;

    // X and Y scales
    private float xScale, yScale;

    // Updater for controlling invalidations
    private ViewUpdater viewUpdater;

    // Width of a tick-mark on the graph
    private static final float TICKMARK_WIDTH = 8.0f;

    // Padding for drawing the graph
    private float padding;
    // Default padding
    private static final float DEF_PADDING = 0.0f;

    // Curve colors
    private static final int[] COLORS = {Color.RED, Color.GREEN, Color.BLUE};
    // Number of curves
    private static final int NUM_CURVES = 3;

    // Temporary arrays used for drawing the curves
    private float[] pt, prevPt;

    // Draws every nth point
    private static final int N = 2;

    // Log tag String
    private static final String TAG = "DataView";

    /**
     * Required Constructor.
     * @param context The parent Context
     */
    public DataView(Context context)
    {
        super(context);

        init(context, null);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     */
    public DataView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        init(context, attrs);
    }

    /**
     * Required Constructor.
     * @param context The parent Context
     * @param attrs The AttributeSet
     * @param defStyle The default style
     */
    public DataView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        init(context, attrs);
    }

    /*
     * Called from the three Constructors.
     */
    private void init(Context context, AttributeSet attrs)
    {
        if (attrs != null)
        {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.DataView, 0, 0);

            try
            {
                padding = a.getDimension(R.styleable.DataView_padding, DEF_PADDING);
            }
            finally
            {
                a.recycle();
            }
        }
        else
        {
            padding = DEF_PADDING;
        }

        viewUpdater = new ViewUpdater(this, 33L);
        viewUpdater.start();

        pt = new float[NUM_CURVES];
        prevPt = new float[NUM_CURVES];
    }

    /**
     * Called when the view is rendered.
     * @param canvas The Canvas on which to draw
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // Draw the axes
        drawAxes(canvas);

        // Draw the data
        RawImpactData data = getDataToDraw();

        if (data != null)
        {
            drawData(canvas, data);
        }
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

        // Set the X scale
        RawImpactData data = getDataToDraw();

        if (data == null)
        {
            xScale = 1.0f;
        }
        else
        {
            xScale = (w - 2.0f * (padding + 2.0f)) / (data.getNumSamplesAskedFor());
        }

        // Set the Y scale
        yScale = (h - 2.0f * (padding + 2.0f)) / (2.0f * G_RANGE);
    }

    /**
     * Returns this View's ViewUpdater.
     * @return The ViewUpdater
     */
    public ViewUpdater getViewUpdater()
    {
        return viewUpdater;
    }

    /*
     * Draws the Graph's axes.
     */
    private void drawAxes(Canvas canvas)
    {
        PAINT.setColor(Color.BLACK);
        PAINT.setStrokeWidth(4.0f);
        PAINT.setAntiAlias(true);

        // Y axis
        canvas.drawLine(padding, padding, padding, getHeight() - padding, PAINT);

        // Draw the vertical tick marks
        for (int i = 0; i <= (int) G_RANGE; ++i)
        {
            canvas.drawLine(padding, getHeight() / 2 + i * yScale, padding + TICKMARK_WIDTH,
                    getHeight() / 2 + i * yScale, PAINT);
            canvas.drawLine(padding, getHeight() / 2 - i * yScale, padding + TICKMARK_WIDTH,
                    getHeight() / 2 - i * yScale, PAINT);
        }

        // X axis
        canvas.drawLine(padding, getHeight() / 2, getWidth() - padding, getHeight() / 2, PAINT);
    }

    /*
     * Draws the specified RawImpactData.
     */
    private void drawData(Canvas canvas, @NonNull RawImpactData data)
    {
        List<Sample> samples = data.getData();

        if (samples.size() > 1)
        {
            PAINT.setStrokeWidth(4.0f);

            // Reset the scale // TODO shouldn't have to happen every frame
            xScale = (getWidth() - 2.0f * (padding + 2.0f)) / (data.getNumSamplesAskedFor());

            // Initialize prevPt
            prevPt[0] = samples.get(0).x * Sample.DATA_TO_GS;
            prevPt[1] = samples.get(0).y * Sample.DATA_TO_GS;
            prevPt[2] = samples.get(0).z * Sample.DATA_TO_GS;

            // Draw the curves
            for (int i = N; i < samples.size(); i += N)
            {
                pt[0] = samples.get(i).x * Sample.DATA_TO_GS;
                pt[1] = samples.get(i).y * Sample.DATA_TO_GS;
                pt[2] = samples.get(i).z * Sample.DATA_TO_GS;

                // Draw the line segment
                for (int j = 0; j < NUM_CURVES; ++j)
                {
                    PAINT.setColor(COLORS[j]);
                    canvas.drawLine((i - N) * xScale + padding, prevPt[j] * yScale + getHeight() / 2,
                            i * xScale + padding, pt[j] * yScale + getHeight() / 2, PAINT);

                    // Set the previous point
                    prevPt[j] = pt[j];
                }
            }
        }
    }

    /*
     * Returns the Data to draw or null.
     * // TODO won't always want Type 2 Data
     */
    private static RawImpactData getDataToDraw()
    {
        if (MainActivity.getBluetoothBridge().getLastImpact() != null)
            return MainActivity.getBluetoothBridge().getLastImpact().getTypeTwoData();
        else
            return null;
    }

}
