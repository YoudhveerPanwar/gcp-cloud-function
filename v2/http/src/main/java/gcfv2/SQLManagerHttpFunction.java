package gcfv2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.logging.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/***
 * Stop/Start SQL instance through Http cloud function triggered by
 * CloudSchedular
 * 
 * @author youdhveer_panwar
 *
 */

public class SQLManagerHttpFunction implements HttpFunction {
	private static final Logger logger = Logger.getLogger(SQLManagerHttpFunction.class.getName());
	private static final Gson gson = new Gson();

	private static final String STOP_INSTANCE = "stopInstance";
	private static final String START_INSTANCE = "startInstance";
	private static final String PROJECT = getEnvValue("PROJECT");
	private static final String INSTANCE = getEnvValue("INSTANCE");


	public void service(final HttpRequest request, final HttpResponse response) throws Exception {
		final BufferedWriter writer = response.getWriter();

		JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
		String action = "";
		if (body.has("action")) {
			action = body.get("action").getAsString();
		}
		if(START_INSTANCE.equalsIgnoreCase(action) || STOP_INSTANCE.equalsIgnoreCase(action)) {
			processSQLInstance(action);
		}else {
			throw new Exception("Not a supported action parameter "+action);
		}

		writer.write("Success");

	}

	public void processSQLInstance(String action) throws Exception {
		DatabaseInstance requestBody = new DatabaseInstance();
		String activationPolicy = STOP_INSTANCE.equalsIgnoreCase(action) ? "NEVER" : "ALWAYS";
		logger.info("action : " + action + " and activationPolicy : " + activationPolicy);
		requestBody.setSettings(new Settings().setActivationPolicy(activationPolicy));
		SQLAdmin sqlAdminService = createSqlAdminService();
		logger.info("Star/stop sql instance : " + action + " for project : " + PROJECT);
		if (PROJECT == null || INSTANCE == null) {
			throw new Exception("Please provide valid projectId and sql instance name [PROJECT,INSTANCE] ");
		}
		SQLAdmin.Instances.Patch request = sqlAdminService.instances().patch(PROJECT, INSTANCE, requestBody);
		Operation response = request.execute();
		logger.info("Response : " + response);
	}

	public SQLAdmin createSqlAdminService() throws IOException, GeneralSecurityException {
		logger.info("Creating sql admin service ....");
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		GoogleCredential credential = GoogleCredential.getApplicationDefault();
		if (credential.createScopedRequired()) {
			credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
		}

		return new SQLAdmin.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("Google-SQLAdminSample/0.1").build();
	}

	public static String getEnvValue(String key) {
		return getEnvValue(key, null);
	}

	private static String getEnvValue(String key, String defaultValue) {
		return System.getenv().getOrDefault(key, System.getProperty(key, defaultValue));
	}

}
