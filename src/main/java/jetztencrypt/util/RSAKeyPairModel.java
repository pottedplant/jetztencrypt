package jetztencrypt.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.util.io.pem.PemWriter;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedMap;

import jetztencrypt.model.KeyPairModel;

public class RSAKeyPairModel implements KeyPairModel {

	private final RSAKeyParameters privateKey;
	private final RSAKeyParameters publicKey;

	public RSAKeyPairModel(RSAKeyParameters privateKey,RSAKeyParameters publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	@Override
	public void writePrivateKeyTo(Path privateKeyPath) throws IOException {
		try(BufferedWriter out = Files.newBufferedWriter(privateKeyPath)) {
			try(PemWriter pemWriter = new PemWriter(out)) {
				pemWriter.writeObject(new PKCS8Generator(privateKeyInfo(),null));
			}
		}
	}

	@Override
	public ImmutableSortedMap<String,String> webKey() {
		return RSAWebKeyUtils.webKey(publicKey);
	}

	@Override
	public String webKeyThumbprint() {
		return RSAWebKeyUtils.webKeyThumbprint(publicKey);
	}

	@Override
	public PrivateKey privateKey() {
		try {
			return new KeyFactorySpi().generatePrivate(privateKeyInfo());
		} catch(IOException e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public PublicKey publicKey() {
		try {
			return new KeyFactorySpi().generatePublic(publicKeyInfo());
		} catch(IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private SubjectPublicKeyInfo publicKeyInfo() throws IOException {
		return SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey);
	}

	private PrivateKeyInfo privateKeyInfo() throws IOException {
		return PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey);
	}

}
