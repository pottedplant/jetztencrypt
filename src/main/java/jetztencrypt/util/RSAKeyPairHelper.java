package jetztencrypt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.PEMParser;

import jetztencrypt.model.KeyPairModel;

public abstract class RSAKeyPairHelper {

	// defs

	private final static BigInteger defaultPublicExponent = BigInteger.valueOf(0x10001);
	private final static int defaultTests = 112;

	// impl

	public static KeyPairModel generateRSA(int strength) {
		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		kpg.init(new RSAKeyGenerationParameters(defaultPublicExponent,new SecureRandom(),strength,defaultTests));

		AsymmetricCipherKeyPair keyPair = kpg.generateKeyPair();

		return new RSAKeyPairModel(
			RSAKeyParameters.class.cast(keyPair.getPrivate()),
			RSAKeyParameters.class.cast(keyPair.getPublic())
		);
	}

	public static KeyPairModel readPrivateKey(Source source) throws IOException {
		// we only support rsa at the moment

		try(BufferedReader reader = new BufferedReader(source.reader())) {
			try(PEMParser pemParser = new PEMParser(reader)) {
				PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.class.cast(pemParser.readObject());
				RSAPrivateCrtKeyParameters privateKey = RSAPrivateCrtKeyParameters.class.cast(PrivateKeyFactory.createKey(privateKeyInfo));
				RSAKeyParameters publicKey = new RSAKeyParameters(false,privateKey.getModulus(),privateKey.getPublicExponent());
				return new RSAKeyPairModel(privateKey,publicKey);
			}
		}
	}

}
