package jetztencrypt.app;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import com.sun.net.httpserver.HttpServer;

import jetztencrypt.client.Actions;
import jetztencrypt.client.AuthzChallenge;
import jetztencrypt.client.CertificateResponse;
import jetztencrypt.client.Challenge;
import jetztencrypt.client.Directory;
import jetztencrypt.client.NewAuthz;
import jetztencrypt.client.NewCert;
import jetztencrypt.client.NewRegistration;
import jetztencrypt.client.Registration;
import jetztencrypt.client.RegistrationOptions;
import jetztencrypt.model.CertificateModel;
import jetztencrypt.model.KeyPairModel;
import jetztencrypt.util.CertificateHelper;
import jetztencrypt.util.JDKToCommonsHandler;
import jetztencrypt.util.PEMUtils;
import jetztencrypt.util.RSAKeyPairHelper;
import jetztencrypt.util.Source;

public class Application {

	// defs

	private static final String CMD_LINE_SYNTAX = "<java -jar jetztencrypt.jar> <options>";
	private static final long STANDARD_DAY_MILLIS = 24l*60l*60l*1000l;

	private static final String DEFAULT_ACME_SERVER = "https://acme-v01.api.letsencrypt.org/";
//	private static final String DEFAULT_ACME_SERVER = "https://acme-staging.api.letsencrypt.org/";
	private static final int DEFAULT_ACCOUNT_KEY_STRENGTH = 4096;
	private static final int DEFAULT_CERTIFICATE_KEY_STRENGTH = 2048;
	private static final String DEFAULT_BIND_HOST = "127.0.0.1";
	private static final int DEFAULT_BIND_PORT = 8080;
	private static final long DEFAULT_CERTIFICATE_MIN_VALID_DAYS = 30;
	private static final String DEFAULT_LOG_LEVEL = "info";

	// impl

