package arena.arenasmartball.data;

/**
 * Class containing the compression algorithm for type 2 data.
 * @author Nathaniel Stone
 * @author Theodore Stone
 *
 */
public class DataDecompressor
{
    // The callback attached to this DataDecompressor
    private DecompressedDataCallback callback;

    // Records the number of Samples that have been decompressed
    private int numSamplesCreated;

    // The previous Sample
    private Sample previousSample;

//	// The List of samples comprising this CompressedData
//	private LinkedList<CompressedSample> samples;
	
	/*
	 * Constructs a new DataDecompressor.
	 * @param callback The callback to attach to the DataDecompressor
	 */
	public DataDecompressor(DecompressedDataCallback callback)
	{
//		samples = new LinkedList<>();
        previousSample = new Sample();
        this.callback = callback;
	}
	
//	/**
//	 * Writes this CompressedData in a human readable format, with 20 values per line.
//	 * @param dst The destination File
//	 */
//	public void toTextFile(File dst)
//	{
//		try
//		{
//			PrintWriter out = new PrintWriter(dst);
//
//			CompressedSample c1, c2, c3; // 3 per line
//			byte flags;
//
//			for (int i = 0; i < samples.size(); i += 3)
//			{
//				c1 = safeGet(i);
//				c2 = safeGet(i + 1);
//				c3 = safeGet(i + 2);
//
//				// Output sequence number
//				out.print((byte) ((i / 3) & 0xFF) + " "); // Only the lower 8 bits
//
//				// Output compressed flags
//				flags = (byte) (c1.getFlag(7) | c2.getFlag(6) | c3.getFlag(5));
//				out.print(flags + " "); // The other 5 bits should hold higher bits of the
//									    // sequence number based on the decompressor but
//										// that doesn't seem to be the case from sample data
//
//				// The remaining 18 values come from the three CompressedSamples
//				out.println(c1 + " " + c2 + " " + c3);
//			}
//
//			out.close();
//		}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("Could not find file to which to write! (" + e.getMessage() + ")");
//		}
//	}

