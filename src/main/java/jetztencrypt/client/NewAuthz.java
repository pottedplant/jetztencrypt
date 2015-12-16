package jetztencrypt.client;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Verify;

public class NewAuthz implements ResponseObject {

	public final Response response;

	public NewAuthz(Response response) {
		this.response = Verify.verifyNotNull(response);
	}

	public AuthzChallenge firstOfType(String type) {
		for(JsonNode challenge:Verify.verifyNotNull(response.content.get("challenges")))
			if( Objects.equals(type,Verify.verifyNotNull(challenge.get("type")).textValue()))
				return new AuthzChallenge(
					type,
					challenge.get("uri").textValue(),
					challenge.get("token").textValue()
				);

		return null;
	}

	@Override public Response response() { return response; }

}
