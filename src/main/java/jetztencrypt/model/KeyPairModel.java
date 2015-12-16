package jetztencrypt.model;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.common.collect.ImmutableSortedMap;

public interface KeyPairModel {

	PrivateKey privateKey();
	PublicKey publicKey();

	ImmutableSortedMap<String,String> webKey();
	String webKeyThumbprint();

	void writePrivateKeyTo(Path privateKeyPath) throws IOException;

}