    /**
     * Adds a line of compressed data to this DataDecompressor.
     */
    public void addLine(byte[] data)
    {
        // Each line of data contains 3 CompressedSamples
        Sample sample;
        CompressedSample csample;

        for (int i = 0; i < 3; ++i)
        {
            // Create the compressed sample
            csample = new CompressedSample(((data[1] & (0b1000_0000 >>> i)) == (0b1000_0000 >>> i))
                    , 2 + i * 6, data);

            // Decompress the sample
            if (csample.isCompressed()) // csample contains offsets for two samples
            {
                sample = new Sample((numSamplesCreated++) * Sample.SAMPLE_FREQUENCY, previousSample.x + csample.DATA[0], previousSample.y + csample.DATA[1], previousSample.z + csample.DATA[2]);
//                sample.setTime();
                callback.onNewSample(sample);

                sample = new Sample((numSamplesCreated++) * Sample.SAMPLE_FREQUENCY, sample.x + csample.DATA[3], sample.y + csample.DATA[4], sample.z + csample.DATA[5]);
//                sample.setTime((numSamplesCreated++) * Sample.SAMPLE_FREQUENCY);
                callback.onNewSample(sample);
            }
            else // csample contains a single Sample
            {
                sample = csample.toSample();
                sample.time = (numSamplesCreated++) * Sample.SAMPLE_FREQUENCY;
                callback.onNewSample(sample);
            }

            previousSample.x = sample.x;
            previousSample.y = sample.y;
            previousSample.z = sample.z;
        }
    }
	
//	/**
//	 * Decompresses this CompressedData.
//	 * @return The decompressed data
//	 */
//	public ImpactData decompress()
//	{
//		ArrayList<Sample> samples = new ArrayList<>();
//		ImpactData data = new ImpactData(samples);
//
//		short xp, yp, zp = yp = xp = 0;
//		Sample s;
//
//		// Extract all compressed data
//		for (CompressedSample csample: this.samples)
//		{
//			if (csample.isCompressed()) // csample contains offsets for two samples
//			{
//				s = new Sample(xp + csample.DATA[0], yp + csample.DATA[1], zp + csample.DATA[2]);
//                s.setTime(samples.size() * Sample.SAMPLE_FREQUENCY);
//				samples.add(s);
//
//				s = new Sample(s.x + csample.DATA[3], s.y + csample.DATA[4], s.z + csample.DATA[5]);
//                s.setTime(samples.size() * Sample.SAMPLE_FREQUENCY);
//				samples.add(s);
//			}
//			else // csample contains a single Sample
//			{
//				s = csample.toSample();
//                s.setTime(samples.size() * Sample.SAMPLE_FREQUENCY);
//				samples.add(s);
//			}
//
//			xp = s.x;
//			yp = s.y;
//			zp = s.z;
//		}
//
//		return data;
//	}
//
//	/*
//	 * Returns the sample at the specified index, returning an empty one if the index
//	 * is out of bounds.
//	 */
//	private CompressedSample safeGet(int i)
//	{
//		if (i >= 0 && i < samples.size())
//			return samples.get(i);
//		else
//			return new CompressedSample();
//	}
	
//	/**
//	 * Reads a CompressedData from a space separated text file, ignoring headers.
//	 * @param src The File from which to read
//	 * @return The read CompressedData.
//	 */
//	public static DataDecompressor fromTextFile(File src)
//	{
//		try
//		{
//			Scanner in = new Scanner(src);
//
//			DataDecompressor cdata = new DataDecompressor();
//
//			String[] line;
//			byte[] data = new byte[20];
//
//			while (in.hasNextLine())
//			{
//				// Read the line
//				line = in.nextLine().split("\\s");
//
//				// Should have 20 values
//				if (line.length != 20)
//				{
//					in.close();
//					throw new IllegalArgumentException("Expects lines with 20 values: Found " + line.length + "!");
//				}
//
//				// Convert the values to bytes
//				for (int i = 0; i < data.length; ++i)
//				{
//					data[i] = Byte.parseByte(line[i]);
//				}
//
//				// Add the line of data
//				addLine(cdata, data);
//			}
//
//			in.close();
//
//			return cdata;
//		}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("Error reading CompressedData from File! (" + e.getMessage() + ")");
//		}
//
//		return null;
//	}
	
//	/**
//	 * Compresses the specified Data.
//	 * @param data The Data to compress.
//	 * @return The compressed data
//	 */
//	public static DataDecompressor compress(ImpactData data)
//	{
//		DataDecompressor cdata = new DataDecompressor();
//
//		short xp, yp, zp = yp = xp = Short.MAX_VALUE; // previous sample
//		CompressedSample sample; // current sample
//		Sample s1, s2; // Samples to compress
//
//		// debug
//		int numCompressed = 0;
//
//		for (int i = 0; i < data.SAMPLES.size(); ++i)
//		{
//			s1 = data.SAMPLES.get(i);
//			sample = cdata.new CompressedSample();
//
//			// Check whether three samples can be compressed
//			if (xp != Short.MAX_VALUE && canBeCompressed(xp, yp, zp,
//					s1, s2 = data.getSafe(i + 1), sample.DATA))
//			{
//				sample.setCompressed(true);
//
//				++i; // Skip s2
//				xp = s2.x;
//				yp = s2.y;
//				zp = s2.z;
//
//				++numCompressed;
//			}
//			else
//			{
//				xp = s1.x;
//				yp = s1.y;
//				zp = s1.z;
//			}
//
//			cdata.samples.add(sample);
//		}
//
//		System.out.printf("Compressed %d of %d samples\n", numCompressed, data.SAMPLES.size());
//
//		return cdata;
//	}
	
//	/*
//	 * Tests whether the specified samples can be compressed. data will be filled with the byte data for the sample(s).
//	 * Returns true if compressed.
//	 */
//	private static boolean canBeCompressed(short x0, short y0, short z0, Sample s1, Sample s2, byte[] data)
//	{
//		if (s2 == null)
//			return false;
//
//		// Basically, the compression works by finding triplets of samples such that
//		// the latter two can be represented as the first offset with an offset that
//		// can be represented as all bytes
//
//		// Find the six offsets as shorts
//		short u1, u2, u3, u4, u5, u6;
//
//		// Find the first three
//		u1 = (short) (s1.x - x0);
//		u2 = (short) (s1.y - y0);
//		u3 = (short) (s1.z - z0);
//
//		// Find the second three
//		u4 = (short) (s2.x - s1.x);
//		u5 = (short) (s2.y - s1.y);
//		u6 = (short) (s2.z - s1.z);
//
//		// If the offsets can all be represented as bytes then we can set this sample's
//		// data to the offsets and mark it as compressed thus squeezing two samples into the space of one
//		boolean canCompress = valuesAreBytes(u1, u2, u3, u4, u5, u6);
//
//		if (canCompress)
//		{
//			data[0] = (byte) u1;
//			data[1] = (byte) u2;
//			data[2] = (byte) u3;
//			data[3] = (byte) u4;
//			data[4] = (byte) u5;
//			data[5] = (byte) u6;
//		}
//		else // Data will just contain the one sample, s1
//		{
//			s1.toBytes(data);
//		}
//
//		return canCompress;
//	}
	

	
//	/*
//	 * Tests whether the set of shorts are all within the allowable range for a byte.
//	 * @param vals The values to test
//	 */
//	private static boolean valuesAreBytes(short... vals)
//	{
//		for (short val: vals)
//		{
//			if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE)
//				return false;
//		}
//
//		return true;
//	}
	
