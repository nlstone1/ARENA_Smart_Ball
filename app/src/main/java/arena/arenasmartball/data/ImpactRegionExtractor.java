package arena.arenasmartball.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Contains functionality for identifying and isolating impact regions from Impact Data.
 *
 * Created by Nathaniel on 4/18/2016.
 */
public class ImpactRegionExtractor
{
    // Log Tag String
    private static final String TAG = "ImpactRegionExtractor";

    /**
     * Finds and returns all impact regions in the specified data.
     * @param rdata The ImpactData
     * @return A list containing the impact regions. Will be empty if none are found.
     */
    public static ArrayList<ImpactRegion> findImpactRegions(RawImpactData rdata)
    {
        ArrayList<ImpactRegion> regions = new ArrayList<>();

        if (rdata.getData().size() <= 1)
            return regions;

        // Create local copy of the data to work with
        ArrayList<float[]> data = new ArrayList<>(rdata.getData().size());
        for (Sample sample: rdata.getData())
        {
            data.add(sample.toFloatArray());

//            // T0DO DEBUG
//            Log.d(TAG, "\t" + sample.x * Sample.DATA_TO_GS + "\t" +
//                sample.y * Sample.DATA_TO_GS + "\t" + sample.z * Sample.DATA_TO_GS);
        }

        // Run the NAT filter on the data
        float max = filter(data) / 2.0f;

        // Find the impact regions. Assume any peak larger than half the max is one
        int start = -1;

        for (int i = 0; i < data.size(); ++i)
        {
            if (start == -1) // Look for the start
            {
                // Found the start of an impact regions
                if (data.get(i)[0] > max)
                    start = i - 1;
            }
            else // Look for the end
            {
                if (data.get(i)[0] < max)
                {
                    // Create the ImpactRegion
                    regions.add(new ImpactRegion(start, i));

                    start = -1;
                }
            }
        }

        // Make sure an impact region isn't cutoff
        if (start > -1)
        {
            regions.add(new ImpactRegion(start, data.size() - 1));
        }

        // Adjust the bounds on the found impact regions
        for (ImpactRegion region: regions)
        {
            // Backtrack on start
            for (int i = region.getStart(); i >= 0; --i)
            {
                if (data.get(i)[0] < max / 10.0f)
                {
                    region.start = i;
                    i = -1;
                }
            }

            // Find the end
            float avg;
            int j;
            final int N = 16;
            for (int i = region.getEnd(); i < data.size(); ++i)
            {
                avg = 0.0f;
                for (j = Math.max(0, i - N / 2); j < Math.min(i + N / 2, data.size()); ++j)
                {
                    avg += data.get(i)[0];
                }
                avg /= N;

                if (avg < max / 10.0f)
                {
                    region.end = i;
                    i = data.size();
                }
            }

            region.end += region.getEnd() - region.getStart();
        }

        // Combine overlapping impact regions
        for (int i = regions.size() - 1; i > 0; --i)
        {
            if (regions.get(i - 1).getEnd() >= regions.get(i).getStart())
            {
                regions.get(i - 1).end = regions.get(i).getEnd();
                regions.remove(i);
            }
        }

        return regions;
    }

    /**
     * Filters the data for finding impact regions using the Near-linear Active Transform (NAT) filter.
     * @param data The data to filter.
     * @return The maximum value in the filtered data
     */
    private static float filter(ArrayList<float[]> data)
    {
        float prev[], diff;

        prev = new float[3];
        prev[0] = data.get(0)[0];
        prev[1] = data.get(0)[1];
        prev[2] = data.get(0)[2];

        for (int i = 1; i < data.size() - 1; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                diff = Math.abs(data.get(i)[j] - prev[j]) +
                       Math.abs(data.get(i)[j] - data.get(i + 1)[j]);

                prev[j] = data.get(i)[j];

                data.get(i)[j] *= diff;

                if (data.get(i)[j] < 0.0f)
                    data.get(i)[j] = -data.get(i)[j];
            }
        }

        // Now, run momentum a few times for good measure
        for (int i = 0; i < 5; ++i)
            momentum(data);

        // Isolate the maximum of all three axes to the x component and find the maximum value
        diff = Float.MIN_VALUE;

        for (int i = 0; i < data.size(); ++i)
        {
            prev = data.get(i);
            prev[0] = Math.max(prev[0], Math.max(prev[1], prev[2]));

            if (prev[0] > diff)
                diff = prev[0];
        }

//        // T0DO DEBUG
//        for (float[] arr: data)
//        {
//            Log.d(TAG, "\t" + arr[0] + "\t" + arr[1] + "\t" + arr[2]);
//        }
//        //

        return diff;
    }

    /*
     * Helper function for filter().
     */
    private static void momentum(ArrayList<float[]> data)
    {
        float diff, m[] = {0.0f, 0.0f, 0.0f};

        for (int i = 1; i < data.size(); ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                if (data.get(i)[j] > data.get(i - 1)[j])
                {
                    m[j] += (float) Math.pow(data.get(i)[j] - data.get(i - 1)[j], 1.41);
                }
                else
                {
                    diff = (data.get(i - 1)[j] - data.get(i)[j]);

                    if (m[j] > diff)
                    {
                        data.get(i)[j] += diff;
                        m[j] -= (float) Math.sqrt(diff);
                    }
                }
            }
        }
    }

    /**
     * Container for an impact region in the data.
     */
    public static class ImpactRegion implements Parcelable
    {
        // The start and end points of this ImpactRegion
        private int start, end;

        /**
         * Creates an ImpactRegion.
         * @param start The start point
         * @param end The end point
         */
        public ImpactRegion(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        /**
         * Creates this ImpactRegion from the specified Parcel.
         * @param in The Parcel
         */
        public ImpactRegion(Parcel in)
        {
            int[] intData = new int[2];
            in.readIntArray(intData);

            start = intData[0];
            end = intData[1];
        }

        /**
         * Returns the end index.
         * @return The end index
         */
        public int getEnd()
        {
            return end;
        }

        /**
         * Returns the start index.
         * @return The start index
         */
        public int getStart()
        {
            return start;
        }

        /**
         * Tests whether the given point falls within this ImpactRegion.
         * @param pt The point
         * @return True if pt falls in the range [start, end] and false otherwise
         */
        public boolean collision(int pt)
        {
            return pt >= start && pt <= end;
        }

        /**
         * Creator for ScanResultWrappers.
         */
        public static final Creator<ImpactRegion> CREATOR = new Creator<ImpactRegion>()
        {
            public ImpactRegion createFromParcel(Parcel in) {
                return new ImpactRegion(in);
            }

            public ImpactRegion[] newArray(int size) {
                return new ImpactRegion[size];
            }
        };

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
            dest.writeIntArray(new int[] {start, end});
        }

        @Override
        public String toString()
        {
            return "[" + start + ", " + end + "]";
        }
    }
}
