package arena.arenasmartball.correlation;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Wrapper class for a DFT.
 * @author Theodore Stone
 */
public class DFT
{
	/** The number of frequency samples */
	public final int length;
	
	/** The real component array of this DFT */
	public double[] reals;
	
	/** The imaginary component array of this DFT */
	public double[] imags;
	
	/**
	 * Constructs a new DFT from the given time series.
		 * @param timeSeries The time series
	 */
	public DFT(double[] timeSeries)
	{
		length = timeSeries.length;
		reals = new double[length];
		imags = new double[length];
		
		// Calculate
		double[] fft = new double[length * 2];

		if (length <= 0)
		{
			System.err.println("DFT: Error creating DFT because the time series is empty");
			return;
		}
		
		DoubleFFT_1D fftDo = new DoubleFFT_1D(timeSeries.length);
		System.arraycopy(timeSeries, 0, fft, 0, timeSeries.length);
		fftDo.realForwardFull(fft);
		
		// Copy to arrays
		for (int i = 0; i < length; ++i)
		{
			reals[i] = Math.abs(fft[i * 2]);
			imags[i] = Math.abs(fft[i * 2 + 1]);
		}
	}
}