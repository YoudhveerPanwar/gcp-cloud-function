package functions;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.gson.Gson;
import io.cloudevents.CloudEvent;
import java.util.Base64;
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/***
 * Stop/Start SQL instance through cloud function triggered by PubSub message
 * 
 * @author youdhveer
 *
 */

public class PubSubFunction implements CloudEventsFunction {
  private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());
  private static final String STOP_INSTANCE = "stopInstance";
  private static final String START_INSTANCE = "startInstance";
  
  
  //Pass this through running gcp project as an environment variable
  private static final String PROJECT = getEnvValue("PROJECT"); 
  private static final String INSTANCE = getEnvValue("INSTANCE");


  @Override
  public void accept(CloudEvent event) {

    try {
      // Get cloud event data as JSON string
      String cloudEventData = new String(event.getData().toBytes());
      // Decode JSON event data to the Pub/Sub MessagePublishedData type
      Gson gson = new Gson();
      MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);
      // Get the message from the data
      Message message = data.getMessage();
      // Get the base64-encoded data from the message & decode it
      String encodedData = message.getData();
      String decodedData = new String(Base64.getDecoder().decode(encodedData));
      if (decodedData.contains(STOP_INSTANCE)) {
        processSQLInstance(STOP_INSTANCE);
      } else if (decodedData.contains(START_INSTANCE)) {
        processSQLInstance(START_INSTANCE);
      }
      logger.info("Pub/Sub message : " + decodedData);
    } catch (Exception e) {
      logger.severe("Failed to start/stop sql instance :" + e.getMessage());
      e.printStackTrace();
    }
    
  }

  public void processSQLInstance(String action) throws Exception {
    DatabaseInstance requestBody = new DatabaseInstance();
    String activationPolicy = STOP_INSTANCE.equalsIgnoreCase(action)?"NEVER" : "ALWAYS";
    logger.info("action : "+action+" and activationPolicy : "+activationPolicy);
    requestBody.setSettings(new Settings().setActivationPolicy(activationPolicy));
    SQLAdmin sqlAdminService = createSqlAdminService();
    logger.info("Star/stop sql instance : " + action + " for project : " + PROJECT);
    if(PROJECT==null || INSTANCE ==null) {
      throw new Exception("Please provide valid projectId and sql instance name [PROJECT,INSTANCE] ");
    }
    SQLAdmin.Instances.Patch request =
        sqlAdminService.instances().patch(PROJECT, INSTANCE, requestBody);
    Operation response = request.execute();
    logger.info("Response : " + response);
  }

  public SQLAdmin createSqlAdminService() throws IOException, GeneralSecurityException {
    logger.info("Creating sql admin service ....");
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    GoogleCredential credential = GoogleCredential.getApplicationDefault();
    if (credential.createScopedRequired()) {
      credential =
          credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
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
