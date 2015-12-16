package jetztencrypt.client;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Verify;

public class Response implements Closeable {

	public final CloseableHttpResponse httpResponse;
	public final ObjectNode content;

	Response(CloseableHttpResponse httpResponse,ObjectNode content) {
		this.httpResponse = Verify.verifyNotNull(httpResponse);
		this.content = content;
	}

	public int status() {
		return httpResponse.getStatusLine().getStatusCode();
	}

	public String nonce() {
		Header[] headers = httpResponse.getHeaders("Replay-Nonce");
		Verify.verify(headers!=null && headers.length==1,"invalid reply-nonce header(s)");
		return Verify.verifyNotNull(headers[0].getValue());
	}

	@Override
	public void close() throws IOException {
		httpResponse.close();
	}

}
