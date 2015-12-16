package jetztencrypt.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import jetztencrypt.model.CertificateModel;
import jetztencrypt.model.KeyPairModel;
import jetztencrypt.util.CertificateHelper;

public abstract class Actions {

	// defs

	private static final Log log = LogFactory.getLog(Actions.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	// impl

	public static Directory directory(CloseableHttpClient httpClient,String url) throws IOException {
		try(Response response = execute(httpClient,RequestBuilder.get(String.format("%sdirectory",url)).build())) {
			Verify.verify(response.status()==200,"unexepcted response code %s",response.status());

			return new Directory(response);
		}
	}

	public static NewRegistration newReg(CloseableHttpClient httpClient,String resourceUrl,String nonce,KeyPairModel accountKey,RegistrationOptions options) throws IOException {
		String data = Jwts.builder()
			.setHeaderParam("nonce",nonce)
			.setHeaderParam(JwsHeader.JSON_WEB_KEY,accountKey.webKey())
			.setClaims(ImmutableMap.<String,Object>builder()
				.put("resource","new-reg")
				.put("contact",options.contact)
				.build()
			)
			.signWith(SignatureAlgorithm.RS256,accountKey.privateKey())
			.compact()
		;

		try(Response response = execute(httpClient,RequestBuilder.post(resourceUrl).setEntity(new StringEntity(data,StandardCharsets.UTF_8)).build())) {
			Verify.verify(response.status()==201 || response.status()==409,"unexepcted response code %s",response.status());

			return new NewRegistration(response);
		}
	}

	public static Registration reg(CloseableHttpClient httpClient,String resourceUrl,String nonce,KeyPairModel accountKey,RegistrationOptions registrationOptions) throws IOException {
		ImmutableMap.Builder<String,Object> claimsBuilder = ImmutableMap.<String,Object>builder()
			.put("resource","reg")
		;

		if( registrationOptions!=null ) {
			if( registrationOptions.contact!=null )
				claimsBuilder.put("contact",registrationOptions.contact);
			if( registrationOptions.agreement!=null )
				claimsBuilder.put("agreement",registrationOptions.agreement);
		}

		String data = Jwts.builder()
			.setHeaderParam("nonce",nonce)
			.setHeaderParam(JwsHeader.JSON_WEB_KEY,accountKey.webKey())
			.setClaims(claimsBuilder.build())
			.signWith(SignatureAlgorithm.RS256,accountKey.privateKey())
			.compact()
		;

		try(Response response = execute(httpClient,RequestBuilder.post(resourceUrl).setEntity(new StringEntity(data,StandardCharsets.UTF_8)).build())) {
			Verify.verify(response.status()==202,"unexepcted response code %s",response.status());

			return new Registration(response);
		}
	}

	public static NewAuthz newAuthz(CloseableHttpClient httpClient,String resourceUrl,String nonce,KeyPairModel accountKey,String domain) throws IOException {
		String data = Jwts.builder()
			.setHeaderParam("nonce",nonce)
			.setHeaderParam(JwsHeader.JSON_WEB_KEY,accountKey.webKey())
			.setClaims(ImmutableMap.<String,Object>builder()
				.put("resource","new-authz")
				.put("identifier",ImmutableMap.<String,Object>builder()
					.put("type","dns")
					.put("value",domain)
					.build()
				)
				.build()
			)
			.signWith(SignatureAlgorithm.RS256,accountKey.privateKey())
			.compact()
		;

		try(Response response = execute(httpClient,RequestBuilder.post(resourceUrl).setEntity(new StringEntity(data,StandardCharsets.UTF_8)).build())) {
			Verify.verify(response.status()==201,"unexepcted response code %s",response.status());

			return new NewAuthz(response);
		}
	}

	public static Challenge http01(CloseableHttpClient httpClient,String resourceUrl,String nonce,KeyPairModel accountKey,String keyAuthorization) throws IOException {
		String data = Jwts.builder()
			.setHeaderParam("nonce",nonce)
			.setHeaderParam(JwsHeader.JSON_WEB_KEY,accountKey.webKey())
			.setClaims(ImmutableMap.<String,Object>builder()
				.put("resource","challenge")
				.put("keyAuthorization",keyAuthorization)
				.build()
			)
			.signWith(SignatureAlgorithm.RS256,accountKey.privateKey())
			.compact()
		;

		try(Response response = execute(httpClient,RequestBuilder.post(resourceUrl).setEntity(new StringEntity(data,StandardCharsets.UTF_8)).build())) {
			Verify.verify(response.status()==202,"unexepcted response code %s",response.status());

			return new Challenge(response);
		}
	}

	public static Challenge challenge(CloseableHttpClient httpClient,String resourceUrl) throws IOException {
		try(Response response = execute(httpClient,RequestBuilder.get(resourceUrl).build())) {
			Verify.verify(response.status()==202,"unexepcted response code %s",response.status());

			return new Challenge(response);
		}
	}

	public static NewCert newCert(CloseableHttpClient httpClient,String resourceUrl,String nonce,KeyPairModel accountKey,KeyPairModel certificateKey,String domain,String...altNames) throws IOException, OperatorCreationException {
		JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Name(String.format("CN=%s",domain)),certificateKey.publicKey());

		if( altNames!=null && altNames.length>0 ) {
			GeneralName[] subjectAltNames = new GeneralName[altNames.length];

			for(int i=0;i<altNames.length;++i)
				subjectAltNames[i] = new GeneralName(GeneralName.dNSName,altNames[i]);

			ExtensionsGenerator extGen = new ExtensionsGenerator();
			extGen.addExtension(Extension.subjectAlternativeName,false,new GeneralNames(subjectAltNames));

			csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,extGen.generate());
		}

