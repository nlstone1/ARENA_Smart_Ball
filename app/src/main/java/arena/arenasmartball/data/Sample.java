package arena.arenasmartball.data;

/**
 * Wrapper class for a sample of data. Sample wraps three spatial components stored as signed 16 bit values.
 */
public class Sample
{
    /** The conversion from raw data values to gs (1 g = 9.81 m/s^2) */
    public static final float DATA_TO_GS = 0.002f;

    /** The x, y, and z components of the Sample. */
    public short x, y, z;

    /**
     * Constructs a new Sample.
     * @param x The x component
     * @param y The y component
     * @param z The z component
     */
    public Sample(short x, short y, short z)
    {
        this.x = x; this.y = y; this.z = z;
    }

    /**
     * Returns this Sample as a float array in G's.
     * @return This Sample as a float array in G's
     */
    public float[] toFloatArray()
    {
        return new float[] {x * DATA_TO_GS, y * DATA_TO_GS, z * DATA_TO_GS};
    }

    /**
     * Same as above but the specified float array is used.
     * @param arr The float array. Must be of length at least 3
     */
    public void toFloatArray(float[] arr)
    {
        arr[0] = x * DATA_TO_GS;
        arr[1] = y * DATA_TO_GS;
        arr[2] = z * DATA_TO_GS;
    }

    /**
     * Method to get a String representation of this Sample.
     * @return A String representation of this Sample
     */
    @Override
    public String toString()
    {
        // Scale values to g's
        double xg = x * DATA_TO_GS;
        double yg = y * DATA_TO_GS;
        double zg = z * DATA_TO_GS;
        return xg + "," + yg + "," + zg;
    }
}