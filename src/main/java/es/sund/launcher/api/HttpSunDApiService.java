package es.sund.launcher.api;

import com.google.gson.Gson;
import es.sund.launcher.config.AppConstants;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.model.AccountCheckResponse;
import es.sund.launcher.model.GameCatalogResponse;
import es.sund.launcher.model.GameSessionTokenResponse;
import es.sund.launcher.model.VersionCheckResponse;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class HttpSunDApiService implements SunDApiService {

	private final Gson gson = new Gson();
	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(AppConstants.CONNECT_TIMEOUT_SECONDS)).build();

	@Override
	public AccountCheckResponse checkAccount(String username, char[] password)
			throws ApiTimeoutException, ApiConnectionException {

		String jsonBody = gson.toJson(Map.of("username", username, "password", new String(password)));

		HttpRequest request = HttpRequest.newBuilder(URI.create(AppConstants.API_CHECK_ACCOUNT_ENDPOINT))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(AppConstants.REQUEST_TIMEOUT_SECONDS))
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)).build();

		HttpResponse<String> response = send(request);

		if (response.statusCode() != 200) {
			AccountCheckResponse fallback = new AccountCheckResponse();
			fallback.success = false;
			fallback.message = "El servidor respondió con un error (HTTP " + response.statusCode() + ")";
			return fallback;
		}
		return gson.fromJson(response.body(), AccountCheckResponse.class);
	}

	@Override
	public VersionCheckResponse checkLauncherVersion() throws ApiTimeoutException, ApiConnectionException {
	    String url = AppConstants.API_CHECK_LAUNCHER_VERSION_ENDPOINT
	            + "?version=" + AppConstants.CURRENT_LAUNCHER_VERSION
	            + "&data_val=1";

	    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
	            .timeout(Duration.ofSeconds(AppConstants.REQUEST_TIMEOUT_SECONDS))
	            .GET()
	            .build();

	    HttpResponse<String> response = send(request);

	    if (response.statusCode() != 200) {
	        throw new ApiConnectionException(
	                "Error desconocido || Servidor fuera de servicio"
	        );
	    }

	    return gson.fromJson(response.body(), VersionCheckResponse.class);
	}

	@Override
	public GameCatalogResponse fetchGameCatalog() throws ApiTimeoutException, ApiConnectionException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(AppConstants.API_GAME_CATALOG_ENDPOINT))
				.timeout(Duration.ofSeconds(AppConstants.REQUEST_TIMEOUT_SECONDS))
				.GET()
				.build();

		HttpResponse<String> response = send(request);

		if (response.statusCode() != 200) {
			throw new ApiConnectionException(
					"Error desconocido || Servidor fuera de servicio"
			);
		}

		return gson.fromJson(response.body(), GameCatalogResponse.class);
	}

	@Override
	public GameSessionTokenResponse requestGameSessionToken(String username, char[] password)
			throws ApiTimeoutException, ApiConnectionException {

		String jsonBody = gson.toJson(Map.of("username", username, "password", new String(password)));

		HttpRequest request = HttpRequest.newBuilder(URI.create(AppConstants.API_GAME_SESSION_TOKEN_ENDPOINT))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(AppConstants.REQUEST_TIMEOUT_SECONDS))
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)).build();

		HttpResponse<String> response = send(request);

		if (response.statusCode() != 200) {
			GameSessionTokenResponse fallback = new GameSessionTokenResponse();
			fallback.success = false;
			fallback.message = "El servidor respondió con un error (HTTP " + response.statusCode() + ")";
			return fallback;
		}
		return gson.fromJson(response.body(), GameSessionTokenResponse.class);
	}

	private HttpResponse<String> send(HttpRequest request) throws ApiTimeoutException, ApiConnectionException {
		try {
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (HttpTimeoutException e) {
			throw new ApiTimeoutException("El servidor tardó demasiado en responder", e);
		} catch (ConnectException e) {
			throw new ApiConnectionException("No se pudo conectar con el servidor", e);
		} catch (java.io.IOException e) {
			throw new ApiConnectionException("Error de red al hablar con el servidor: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ApiConnectionException("La petición fue interrumpida", e);
		}
	}
}
