package jetztencrypt.client;

import org.apache.http.Header;

import com.google.common.base.Verify;

public class NewCert implements ResponseObject {

	public final Response response;

	public NewCert(Response response) {
		this.response = Verify.verifyNotNull(response);
	}

	public String location() {
		Header[] headers = response.httpResponse.getHeaders("Location");
		Verify.verify(headers!=null && headers.length==1);
		return Verify.verifyNotNull(headers[0].getValue());
	}

	@Override public Response response() { return response; }

}
