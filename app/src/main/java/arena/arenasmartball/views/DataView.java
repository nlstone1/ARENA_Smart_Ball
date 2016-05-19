package arena.arenasmartball.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.correlation.Correlator;
import arena.arenasmartball.correlation.CorrelatorMLR;
import arena.arenasmartball.correlation.FeatureExtractor;
import arena.arenasmartball.correlation.SensorData;
import arena.arenasmartball.data.ImpactRegionExtractor;
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

    // Impact regions
    private ArrayList<ImpactRegionWrapper> impactRegions;
    private boolean requestedImpactRegions;

    // Bundle ids
    private static final String IMPACT_REGIONS_BUNDLE_ID = "arena.arenasmartball.views.DataView.impactRegions";
    private static final String REQUESTED_IMPACT_REGIONS_BUNDLE_ID = "arena.arenasmartball.views.DataView.requestedImpactRegions";

    // Draws every nth point
    private static final int N = 2;

    // TODO temporary just for DDay
    private static DataView dataView;

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

        dataView = this;
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

            // Check for finished transmission
            if (data.isComplete() && !requestedImpactRegions)
            {
                requestedImpactRegions = true;

                // Run the region extractor
                new RegionExtractor().execute(data);
            }
            else if (!data.isComplete())
            {
                requestedImpactRegions = false;
                impactRegions = null;
            }

            // Draw impact regions
            if (impactRegions != null)
            {
                drawImpactRegions(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Test for impact region clicks
        if (impactRegions != null && event.getActionIndex() == MotionEvent.ACTION_DOWN)
        {
            int idx = (int) (event.getX() / xScale);

            for (ImpactRegionWrapper region: impactRegions)
            {
                if (region.impactRegion.collision(idx))
                {
                    Log.d(TAG, "Clicked region " + region);
                    region.requestForce();
                    break;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    public void load(Bundle bundle)
    {
        this.requestedImpactRegions = bundle.getBoolean(REQUESTED_IMPACT_REGIONS_BUNDLE_ID, false);

        ArrayList<Parcelable> list = bundle.getParcelableArrayList(IMPACT_REGIONS_BUNDLE_ID);

        if (list != null)
        {
            impactRegions = new ArrayList<>();
            for (Parcelable p : list)
            {
                impactRegions.add((ImpactRegionWrapper) p);
            }
        }
    }

    public void save(Bundle bundle)
    {
        bundle.putBoolean(REQUESTED_IMPACT_REGIONS_BUNDLE_ID, requestedImpactRegions);
        bundle.putParcelableArrayList(IMPACT_REGIONS_BUNDLE_ID, impactRegions);
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
     * Draws the impact regions.
     */
    private void drawImpactRegions(Canvas canvas)
    {
        PAINT.setStrokeWidth(4.0f);
        PAINT.setColor(Color.WHITE);

        PAINT.setTextSize(32.0f);

        ImpactRegionExtractor.ImpactRegion r;

        float x;

        for (ImpactRegionWrapper rw: impactRegions)
        {
            r = rw.impactRegion;
            canvas.drawLine(r.getStart() * xScale, padding, r.getStart() * xScale, getHeight() - padding, PAINT);
            canvas.drawLine(x = (r.getEnd() * xScale), padding, r.getEnd() * xScale, getHeight() - padding, PAINT);

            if (rw.forceReceived)
            {
//                canvas.drawText("HardSoft: " + rw.hardSoft, x + 16.0f, padding + 48.0f, PAINT);
//                canvas.drawText("HitDrop: " + rw.hitDrop, x + 16.0f, padding + 72.0f, PAINT);
//                boolean hit = rw.hitDrop < 0.5f;
//                boolean hard = rw.hardSoft > 0.5f;
//
//                float hitP, hardP;
//
//                if (hit)
//                    hitP = (1.0f - rw.hitDrop) * 100.0f;
//                else
//                    hitP = rw.hitDrop * 100.0f;
//
//                if (hard)
//                    hardP = rw.hardSoft * 100.0f;
//                else
//                    hardP = (1.0f - rw.hardSoft) * 100.0f;

//                canvas.drawText((hit ? "Hit": "Drop") + " (certainty: " + (int) hitP + " %)", x + 16.0f,
//                        padding + 48.0f, PAINT);
//                canvas.drawText((hard ? "Hard": "Soft") + " (certainty: " + (int) hardP + " %)", x + 16.0f,
//                        padding + 96.0f, PAINT);
//                canvas.drawText((hard ? "Hard": "Soft") + " " + (hit ? "Hit": "Drop") + " (" + (int) (hitP * hardP / 100.0f) + "%)",
//                        x + 16.0F, padding + 48.0f, PAINT);

                canvas.drawText("Force = " + rw.force + " N",
                        x + 16.0F, padding + 48.0f, PAINT);
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

    /*
     * Async task for doing region extraction.
     */
    private class RegionExtractor extends AsyncTask<RawImpactData, Void, ArrayList<ImpactRegionExtractor.ImpactRegion>>
    {
        @Override
        protected ArrayList<ImpactRegionExtractor.ImpactRegion> doInBackground(RawImpactData... params)
        {
            if (params.length == 0)
            {
                Log.w(TAG, "No ImpactData provided to DataView for region extraction!");
                return new ArrayList<>(0);
            }

            return ImpactRegionExtractor.findImpactRegions(params[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<ImpactRegionExtractor.ImpactRegion> impactRegions)
        {
            DataView.this.impactRegions = new ArrayList<>(impactRegions.size());

            for (ImpactRegionExtractor.ImpactRegion region: impactRegions)
            {
                DataView.this.impactRegions.add(new ImpactRegionWrapper(region));
            }

            Log.d(TAG, "Found " + impactRegions.size() + " Impact Regions");

            invalidate();
        }
    }

    /**
     * Wrapper for an impact region in the data.
     */
    public static class ImpactRegionWrapper implements Parcelable
    {
        // The Impact Region
        private ImpactRegionExtractor.ImpactRegion impactRegion;

        // The force
        private float force;

//        // hit / drop
//        private float hitDrop;
//        // hard / soft
//        private float hardSoft;

        // Whether the force has been requested
        private boolean forceRequested;
        // Whether the force has been received
        private boolean forceReceived;

        /**
         * Creates an ImpactRegionWrapper.
         * @param impactRegion The wrapped ImpactRegion
         */
        public ImpactRegionWrapper(ImpactRegionExtractor.ImpactRegion impactRegion)
        {
            this.impactRegion = impactRegion;
        }

        /**
         * Creates this ImpactRegion from the specified Parcel.
         * @param in The Parcel
         */
        public ImpactRegionWrapper(Parcel in)
        {
            this.impactRegion = in.readParcelable(ImpactRegionExtractor.ImpactRegion.class.getClassLoader());

            float[] arr = new float[3];
            in.readFloatArray(arr);
            force = arr[0];
//            hardSoft = arr[1];
//            hitDrop = arr[2];
            byte[] bytes = new byte[2];
            in.readByteArray(bytes);
            forceRequested = bytes[0] == 1;
            forceReceived = bytes[1] == 1;

//            Log.d(TAG, "Hard Soft: " + hardSoft);
//            Log.d(TAG, "Hit Drop: " + hitDrop);
        }

        /**
         * Creator for ImpactRegionWrappers.
         */
        public static final Creator<ImpactRegionWrapper> CREATOR = new Creator<ImpactRegionWrapper>()
        {
            public ImpactRegionWrapper createFromParcel(Parcel in)
            {
                return new ImpactRegionWrapper(in);
            }

            public ImpactRegionWrapper[] newArray(int size)
            {
                return new ImpactRegionWrapper[size];
            }
        };

        /**
         * Requests the force for this ImpactRegion to be calculated.
         */
        public void requestForce()
        {
            if (!forceRequested)
            {
                forceRequested = true;
                forceReceived = false;

                new AsyncTask<ImpactRegionExtractor.ImpactRegion, Void, Double>()
                {
                    /**
                     * Performs the force calculation on the background Thread.
                     *
                     * @param params The parameters of the task.
                     * @return A result, defined by the subclass of this task.
                     */
                    @Override
                    protected Double doInBackground(ImpactRegionExtractor.ImpactRegion... params)
                    {
                        int l = params[0].getEnd() - params[0].getStart() + 1;
                        final double[] x = new double[l];
                        final double[] y = new double[l];
                        final double[] z = new double[l];
                        float[] sample = new float[3];
                        RawImpactData data = getDataToDraw();

                        if (data == null)
                        {
                            Log.e(TAG, "No data to calculate the force for!");
                            return null;
                        }

                        for (int i = 0; i < l; ++i)
                        {
                            data.getData().get(i + params[0].getStart()).toFloatArray(sample);

                            x[i] = sample[0];
                            y[i] = sample[1];
                            z[i] = sample[2];
                        }

                        return CorrelatorMLR.evaluate(new FeatureExtractor.DataSeriesFeaturable()
                        {
                            /**
                             * Gets an array of SensorData objects for each axis of data of this Featurable.
                             *
                             * @return An array of SensorData objects for each axis of data of this Featurable
                             */
                            @Override
                            public SensorData[] getAxes()
                            {
                                return new SensorData[] {new SensorData(x), new SensorData(y), new SensorData(z)};
                            }
                        });
                    }

                    @Override
                    protected void onPostExecute(Double vals)
                    {
                        if (vals != null)
                        {
//                            hardSoft = (float) vals[0];
//                            hitDrop = (float) vals[1];
                            force = vals.floatValue();
                            forceReceived = true;
                        }

                        Log.d(TAG, "Force is: " + force + " N");
//                        Log.d(TAG, "Hard Soft: " + hardSoft);
//                        Log.d(TAG, "Hit Drop: " + hitDrop);

                        if (dataView != null)
                        {
                            dataView.postInvalidate();
                        }
                    }
                }.execute(impactRegion);
            }
            else
            {
                Log.d(TAG, "Force is: " + force + " N");
            }
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
         *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
         */
        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeParcelable(impactRegion, 0);
//            dest.writeFloatArray(new float[] {force, hardSoft, hitDrop});
            dest.writeFloatArray(new float[] {force, 0.0f, 0.0f});
            dest.writeByteArray(new byte[] {(byte) (forceRequested ? 1: 0), (byte) (forceReceived ? 1: 0)});
        }
    }
}
