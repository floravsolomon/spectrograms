package SpectroClasses;

public class Fourier {
	public static Complex[] fft(Complex[] input) {

		int n = input.length;

		if (n == 1) {
			return new Complex[] { input[0] };
		}

		Complex[] even = new Complex[n / 2];

		for (int k = 0; k < n / 2; k++) {
			even[k] = input[2 * k];
		}

		Complex[] evenFFT = fft(even);

		Complex[] odd = new Complex[n / 2];

		for (int k = 0; k < n / 2; k++) {
			odd[k] = input[2 * k + 1];
		}

		Complex[] oddFFT = fft(odd);

		Complex[] result = new Complex[n];

		for (int k = 0; k < n / 2; k++) {
			double kth = -2 * Math.PI * k / n;

			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));

			result[k] = evenFFT[k].add(wk.mult(oddFFT[k]));
			result[k + n / 2] = evenFFT[k].sub(wk.mult(oddFFT[k]));
		}

		return result;
	}

	public static Complex[] ifft(Complex[] x) {
		int n = x.length;
		Complex[] y = new Complex[n];

		// take conjugate
		for (int i = 0; i < n; i++) {
			y[i] = x[i].conjugate();
		}

		// compute forward FFT
		y = fft(y);

		// take conjugate again
		for (int i = 0; i < n; i++) {
			y[i] = y[i].conjugate();
		}

		// divide by n
		for (int i = 0; i < n; i++) {
			y[i] = y[i].scale(1.0 / n);
		}

		return y;
	}

	public static int bitReverse(int n, int bits) {
		int reversedN = n;
		int count = bits - 1;

		n >>= 1;
		while (n > 0) {
			reversedN = (reversedN << 1) | (n & 1);
			count--;
			n >>= 1;
		}

		return ((reversedN << count) & ((1 << bits) - 1));
	}
}
