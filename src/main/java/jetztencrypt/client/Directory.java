package jetztencrypt.client;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class Directory implements ResponseObject {

	public final Response response;

	public Directory(Response response) {
		this.response = Verify.verifyNotNull(response);
	}

	public String resourceUrl(String resource) {
		return Verify.verifyNotNull(Verify.verifyNotNull(response.content.get(resource)).textValue());
	}

	public ImmutableMap<String,String> resourceUrls() {
		Builder<String,String> r = ImmutableMap.<String,String>builder();

		for(Iterator<Entry<String,JsonNode>> i=response.content.fields();i.hasNext();) {
			Entry<String,JsonNode> e = i.next();
			r.put(e.getKey(),Verify.verifyNotNull(e.getValue().textValue(),"null or non text value for key '%s'",e.getKey()));
		}

		return r.build();
	}

	@Override public Response response() { return response; }

}