	public static void main(String[] args) throws Throwable {
		try {
//			System.exit(run(new String[]{
//				"--account-key","tmp/account.key",
//				"--certificate-key","tmp/certificate.key",
//				"--certificate","tmp/certificate.crt",
//				"--hostname","std.cx",
//				"--alt-name","std.cx",
//				"--alt-name","test.std.cx",
//				"--mode","server",
//				"--server-bind-port","8090",
//				"--embedded-identrust-root",
////				"--acme-directory","tmp/acme",
//				"--certificate-min-valid-days","90",
//				"--log-level","info",
////					"--help",
//			}));
			run(args);
		} catch(Throwable t) {
			t.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	public static int run(String[] args) throws Throwable {
		Options options = options();

		// test for --help
		if( Arrays.asList(args).contains("--help") ) {
			showHelp(options);
			return 0;
		}

		Path accountKeyPath;
		int accountKeyStrength;
		Path certificateKeyPath;
		int certificateKeyStrength;
		long certificateMinValidDays;
		Path certificatePath;
		String hostname;
		String[] altNames;
		String mode;
		String acmeServerUrl;
		String bindHost = null;
		int bindPort = -1;
		Path acmeDirectory = null;

		String logLevel;
		boolean embeddedIdenTrustRoot;

		try {
			CommandLine cmdline = new DefaultParser().parse(options,args);
			if( !cmdline.getArgList().isEmpty() )
				throw new ParseException(String.format("unrecognized arguments: %s",cmdline.getArgList()));

			accountKeyPath = Paths.get(singleton(cmdline,"account-key"));
			accountKeyStrength = Integer.parseInt(singleton(cmdline,"account-key-strength",Integer.toString(DEFAULT_ACCOUNT_KEY_STRENGTH)));
			certificateKeyPath = Paths.get(singleton(cmdline,"certificate-key"));
			certificateKeyStrength = Integer.parseInt(singleton(cmdline,"certificate-key-strength",Integer.toString(DEFAULT_CERTIFICATE_KEY_STRENGTH)));
			certificatePath = Paths.get(singleton(cmdline,"certificate"));
			certificateMinValidDays = Long.parseLong(singleton(cmdline,"certificate-min-valid-days",Long.toString(DEFAULT_CERTIFICATE_MIN_VALID_DAYS)));
			hostname = singleton(cmdline,"hostname");
			altNames = cmdline.getOptionValues("alt-name");
			mode = singleton(cmdline,"mode");
			acmeServerUrl = singleton(cmdline,"acme-server",DEFAULT_ACME_SERVER);
			embeddedIdenTrustRoot = cmdline.hasOption("embedded-identrust-root");
			logLevel = singleton(cmdline,"log-level",DEFAULT_LOG_LEVEL);

			switch(mode) {

			case "server": {
				bindHost = singleton(cmdline,"server-bind-host",DEFAULT_BIND_HOST);
				bindPort = Integer.parseInt(singleton(cmdline,"server-bind-port",Integer.toString(DEFAULT_BIND_PORT)));
			} break;

			case "directory": {
				acmeDirectory = Paths.get(singleton(cmdline,"acme-directory"));
				Verify.verify(Files.isDirectory(acmeDirectory),"not a directory: '%s'",acmeDirectory);
			} break;

			default: throw new ParseException(String.format("invalid mode '%s'",mode));
			}

		} catch(ParseException e) {
			System.err.println(e.getMessage());
			return -1;
		}

		// init logging

		{
			ConsoleAppender console = new ConsoleAppender(new PatternLayout("%d %5p [%c] %m%n"));
			console.setThreshold(org.apache.log4j.Level.toLevel(logLevel));
			Logger.getRootLogger().addAppender(console);
		}

		Log log = LogFactory.getLog(Application.class);
		JDKToCommonsHandler.rerouteJDKToCommons(Level.FINEST);

		// test if certificate is valid

		if( log.isDebugEnabled() )
			log.debug(String.format("testing certificate (%s,%s)",certificatePath,certificateKeyPath));

		if( certificateValid(log,certificatePath,certificateKeyPath,certificateMinValidDays,hostname,Sets.newHashSet(altNames)) ) {

			if( log.isInfoEnabled() )
				log.info("valid certificate found.");

			return 0;
		}

		// get or create account key

		if( log.isDebugEnabled() )
			log.debug(String.format("testing account key (%s)",accountKeyPath));

		KeyPairModel accountKey = privateKey(log,accountKeyPath,accountKeyStrength);

		// with client
		try(CloseableHttpClient httpClient = HttpClientBuilder.create()
				.disableAutomaticRetries()
				.setSSLContext(customSSLContext(embeddedIdenTrustRoot))
				.setConnectionManagerShared(false)
				.build()
			) {

			// retrieve service urls
			if( log.isDebugEnabled() )
				log.debug("retrieving acme service directory");

			Directory directory = Actions.directory(httpClient,acmeServerUrl);
			String nonce = directory.nonce();

			// new registration (or 409 conflict)
			if( log.isDebugEnabled() )
				log.debug("(re-)registering account");

			RegistrationOptions registrationOptions = RegistrationOptions.forContact();
			NewRegistration newRegistration = Actions.newReg(httpClient,directory.resourceUrl("new-reg"),nonce,accountKey,registrationOptions);
			nonce = newRegistration.nonce();

			String registrationUri = newRegistration.registrationUri();

			// fetch registration
			Registration registration = Actions.reg(httpClient,registrationUri,nonce,accountKey,null);
			nonce = registration.nonce();

			// do we need to update the agreement?
			String termsOfService = registration.termsOfService();
			if( termsOfService!=null && !Objects.equals(termsOfService,registration.agreement()) ) {
				registrationOptions = registrationOptions.withAgreement(termsOfService);

				// update agreement
				registration = Actions.reg(httpClient,registrationUri,nonce,accountKey,registrationOptions);
				nonce = registration.nonce();
			}

			// verify ownership

			Set<String> domains = new HashSet<>();
			domains.add(hostname);
			domains.addAll(Arrays.asList(altNames));

			BiFunction<String,byte[],VerificationHandle> verificationMethod;

			switch(mode) {
			case "server": verificationMethod = serverVerification(bindHost,bindPort); break;
			case "directory": verificationMethod = directoryVerification(acmeDirectory); break;
			default: throw new UnsupportedOperationException();
			}

			for(String domain:domains) {
				if( log.isInfoEnabled() )
					log.info(String.format("verifying ownership of '%s'",domain));

				// new authorization
				NewAuthz newAuthz = Actions.newAuthz(httpClient,directory.resourceUrl("new-authz"),nonce,accountKey,domain);
				nonce = newAuthz.nonce();

				AuthzChallenge authzChallenge = newAuthz.firstOfType("http-01");
				if( authzChallenge==null ) throw new UnsupportedOperationException("no http-01 challenge");

				// key auth
				String keyAuthz = String.format("%s.%s",authzChallenge.token,accountKey.webKeyThumbprint());
				byte[] keyAuthzData = keyAuthz.getBytes(StandardCharsets.UTF_8);

				try {
					try(VerificationHandle handle = verificationMethod.apply(authzChallenge.token,keyAuthzData)) {
						// trigger challenge
						if( log.isDebugEnabled() )
							log.debug("triggering challenge");

						Challenge challenge = Actions.http01(httpClient,authzChallenge.uri,nonce,accountKey,keyAuthz);
						nonce = challenge.nonce();

						Challenge challengeStatus = null;

						retry: for(int i=0;i<3;++i) {
							handle.sleep();

							// verify challenge
							challengeStatus = Actions.challenge(httpClient,authzChallenge.uri);
							nonce = challengeStatus.nonce();

							switch(challengeStatus.status()) {
							case "valid": break retry;
							case "pending": continue;
							default: throw new RuntimeException(String.format("unexpected challenge status '%s'",challengeStatus.status()));
							}
						}

						Verify.verifyNotNull(challengeStatus,"verification timed out");
					}
				} catch(Throwable t) {
					throw new RuntimeException(String.format("while verifying '%s'",domain),t);
				}
			}

			// generate certificate key pair
			if( log.isInfoEnabled() )
				log.info(String.format("generating certificate key (strength=%s)",certificateKeyStrength));

			KeyPairModel certificateKey = RSAKeyPairHelper.generateRSA(certificateKeyStrength);

			// request certificate generation
			NewCert newCert = Actions.newCert(httpClient,directory.resourceUrl("new-cert"),nonce,accountKey,certificateKey,hostname,altNames);
			nonce = newCert.nonce();

			List<CertificateModel> certChain = new ArrayList<>();

			String certificateLocation = newCert.location();
			URI location = URI.create(certificateLocation);

			for(int i=0;i<16;++i) {
				CertificateResponse response = Actions.fetchCertifictate(httpClient,location.toString());

				certChain.add(response.certificate);

				if( response.up==null )
					break;

				location = location.resolve(response.up);
			}

			if( log.isInfoEnabled() )
				log.info(String.format("writing new certificate key to '%s'",certificateKeyPath));

			certificateKey.writePrivateKeyTo(certificateKeyPath);

			if( log.isInfoEnabled() )
				log.info(String.format("writing new certificate chain to '%s'",certificatePath));

			try(OutputStream stream = Files.newOutputStream(certificatePath)) {
				for(CertificateModel certificate:certChain)
					certificate.writeTo(stream);
			}
		}

		return 0;
	}

	private static BiFunction<String,byte[],VerificationHandle> serverVerification(String bindHost,int bindPort) {
		return (token,data)->{
			try {
				HttpServer httpServer = HttpServer.create(new InetSocketAddress(bindHost,bindPort),1);
				try {
					httpServer.start();

					CompletableFuture<Void> future = new CompletableFuture<Void>();

					httpServer.createContext(String.format("/.well-known/acme-challenge/%s",token),e->{
						e.sendResponseHeaders(200,data.length);
						OutputStream out = e.getResponseBody();
						out.write(data);
						out.close();
						future.complete(null);
					});

					return new VerificationHandle() {

						@Override
						public void sleep() {
							try {
								future.get(10,TimeUnit.SECONDS);
								Thread.sleep(1000);
							} catch(TimeoutException e) {
								return;
							} catch(InterruptedException|ExecutionException e) {
								throw Throwables.propagate(e);
							}
						}

						@Override
						public void close() throws Exception {
							httpServer.stop(0);
						}

					};
				} catch(Throwable t) {
					httpServer.stop(0);
					throw t;
				}
			} catch(IOException e) {
				throw Throwables.propagate(e);
			}
		};
	}

	private static BiFunction<String,byte[],VerificationHandle> directoryVerification(Path directory) {
		return (token,data)->{
			Path file = directory.resolve(token);

			try(OutputStream stream = Files.newOutputStream(file)) {
				stream.write(data);
			} catch(IOException e) {
				throw Throwables.propagate(e);
			}

			return new VerificationHandle() {

				@Override
				public void sleep() {
					try {
						Thread.sleep(3000);
					} catch(InterruptedException e) {
						throw Throwables.propagate(e);
					}
				}

				@Override
				public void close() throws Exception {
					Files.deleteIfExists(file);
				}

			};
		};
	}

	private static KeyPairModel privateKey(Log log,Path privateKeyPath,int keyStrength) throws IOException {
		if( !Files.exists(privateKeyPath) ) {
			if( log.isDebugEnabled() )
				log.debug(String.format("private key file (%s) missing",privateKeyPath));

			if( log.isInfoEnabled() )
				log.info(String.format("generating new key (strength=%s) for path '%s'",keyStrength,privateKeyPath));

			RSAKeyPairHelper.generateRSA(keyStrength).writePrivateKeyTo(privateKeyPath);
		}

		if( log.isDebugEnabled() )
			log.debug(String.format("reading private key from '%s'",privateKeyPath));

		return Verify.verifyNotNull(PEMUtils.readPrivateKey(Source.of(privateKeyPath)));
	}

	private static boolean certificateValid(Log log,Path certificatePath,Path certificateKeyPath,long minValidDays,String hostname,Set<String> altNames) throws IOException {
		altNames = new HashSet<>(altNames);
		altNames.add(hostname);

		if( !Files.exists(certificatePath) || !Files.exists(certificateKeyPath) ) {
			if( log.isInfoEnabled() )
				log.info("some or all certificate files are missing");

			return false;
		}

		CertificateModel certificate = CertificateHelper.firstFrom(Source.of(certificatePath));

		Instant now = Instant.now();
		Verify.verify( now.compareTo(certificate.notBefore())>=0, "time is messy" );

		if( now.compareTo(certificate.notAfter())>0 ) {
			if( log.isInfoEnabled() )
				log.info("certificate expired");

			return false;
		}

		Duration duration = Duration.between(now,certificate.notAfter());
		long validDays = duration.toMillis()/STANDARD_DAY_MILLIS;

		if( log.isDebugEnabled() )
			log.debug(String.format("certificate valid days: %s",validDays));

		if( validDays<minValidDays ) {

			if( log.isInfoEnabled() )
				log.info(String.format("certificate valid days: %s (<%s)",validDays,minValidDays));

			return false;
		}

		if(
			!Objects.equals(hostname,certificate.cn()) ||
			!Objects.equals(altNames,certificate.altNames())
		) {
			if( log.isInfoEnabled() )
				log.info(String.format("name mismatch: hostname: required='%s' existing='%s' altNames: required='%s' existing='%s'",hostname,certificate.cn(),altNames,certificate.altNames()));

			return false;
		}

		KeyPairModel certificateKeyPair = PEMUtils.readPrivateKey(Source.of(certificateKeyPath));

		if( !Objects.equals(certificate.publicKey(),certificateKeyPair.publicKey()) ) {
			if( log.isInfoEnabled() )
				log.info("certificate and key file pairs do not match");

			return false;
		}

		return true;
	}

	private static String singleton(CommandLine cmdline,String key) throws ParseException {
		String[] values = cmdline.getOptionValues(key);

		if( values==null || values.length<1 )
			throw new ParseException(String.format("missing option '%s'",key));

		if( values.length!=1 )
			throw new ParseException(String.format("invalid option count for '%s'",key));

		return values[0];
	}

	private static String singleton(CommandLine cmdline,String key,String defaultValue) throws ParseException {
		String[] values = cmdline.getOptionValues(key);
		if( values==null || values.length==0 )
			return defaultValue;

		if( values.length!=1 )
			throw new ParseException(String.format("invalid option count for '%s'",key));

		return values[0];
	}

	private static SSLContext customSSLContext(boolean addIdenTrustRoot) throws GeneralSecurityException,IOException {
		if( !addIdenTrustRoot )
			return SSLContext.getDefault();

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null,null);

		if(true) {
			int certCount = 0;

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			for(TrustManager tm:tmf.getTrustManagers())
				if( X509TrustManager.class.isInstance(tm) )
					for(X509Certificate x509Certificate:X509TrustManager.class.cast(tm).getAcceptedIssuers())
						keyStore.setEntry(String.format("certificate.%s",++certCount),new KeyStore.TrustedCertificateEntry(x509Certificate),null);

			CertificateModel certificate = CertificateHelper.firstFrom(Source.of(Application.class.getResourceAsStream("identrust.crt")));
			keyStore.setEntry(String.format("certificate.%s",++certCount),new KeyStore.TrustedCertificateEntry(certificate.jceCert()),null);
		}

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null,tmf.getTrustManagers(),new SecureRandom());
		return sc;
	}

