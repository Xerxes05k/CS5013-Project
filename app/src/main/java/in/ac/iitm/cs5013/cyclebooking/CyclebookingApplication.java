package in.ac.iitm.cs5013.cyclebooking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CyclebookingApplication {

	public static void main(String[] args) {
		// SQLite will not create a missing parent directory for its database file.
		try {
			Files.createDirectories(Path.of("data"));
		} catch (IOException e) {
			throw new UncheckedIOException("Could not create the data/ directory for the SQLite database", e);
		}
		SpringApplication.run(CyclebookingApplication.class, args);
	}

}
