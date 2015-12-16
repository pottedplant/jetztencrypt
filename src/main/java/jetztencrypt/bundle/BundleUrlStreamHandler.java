package jetztencrypt.bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class BundleUrlStreamHandler extends URLStreamHandler {

	// defs

	private static final String PROTOCOL = "bundle";

	// state

	private final Map<String,Map<String,byte[]>> sources;

	// impl

	public BundleUrlStreamHandler(Map<String,Map<String,byte[]>> sources) {
		this.sources = sources;
	}

	public Enumeration<URL> find(String path) {
		ArrayList<URL> result = new ArrayList<>();

		for(Map.Entry<String,Map<String,byte[]>> e:sources.entrySet())
			if( e.getValue().containsKey(path) )
				try {
					result.add(new URL(PROTOCOL,e.getKey(),path));
				} catch(MalformedURLException ex) {
					throw new RuntimeException(ex);
				}

		return Collections.enumeration(result);
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		byte[] data = sources.get(url.getHost()).get(url.getPath());
		return new Connection(url,data);
	}

	public void register() {
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
			@Override
			public URLStreamHandler createURLStreamHandler(String protocol) {
				if( PROTOCOL.equals(protocol) )
					return BundleUrlStreamHandler.this;
				return null;
			}
		});
	}

	// connection

	private class Connection extends URLConnection {

		private final byte[] data;

		protected Connection(URL url,byte[] data) {
			super(url);
			this.data = data;
		}

		@Override
		public void connect() throws IOException {
			if( data==null )
				throw new IOException(String.format("no such bundle resource: %s",getURL().getPath()));
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(data);
		}

	}

}