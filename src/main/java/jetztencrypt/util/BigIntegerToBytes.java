package jetztencrypt.util;

import java.math.BigInteger;

public abstract class BigIntegerToBytes {

	// from commons codec
	public static byte[] convert(BigInteger value) {
		int bitlen = value.bitLength();
		// round bitlen
		bitlen = ((bitlen + 7) >> 3) << 3;
		final byte[] bigBytes = value.toByteArray();

		if (((value.bitLength() % 8) != 0) && (((value.bitLength() / 8) + 1) == (bitlen / 8))) {
			return bigBytes;
		}
		// set up params for copying everything but sign bit
		int startSrc = 0;
		int len = bigBytes.length;

		// if bigInt is exactly byte-aligned, just skip signbit in copy
		if ((value.bitLength() % 8) == 0) {
			startSrc = 1;
			len--;
		}
		final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
		final byte[] resizedBytes = new byte[bitlen / 8];
		System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
		return resizedBytes;
	}

}
