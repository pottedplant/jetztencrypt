package jetztencrypt.bundle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BundleMain {

	// defs

	private static final Collator COLLATOR = Collator.getInstance(Locale.ENGLISH);
	private static final String MAIN_JAR = "main.jar";
	private static final String MAIN_CLASS = "jetztencrypt.app.Application";

	// impl

	public static void main(String[] args) throws Throwable {
		Map<String,Map<String,byte[]>> jars = new LinkedHashMap<>();

//		System.out.println("preloading jars..");

		// load all jars to memory
		ClassLoader initialClassLoader = BundleMain.class.getClassLoader();
		jars.put(MAIN_JAR,readJar(initialClassLoader,MAIN_JAR));
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(initialClassLoader.getResourceAsStream("classpath"),StandardCharsets.UTF_8))) {
			for(String line=reader.readLine();line!=null;line=reader.readLine())
				jars.put(line,readJar(initialClassLoader,line));
		}

		BundleUrlStreamHandler urlStreamHandler = new BundleUrlStreamHandler(jars);
		urlStreamHandler.register();

		ClassLoader bundleClassLoader = new BundleClassLoader(jars,urlStreamHandler,initialClassLoader);

//		System.out.println("starting application..");

		Thread.currentThread().setContextClassLoader(bundleClassLoader);
		bundleClassLoader.loadClass(MAIN_CLASS).getMethod("main",String[].class).invoke(null,(Object)args);
	}

	// pimpl

	private static Map<String,byte[]> readJar(ClassLoader cl,String path) throws IOException {
		Map<String,byte[]> files = new TreeMap<>(COLLATOR);
		byte[] buffer = new byte[16*1024];

		try(ZipInputStream zipStream = new ZipInputStream(cl.getResourceAsStream(path),StandardCharsets.UTF_8)) {
			for(ZipEntry zipEntry = zipStream.getNextEntry();zipEntry!=null;zipEntry=zipStream.getNextEntry()) {
				if( zipEntry.isDirectory() ) continue;

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				for(int bytes=zipStream.read(buffer);bytes!=-1;bytes=zipStream.read(buffer))
					baos.write(buffer,0,bytes);

				files.put(zipEntry.getName(),baos.toByteArray());
			}
		}

		return files;
	}

}