package org.freemedsoftware.util.exchangebackup;

import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.webdav.lib.methods.SearchMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ExchangeBackup {

	public static String[] AVAILABLE_PROTOCOLS = { "http", "https" };

	/**
	 * @param args
	 */
	public static void main(String[] argv) {
		String host = "";
		String user = "";
		String pass = "";
		String domain = "";
		String version = "";
		String protocol = "https";
		String folder = "Contacts";
		Integer port = 443;

		for (int optind = 0; optind < argv.length; optind++) {
			if (argv[optind].equals("--host")) {
				host = argv[++optind];
			} else if (argv[optind].equals("--protocol")) {
				protocol = argv[++optind];
				if (!Arrays.asList(AVAILABLE_PROTOCOLS).contains(protocol)) {
					System.out.println("'" + protocol
							+ "' is not a valid protocol");
					System.exit(1);
				}
			} else if (argv[optind].equals("--username")) {
				user = argv[++optind];
			} else if (argv[optind].equals("--version")) {
				version = argv[++optind];
			} else if (argv[optind].equals("--password")) {
				pass = argv[++optind];
			} else if (argv[optind].equals("--port")) {
				port = Integer.parseInt(argv[++optind]);
			} else if (argv[optind].equals("--domain")) {
				domain = argv[++optind];
			} else if (argv[optind].equals("--folder")) {
				folder = argv[++optind];
			} else if (argv[optind].equals("--")) {
				optind++;
				break;
			} else if (argv[optind].startsWith("-")) {
				System.out.println("Could not identify option " + argv[optind]);
				printSyntax();
			} else {
				break;
			}
		}

		if (user == "" || pass == "" || domain == "" || host == "") {
			printSyntax();
			System.exit(1);
		}

		if (version == "") {
			version = "2007";
		}
		
		String prefix = null;
		if (version == "2003") {
			prefix = "Exchange";
		}
		if (version == "2007") {
			prefix = "owa";
		}
		
		String url = null;

		if (version == "2003") {
			url = protocol + "://" + host + "/" + prefix + "/" + user + "/"
					+ folder + (folder.contains("?") ? "" : "/");
		} else if (version == "2007") {
			url = protocol + "://" + host + "/" + prefix + "/" + user + "/"
					+ "?cmd=contents&module=" + folder;
		}
		System.out.println("URL: " + url);
		
		NTCredentials credentials = new NTCredentials(user, pass, host, domain);

		HttpClient httpClient = new HttpClient();
		httpClient.getState().setCredentials(null, host, credentials);

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\"?>");
		xml.append("<a:searchrequest xmlns:a=\"DAV:\">");
		xml.append("<a:sql>");
		xml.append("SELECT * FROM \"");
		xml.append(url);
		xml.append("\"");
		xml.append("</a:sql>");
		xml.append("</a:searchrequest>");

		try {
			String folderPath = "/" + prefix + "/" + user + "/" + folder + "/";
			if (version == "2003") {
				folderPath = protocol + "://" + host + "/" + prefix + "/" + user + "/"
						+ folder + (folder.contains("?") ? "" : "/");
			} else if (version == "2007") {
				folderPath = protocol + "://" + host + "/" + prefix + "/" + user + "/"
						+ "?cmd=contents&module=" + folder;
			}
			HostConfiguration hostConfiguration = new HostConfiguration();
			hostConfiguration.setHost(host, port, protocol);

			SearchMethod searchMethod = new SearchMethod(folderPath);
			searchMethod.setFollowRedirects(true);
			searchMethod.setRequestHeader("Content-Type", "text/xml");
			searchMethod.setRequestHeader("Depth", "0");
			searchMethod.setRequestHeader("Translate", "f");
			// searchMethod.setRequestHeader("Content-Length", ""+
			// query.length());
			searchMethod.setRequestBody(xml.toString());
			searchMethod.setHostConfiguration(hostConfiguration);

			// searchMethod.setDebug(1);

			httpClient.executeMethod(searchMethod);
			Document responseDocument = searchMethod.getResponseDocument();
			System.out.println(xmlToString((Node) responseDocument
					.getDocumentElement()));
			searchMethod.releaseConnection();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static String xmlToString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void printSyntax() {
		System.out.println("Usage: java -jar ExchangeBackup.jar [options]");
		System.out.println("\tOptions:");
		System.out.println("\t[--host source hostname]");
		System.out.println("\t[--port source port (defaults to 443)]");
		System.out
				.println("\t[--protocol source protocol (defaults to https)]");
		System.out.println("\t[--username (username)]");
		System.out.println("\t[--password (password)]");
		System.out.println("\t[--domain (domain)]");
		System.out.println("\t[--version (exchange version [2003, 2007])]");
		System.out.println("\t[--folder (folder) (defaults to 'Contacts')");
		System.exit(1);
	}

}
