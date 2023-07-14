package SpectroClasses;

public class Complex {
	public double re;
	public double im;

	public Complex() {
		this(0, 0);
	}

	public Complex(double r, double i) {
		re = r;
		im = i;
	}

	public Complex(Complex b) {
		re = b.real();
		im = b.imag();
	}

	public double real() {
		return this.re;
	}

	public double imag() {
		return this.im;
	}

	public Complex add(Complex b) {
		return new Complex(this.re + b.re, this.im + b.im);
	}

	public Complex sub(Complex b) {
		return new Complex(this.re - b.re, this.im - b.im);
	}

	public Complex mult(Complex b) {
		return new Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re);
	}

	public Complex div(Complex b) {
		Complex r = new Complex();
		double n = b.mag();

		r.re = ((this.real() * b.real()) + (this.imag() * b.imag())) / n;
		r.im = ((b.real() * this.imag()) - (this.real() * b.imag())) / n;

		return r;
	}

	public Complex exp() {
		Complex x, y;

		x = new Complex(Math.exp(this.real()), 0);
		y = new Complex(Math.cos(this.imag()), Math.sin(this.imag()));

		return x.mult(y);
	}

	public double mag() {
		return Math.sqrt(re * re + im * im);
	}

	public Complex conjugate() {
		return new Complex(re, -im);
	}

	public Complex scale(double alpha) {
		return new Complex(alpha * re, alpha * im);
	}

	@Override
	public String toString() {
		return String.format("(%f,%f)", re, im);
	}
}
