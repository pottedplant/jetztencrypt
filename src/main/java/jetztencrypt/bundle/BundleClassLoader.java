package jetztencrypt.bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleClassLoader extends ClassLoader {

	// state

	private final Map<String,Map<String,byte[]>> jars;
	private final BundleUrlStreamHandler urlStreamHandler;

	// impl

	static { registerAsParallelCapable(); }

	public BundleClassLoader(Map<String,Map<String,byte[]>> jars,BundleUrlStreamHandler urlStreamHandler, ClassLoader parent) {
		super(parent);
		this.urlStreamHandler = urlStreamHandler;
		this.jars = jars;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		String path = name.replaceAll(Pattern.quote("."),Matcher.quoteReplacement("/"))+".class";

		for(Map<String,byte[]> jar:jars.values()) {
			byte[] data = jar.get(path);
			if( data==null ) continue;

			return defineClass(name,data,0,data.length,null);
		}

		return super.findClass(name);
	}

	@Override
	protected URL findResource(String name) {
		try {
			Enumeration<URL> e = this.findResources(name);
			if( !e.hasMoreElements() ) return null;
			return e.nextElement();
		} catch(IOException e) {
			throw new RuntimeException(String.format("while finding resource '%s'",name),e);
		}
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return urlStreamHandler.find(name);
	}

}