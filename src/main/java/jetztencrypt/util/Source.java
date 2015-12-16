package jetztencrypt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Source {

	InputStream inputStream() throws IOException;
	default Reader reader() throws IOException { return new InputStreamReader(inputStream(),StandardCharsets.UTF_8); }

	static Source of(Path path) {
		return ()->Files.newInputStream(path);
	}

	static Source of(InputStream stream) {
		return ()->stream;
	}

}
