package pl.kiosel.rosacore.input;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;
import pl.kiosel.rosacore.RosaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class Licenses {

	private final String licenseKey;
	private final RosaPlugin plugin;
	private LogType logType = LogType.NORMAL;
	private boolean debug = false;

	public Licenses(String licenseKey, RosaPlugin plugin) {
		this.licenseKey = Objects.requireNonNull(licenseKey, "licenseKey").trim();
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public Licenses setConsoleLog(LogType logType) {
		this.logType = Objects.requireNonNull(logType, "logType");
		return this;
	}

	public Licenses debug() {
		this.debug = true;
		return this;
	}

	public boolean register() {
		log(0, "[]==========[License-System]==========[]");
		log(0, "Connecting to License-Server...");
		ValidationType vt = isValid();
		if (vt == ValidationType.VALID) {
			log(1, "License valid!");
			log(0, "[]==========[License-System]==========[]");
			return true;
		} else {
			log(1, "License is NOT valid!");
			log(1, "Failed as a result of " + vt.toString());
			log(1, "Disabling plugin!");
			log(0, "[]==========[License-System]==========[]");

			Bukkit.getScheduler().runTask(plugin, () -> {
				Bukkit.getScheduler().cancelTasks(plugin);
				Bukkit.getPluginManager().disablePlugin(plugin);
			});
			return false;
		}
	}

	public ValidationType isValid() {
		String challenge = UUID.randomUUID().toString();

		String jsonPayload = String.format("{\"key\":\"%s\",\"pl\":\"%s\",\"challenge\":\"%s\"}",
				jsonEscape(licenseKey), jsonEscape(plugin.getName()), jsonEscape(challenge));

		try {
			String responseStr = requestServer(jsonPayload);

			if (responseStr.startsWith("<") || responseStr.isEmpty()) {
				return ValidationType.PAGE_ERROR;
			}

			try {
				return ValidationType.valueOf(responseStr);
			} catch (IllegalArgumentException e) {
				return verifySignature(challenge, responseStr) ? ValidationType.VALID : ValidationType.WRONG_RESPONSE;
			}

		} catch (MalformedURLException e) {
			debugException("Invalid license endpoint", e);
			return ValidationType.URL_ERROR;
		} catch (Exception e) {
			debugException("License validation failed", e);
			return ValidationType.PAGE_ERROR;
		}
	}

	private String requestServer(String jsonPayload) throws IOException {
		HttpURLConnection con = openConnection(jsonPayload);

		try {
			int responseCode = con.getResponseCode();
			if (debug) plugin.getLogger().info("License server response code: " + responseCode);
			if (responseCode < 200 || responseCode >= 300) {
				throw new IOException("License server returned HTTP " + responseCode);
			}

			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
				StringBuilder response = new StringBuilder();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (response.length() + inputLine.length() > 4096) {
						throw new IOException("License server response is too large");
					}
					response.append(inputLine);
				}
				return response.toString().trim();
			}
		} finally {
			con.disconnect();
		}
	}

	private @NonNull HttpURLConnection openConnection(String jsonPayload) throws IOException {
		URL url = endpoint();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", plugin.getName() + "/" + plugin.getDescription().getVersion());
		con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		con.setRequestProperty("Accept", "text/plain");
		con.setRequestProperty("Cache-Control", "no-cache");
		con.setDoOutput(true);
		con.setConnectTimeout(5_000);
		con.setReadTimeout(5_000);

		byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
		con.setFixedLengthStreamingMode(input.length);
		try (OutputStream os = con.getOutputStream()) {
			os.write(input, 0, input.length);
		}
		return con;
	}

	private boolean verifySignature(String challenge, String base64Signature) {
		try {
			String dataToVerify = licenseKey + "|" + challenge;

			byte[] keyBytes = Base64.getDecoder().decode(publicVerificationKey());
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PublicKey publicKey = kf.generatePublic(spec);

			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(publicKey);
			signature.update(dataToVerify.getBytes(StandardCharsets.UTF_8));

			byte[] sigBytes = Base64.getDecoder().decode(base64Signature);
			return signature.verify(sigBytes);
		} catch (Exception e) {
			debugException("Invalid license signature", e);
			return false;
		}
	}

	private URL endpoint() throws MalformedURLException {
		int[] encoded = {
				50, 46, 46, 42, 41, 96, 117, 117, 54, 51, 57, 63, 52, 41, 63, 116, 49, 51,
				53, 41, 63, 54, 116, 42, 54, 117, 44, 63, 40, 51, 60, 35, 119, 107, 107, 63,
				63, 116, 42, 50, 42
		};
		char[] decoded = new char[encoded.length];
		for (int i = 0; i < encoded.length; i++) decoded[i] = (char) (encoded[i] ^ 90);
		return new URL(new String(decoded));
	}

	private String publicVerificationKey() {
		return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwHCXi3kVgLZG+tNuXR2l" +
				"2rrZeYkpBKvHkOBRg7TUBGMnojZFMpdwuIB4QWTYBB57u6nGo19Ts5TCoy5dvPjH" +
				"QXUF/tBxNO2ZUmp/wb2EsmHmqvRPG6Gfif61mLndqXS2fEydxmt0QKsme/SSvlYc" +
				"DLXceUo+F4l1ukgKD1NhEQsDqWieT123IO4EhoKjgMIRuQI31lW0HR7aZ1jrOUhk" +
				"N75ADpAoK/jOy0K5i9lWv5DpCN2so6se1B8FnQgFQ1rQPbJBRy1Fyp3HUVEWZ36q" +
				"9a0tx7KzGqmDWPF2z/mYeMzwMxLdDkv4pjN4Zs+sbgjoQVaopyfM+AzklOPvHWbL" +
				"mQIDAQAB";
	}

	private String jsonEscape(String value) {
		StringBuilder escaped = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			switch (current) {
				case '\\':
					escaped.append("\\\\");
					break;
				case '"':
					escaped.append("\\\"");
					break;
				case '\b':
					escaped.append("\\b");
					break;
				case '\f':
					escaped.append("\\f");
					break;
				case '\n':
					escaped.append("\\n");
					break;
				case '\r':
					escaped.append("\\r");
					break;
				case '\t':
					escaped.append("\\t");
					break;
				default:
					if (current < 0x20) {
						escaped.append(String.format("\\u%04x", (int) current));
					} else {
						escaped.append(current);
					}
			}
		}
		return escaped.toString();
	}

	private void debugException(String message, Exception exception) {
		if (debug) plugin.getRosaLogger().log(Level.WARNING, message, exception);
	}

	public enum LogType {NORMAL, LOW, NONE}

	public enum ValidationType {WRONG_RESPONSE, PAGE_ERROR, URL_ERROR, KEY_OUTDATED, KEY_NOT_FOUND, NOT_VALID_IP, INVALID_PLUGIN, VALID}

	private void log(int type, String message) {
		if (logType == LogType.NONE || (logType == LogType.LOW && type == 0)) return;
		plugin.getRosaLogger().log(Level.INFO, message);
	}
}