		String csr = TextCodec.BASE64URL.encode(csrBuilder
			.build(new JcaContentSignerBuilder("SHA256withRSA").build(certificateKey.privateKey()))
			.getEncoded()
		);

		String data = Jwts.builder()
			.setHeaderParam("nonce",nonce)
			.setHeaderParam(JwsHeader.JSON_WEB_KEY,accountKey.webKey())
			.setClaims(ImmutableMap.<String,Object>builder()
				.put("resource","new-cert")
				.put("csr",csr)
				.build()
			)
			.signWith(SignatureAlgorithm.RS256,accountKey.privateKey())
			.compact()
		;

		try(Response response = execute(httpClient,RequestBuilder.post(resourceUrl).setEntity(new StringEntity(data,StandardCharsets.UTF_8)).build())) {
			Verify.verify(response.status()==201,"unexepcted response code %s",response.status());

			return new NewCert(response);
		}
	}

	public static CertificateResponse fetchCertifictate(CloseableHttpClient httpClient,String location) throws IOException {
		HttpGet request = new HttpGet(location);

		if( log.isTraceEnabled() )
			log.trace(String.format("[%s] <<",request));

		try(CloseableHttpResponse response = httpClient.execute(request)) {
			if( log.isTraceEnabled() )
				log.trace(String.format("[%s] >> %s",request,response));

			Verify.verify(response.getStatusLine().getStatusCode()/100==2);

			CertificateModel certificate = CertificateHelper.fromASN1(response.getEntity().getContent());

			String up = null;

			for(Header header:response.getHeaders("Link")) {
				String value = header.getValue();
				if( value.startsWith("<") && value.endsWith(">;rel=\"up\"") ) {
					up = value.substring(1,value.length()-10);;
					break;
				}
			}

			return new CertificateResponse(certificate,up);
		}
	}

	public static Response execute(CloseableHttpClient httpClient,HttpUriRequest request) throws IOException {
		if( log.isTraceEnabled() )
			log.trace(String.format("[%s] <<",request));

		CloseableHttpResponse response = httpClient.execute(request);
		try {
			if( log.isTraceEnabled() )
				log.trace(String.format("[%s] >> %s",request,response));

			ObjectNode content = null;

			Header contentTypeHeader = response.getFirstHeader("Content-Type");
			if( contentTypeHeader!=null && Objects.equals(contentTypeHeader.getValue(),"application/json") )
				content = mapper.readValue(response.getEntity().getContent(),ObjectNode.class);

			if( log.isTraceEnabled() )
				log.trace(String.format("[%s] >> JSON: %s",request,content));

			return new Response(response,content);
		} catch(Throwable t) {
			if( log.isErrorEnabled() )
				log.error(String.format("[%s] failed",request),t);

			EntityUtils.consumeQuietly(response.getEntity());
			throw t;
		}
	}

}
