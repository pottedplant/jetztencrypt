package jetztencrypt.util;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.jsonwebtoken.impl.TextCodec;

public abstract class RSAWebKeyUtils {

	// defs

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final HashFunction sha256 = Hashing.sha256();

	// impl

	public static ImmutableSortedMap<String,String> webKey(RSAKeyParameters key) {
		Verify.verify(!key.isPrivate());

		return ImmutableSortedMap.<String,String>naturalOrder()
			.put("e",TextCodec.BASE64URL.encode(BigIntegerToBytes.convert(key.getExponent())))
			.put("kty","RSA")
			.put("n",TextCodec.BASE64URL.encode(BigIntegerToBytes.convert(key.getModulus())))
			.build()
		;
	}

	public static String webKeyThumbprint(RSAKeyParameters key) {
		try {
			return TextCodec.BASE64URL.encode(sha256.hashBytes(mapper.writeValueAsBytes(webKey(key))).asBytes());
		} catch(JsonProcessingException e) {
			throw Throwables.propagate(e);
		}
	}

}
