package jetztencrypt.client;

import org.apache.http.Header;

import com.google.common.base.Verify;

public class NewRegistration implements ResponseObject {

	public final Response response;

	public NewRegistration(Response response) {
		this.response = Verify.verifyNotNull(response);
	}

	public String link(String type) {
		Header[] headers = response.httpResponse.getHeaders("Link");
		if( headers==null ) return null;

		String suffix = String.format(">;rel=\"%s\"",type);

		for(Header header:headers) {
			String value = header.getValue();
			if( value.startsWith("<") && value.endsWith(suffix) )
				return value.substring(1,value.length()-suffix.length());
		}

		return null;
	}

	public String registrationUri() {
		Header[] headers = response.httpResponse.getHeaders("Location");
		Verify.verify( headers!=null && headers.length==1 );
		return Verify.verifyNotNull(headers[0].getValue());
	}

	@Override public Response response() { return response; }

}
