package jetztencrypt.client;

public interface ResponseObject {

	Response response();
	default String nonce() { return response().nonce(); }

}
