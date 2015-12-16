package jetztencrypt.client;

import jetztencrypt.model.CertificateModel;

public class CertificateResponse {

	public final CertificateModel certificate;
	public final String up;

	public CertificateResponse(CertificateModel certificate,String up) {
		this.certificate = certificate;
		this.up = up;
	}

}
