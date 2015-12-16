package jetztencrypt.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import jetztencrypt.model.KeyPairModel;

public abstract class PEMUtils {

	public static List<X509CertificateHolder> certificates(Source source) throws IOException {
		List<X509CertificateHolder> certificates = new ArrayList<>();

		try(PEMParser parser = new PEMParser(source.reader())) {
			Object obj;

			while( (obj=parser.readObject())!=null )
				certificates.add(X509CertificateHolder.class.cast(obj));
		}

		return certificates;
	}

	public static KeyPairModel readPrivateKey(Source source) throws IOException {
		return RSAKeyPairHelper.readPrivateKey(source);
	}

	public static void write(List<X509CertificateHolder> certificates,OutputStream stream) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(stream,StandardCharsets.UTF_8);
		write(certificates,writer);
		writer.flush();
	}

	public static void write(List<X509CertificateHolder> certificates,Writer writer) throws IOException {
		@SuppressWarnings("resource")
		PemWriter pemWriter = new PemWriter(writer);
		for(X509CertificateHolder certificate:certificates)
			pemWriter.writeObject(new JcaMiscPEMGenerator(certificate));
		pemWriter.flush();
	}

}
