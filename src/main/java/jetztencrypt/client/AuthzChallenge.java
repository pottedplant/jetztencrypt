package jetztencrypt.client;

public class AuthzChallenge {

	public final String type;
	public final String uri;
	public final String token;

	public AuthzChallenge(String type,String uri,String token) {
		this.type = type;
		this.uri = uri;
		this.token = token;
	}

}
