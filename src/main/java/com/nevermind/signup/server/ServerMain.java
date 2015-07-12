package com.nevermind.signup.server;

// Packages for reading property files.
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Packages for normal MongoDB authentication.
import java.io.FileNotFoundException;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import java.util.Arrays;

// packages for MongoDB access.
import com.mongodb.client.MongoDatabase;

public class ServerMain {
	final private static String databasePropertyFile = "database.properties";

	/*
	 * Error code
	 *  1:	Incorrect CLI argument.
	 *	2:	Can't interpret the port number.
	 *	3: 	Can't load database properties.
	 *	4: 	Having trouble initiate the HTTP server.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java ServerMain <port number>");
			System.exit(1);
		}

		// Register the port number using the first commandline parameter.
		int portNumber = Integer.parseInt(args[0]);
		if (portNumber != 0) {
			System.err.println("Open port on " + portNumber + ".");
		} else {
			System.err.println("Can't parse the port.");
			System.exit(2);
		}

		ServerAddress seed = null;
		MongoCredential credential = null;
		try {
			seed = readConnectionInfoFromFile();
			credential = readCredentialFromFile();
		} catch (Exception e) {
			System.err.println("Can't load database properties.");
			System.exit(3);
		}

		// Create the client and establish the connection.
		MongoClient mongoClient = new MongoClient(seed, Arrays.asList(credential));
		MongoDatabase database = mongoClient.getDatabase(credential.getSource());

		// Start the HTTP server.
		IncomingListener server = new IncomingListener(portNumber, new IncomingHandler(database));
		server.start();
		System.out.println("Server is started and listening on port " + portNumber);
	}

	private static ServerAddress readConnectionInfoFromFile() throws IOException, FileNotFoundException {
		ServerAddress connectionInfo = null;

		Properties properties = new Properties();
		InputStream input = null;

		input = ServerMain.class.getClassLoader().getResourceAsStream(databasePropertyFile);
		if (input == null) {
			System.err.println("Unable to find \"" + databasePropertyFile + "\".");
			throw new FileNotFoundException();
		}

		// Load the property file.
		properties.load(input);

		// Load the fields.
		String address = properties.getProperty("address");
		String port = properties.getProperty("port");
		connectionInfo = new ServerAddress(address, Integer.parseInt(port));

		input.close();

		return connectionInfo;
	}

	private static MongoCredential readCredentialFromFile() throws IOException, FileNotFoundException {
		MongoCredential credential = null;

		Properties properties = new Properties();
		InputStream input = null;

		// Check if file exists, otherwise, use the default setup.
		File customProperties = new File(databasePropertyFile);
		if (customProperties.exists()) {
			input = new FileInputStream(databasePropertyFile);
		} else {
			input = ServerMain.class.getClassLoader().getResourceAsStream(databasePropertyFile);
			if (input == null) {
				System.err.println("Unable to find \"" + databasePropertyFile + "\".");
				throw new FileNotFoundException();
			}
		}

		// Load the property file.
		properties.load(input);

		// Load the fields.
		String username = properties.getProperty("username");
		String password = properties.getProperty("password");
		String database = properties.getProperty("database");
		credential = MongoCredential.createCredential(username, database, password.toCharArray());

		input.close();

		return credential;
	}
}