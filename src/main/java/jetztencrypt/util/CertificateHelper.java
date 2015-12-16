package jetztencrypt.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;

import com.google.common.base.Verify;

import jetztencrypt.model.CertificateModel;

public abstract class CertificateHelper {

	public static CertificateModel firstFrom(Source source) throws IOException {
		List<X509CertificateHolder> certificates = PEMUtils.certificates(source);
		Verify.verify(!certificates.isEmpty(),"no certificates found");

		return CertificateModelFactory.from(certificates.iterator().next());
	}

	public static CertificateModel fromASN1(InputStream stream) throws IOException {
		try(ASN1InputStream asn1Stream = new ASN1InputStream(stream)) {
			return fromASN1(asn1Stream);
		}
	}

	public static CertificateModel fromASN1(ASN1InputStream stream) throws IOException {
		return CertificateModelFactory.from(new X509CertificateHolder(Certificate.getInstance(stream.readObject())));
	}

}
