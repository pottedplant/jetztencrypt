package jetztencrypt.model;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Set;

public interface CertificateModel {

	Instant notBefore();
	Instant notAfter();

	String cn();
	Set<String> altNames();

	Certificate jceCert();
	PublicKey publicKey();

	void writeTo(OutputStream stream) throws IOException;

}