	/*
	 * Wrapper class for a CompressedSample.
	 * This will contain either 1 or 2 samples of data depending on the data.
	 */
	private class CompressedSample
	{
		private boolean isCompressed; // Indicates whether this contains 1 (false) or 2 (true) samples
		public final byte[] DATA; // The sample data, always 6 bytes
		
		/**
		 * Default Constructor.
		 */
		public CompressedSample()
		{
			DATA = new byte[6];
		}
		
		/**
		 * Creates a CompressedSample.
		 * @param b The compressed flag
		 * @param off Offset into data
		 * @param data Array of data from which to read, beginning at offset
		 */
		public CompressedSample(boolean b, int off, byte[] data)
		{
			this ();
			
			this.isCompressed = b;
			
			for (int i = 0; i < DATA.length; ++i)
				DATA[i] = data[i + off];
		}
		
//		/**
//		 * Returns the bit mask value of field b.
//		 * @param shift The amount by which to left shift
//		 * @return The bit mask value of field b
//		 */
//		public byte getFlag(int shift)
//		{
//			return (byte) ((isCompressed ? 1: 0) << shift);
//		}
		
		/**
		 * Tests whether this sample is compressed.
		 * @return Whether this sample is compressed
		 */
		public boolean isCompressed()
		{
			return isCompressed;
		}
		
		/**
		 * Converts this CompressedSample to a Sample. Only applicable for uncompressed Samples.
		 * @return The Sample from this CompressedSample
		 */
		public Sample toSample()
		{
			if (isCompressed)
				throw new IllegalArgumentException("This CompressedSample is compressed!");
			
			Sample s = new Sample();
			s.fromBytes(DATA);
			
			return s;
		}
		
//		/**
//		 * Sets whether this CompressedSample contains 2 (true) or 1 (false) samples
//		 * @param b Whether or not this CompressedSample contains 2 (true) or 1 (false) samples
//		 */
//		public void setCompressed(boolean b)
//		{
//			this.isCompressed = b;
//		}
		
		@Override
		public String toString()
		{
			return DATA[0] + " " + DATA[1] + " " + DATA[2] + " " + DATA[3] + " " + DATA[4] + " " + DATA[5]; 
		}
	}

    /**
     * Interface for listening for decompressed data.
     */
    public interface DecompressedDataCallback
    {
        /**
         * Called when this DecompressedDataCallback has received a new decompressed Sample.
         * @param sample The newly received decompressed Sample
         */
        void onNewSample(Sample sample);
    }
}
