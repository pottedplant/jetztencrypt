package jetztencrypt.client;

import com.google.common.base.Verify;

public class Challenge implements ResponseObject {

	public final Response response;

	public Challenge(Response response) {
		this.response = Verify.verifyNotNull(response);
	}

	public String status() {
		return response.content.get("status").textValue();
	}

	@Override public Response response() { return response; }

}
