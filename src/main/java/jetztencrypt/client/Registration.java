package jetztencrypt.client;

import org.apache.http.Header;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Verify;

public class Registration implements ResponseObject {

	public final Response response;

	public Registration(Response response) {
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

	public String termsOfService() {
		return link("terms-of-service");
	}

	public String agreement() {
		JsonNode agreement = response.content.get("agreement");
		if( agreement==null ) return null;
		return agreement.textValue();
	}

	@Override public Response response() { return response; }

}
