package arena.arenasmartball.data;

import android.support.annotation.NonNull;

/**
 * Represents a single sample of data.
 * @author Nathaniel Stone
 *
 */
public class Sample
{
	/**
	 * The three components of the Sample
	 */
	public short x, y, z;

	/**
	 * This Sample's time.
	 */
	public double time;

	/**
	 * Constant through which to convert samples to Gs
	 */
	public static final double SAMPLE_TO_G = 0.002;

	/**
	 * The sample period.
	 */
	public static final double SAMPLE_PERIOD = 0.001;

//	// Denotes an unset-time stamp
//	private static double NULL_TIME = Double.MIN_VALUE;

	/**
	 * Default Constructor.
	 */
	public Sample()
	{
		x = y = z = 0;
		time = 0.0;
	}

	/**
	 * Creates a Sample from the specified components and time.
	 */
	public Sample(double time, short x, short y, short z)
	{
		this.time = time;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Creates a Sample from the specified components.
	 */
	public Sample(double time, int x, int y, int z)
	{
		this(time, (short) x, (short) y, (short) z);
	}

//	/**
//	 * Creates A Sample from the specified components in Gs.
//	 */
//	public Sample(double x, double y, double z)
//	{
//		this ((short) (x / SAMPLE_TO_G), (short) (y / SAMPLE_TO_G), (short) (z / SAMPLE_TO_G));
//	}

//	/**
//	 * Sets the time stamp on this Sample.
//	 * @param time The time
//	 */
//	public void setTime(double time)
//	{
//		this.time = time;
//	}

//	/**
//	 * @return This Sample's time stamp
//	 */
//	public double getTime()
//	{
////		if (!hasTimeStamp())
////			throw new IllegalArgumentException("No timestamp set for this Sample!");
////
//		return time;
//	}

//	/**
//	 * @return Tests whether this Sample's time stamp has been set.
//	 */
//	public boolean hasTimeStamp()
//	{
//		return time != NULL_TIME;
//	}

//	/**
//	 * Extracts this sample into 3 Little Endian bytes in the passed array.
//	 * @param out The array in which to put the byte values
//	 */
//	public void toBytes(byte[] out)
//	{
//		out[1] = (byte) ((x >> 8) & 0xFF);
//		out[0] = (byte) (x & 0xFF);
//		out[3] = (byte) ((y >> 8) & 0xFF);
//		out[2] = (byte) (y & 0xFF);
//		out[5] = (byte) ((z >> 8) & 0xFF);
//		out[4] = (byte) (z & 0xFF);
//	}

	/**
	 * Returns this Sample as a float array in G's.
	 *
	 * @return This Sample as a float array in G's
	 */
	public float[] toFloatArray()
	{
		return new float[]{(float) (x * SAMPLE_TO_G), (float) (y * SAMPLE_TO_G), (float) (z * SAMPLE_TO_G)};
	}

	/**
	 * Puts this Sample into a float array in G's.
     */
	public void toFloatArray(@NonNull float[] sample)
	{
		sample[0] = (float)(x * SAMPLE_TO_G);
		sample[1] = (float)(y * SAMPLE_TO_G);
		sample[2] = (float)(z * SAMPLE_TO_G);
	}

    /**
     * Puts this Sample into a double array in G's.
     */
    public void toDoubleArray(double[] sample)
    {
        sample[0] = x * SAMPLE_TO_G;
        sample[1] = y * SAMPLE_TO_G;
        sample[2] = z * SAMPLE_TO_G;
    }

	/**
	 * Sets this Sample from the specified byte array.
	 * @param in The array from which to read the byte values
	 */
	public void fromBytes(byte[] in)
	{
		x = (short) ((in[0] & 0xFF) | ((short) in[1]) << 8);
		y = (short) ((in[2] & 0xFF) | ((short) in[3]) << 8);
		z = (short) ((in[4] & 0xFF) | ((short) in[5]) << 8);
	}
	
	@Override
	public String toString()
	{
		return time + ", "+ (x * SAMPLE_TO_G) + ", " + (y * SAMPLE_TO_G) + ", " + (z * SAMPLE_TO_G);
	}

	public String toRawString()
	{
		return time + ", "+ x + ", " + y + ", " + z;
	}
}