	private static interface VerificationHandle extends AutoCloseable {
		void sleep();
	}

	private static void showHelp(Options options) {
		new HelpFormatter().printHelp(CMD_LINE_SYNTAX,options);
	}

	private static Options options() {
		Options options = new Options();

		options.addOption(Option
			.builder()
				.longOpt("account-key")
				.hasArg().argName("path")
				.desc("account key file path")
				.required()
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("account-key-strength")
				.hasArg().argName("strength")
				.desc(String.format("account key strength (default %s)",DEFAULT_ACCOUNT_KEY_STRENGTH))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("certificate")
				.hasArg().argName("path")
				.desc("certificate file path")
				.required()
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("certificate-key")
				.hasArg().argName("strength")
				.desc("certificate key file path")
				.required()
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("certificate-key-strength")
				.hasArg().argName("path")
				.desc(String.format("certificate key strength (default %s)",DEFAULT_CERTIFICATE_KEY_STRENGTH))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("hostname")
				.hasArg().argName("hostname")
				.desc("main host (subject) name")
				.required()
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("alt-name")
				.hasArg().argName("hostname")
				.desc("alternative host (subject) name")
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("mode")
				.hasArg().argName("mode")
				.desc("verification mode (server|directory)")
				.required()
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("server-bind-address")
				.hasArg().argName("host")
				.desc(String.format("bind address (default %s)",DEFAULT_BIND_HOST))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("server-bind-port")
				.hasArg().argName("port")
				.desc(String.format("bind port (default %s)",DEFAULT_BIND_PORT))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("acme-directory")
				.hasArg().argName("path")
				.desc("acme directory path")
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("certificate-min-valid-days")
				.hasArg().argName("days")
				.type(Long.class)
				.desc(String.format("minimum valid days requirement for existing certificate (default %s)",DEFAULT_CERTIFICATE_MIN_VALID_DAYS))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("help")
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("acme-server")
				.hasArg().argName("url")
				.desc(String.format("acme server base url; default:\n%s",DEFAULT_ACME_SERVER))
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("embedded-identrust-root")
				.desc("use embedded identrust root cert")
				.build()
		);

		options.addOption(Option
			.builder()
				.longOpt("log-level")
				.hasArg().argName("level")
				.desc(String.format("log level (default %s)",DEFAULT_LOG_LEVEL))
				.build()
		);

		return options;
	}

}
