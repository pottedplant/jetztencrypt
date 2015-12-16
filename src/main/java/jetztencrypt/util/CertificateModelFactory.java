package jetztencrypt.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;

import com.google.common.base.Throwables;
import com.google.common.base.Verify;

import jetztencrypt.model.CertificateModel;

public abstract class CertificateModelFactory {

	public static CertificateModel from(X509CertificateHolder h) {
		Extensions extensions = h.getExtensions();

		Instant notBefore = Verify.verifyNotNull(h.getNotBefore().toInstant());
		Instant notAfter = Verify.verifyNotNull(h.getNotAfter().toInstant());

		String cn;

		{
			RDN[] cnRdns = h.getSubject().getRDNs(BCStyle.CN);
			Verify.verify(cnRdns!=null && cnRdns.length==1);
			AttributeTypeAndValue[] tvs = cnRdns[0].getTypesAndValues();
			Verify.verify(tvs!=null && tvs.length==1);
			cn = Verify.verifyNotNull(IETFUtils.valueToString(tvs[0].getValue()));
		}

		Set<String> altNames = new LinkedHashSet<>();

		if( extensions!=null ) {
			Extension eAltNames = extensions.getExtension(Extension.subjectAlternativeName);
			if( eAltNames!=null ) {
				GeneralNames gNames = GeneralNames.getInstance(eAltNames.getParsedValue());
				for(GeneralName gName:gNames.getNames())
					if( gName.getTagNo()==GeneralName.dNSName)
						altNames.add(IETFUtils.valueToString(gName.getName()));
			}
		}

		return new CertificateModel() {

			@Override public Instant notBefore() { return notBefore; }
			@Override public Instant notAfter() { return notAfter; }
			@Override public Set<String> altNames() { return altNames; }
			@Override public String cn() { return cn; }

			@Override
			public java.security.cert.Certificate jceCert() {
				try {
					return CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(h.getEncoded()));
				} catch(CertificateException|IOException e) {
					throw Throwables.propagate(e);
				}
			}

			@Override
			public PublicKey publicKey() {
				try {
					return new KeyFactorySpi().generatePublic(h.getSubjectPublicKeyInfo());
				} catch(IOException e) {
					throw Throwables.propagate(e);
				}
			}

			@Override
			public void writeTo(OutputStream stream) throws IOException {
				PEMUtils.write(Arrays.asList(h),stream);
			}

		};
	}

}
