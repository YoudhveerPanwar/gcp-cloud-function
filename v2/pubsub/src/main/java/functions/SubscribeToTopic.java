
package functions;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.gson.Gson;
import functions.eventpojos.PubSubBody;
import io.cloudevents.CloudEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;

public class SubscribeToTopic implements CloudEventsFunction {
  private static final Logger logger = Logger.getLogger(SubscribeToTopic.class.getName());

  @Override
  public void accept(CloudEvent event) {
    // The Pub/Sub message is passed as the CloudEvent's data payload.
    if (event.getData() != null) {
      // Extract Cloud Event data and convert to PubSubBody
      String cloudEventData = new String(event.getData().toBytes(), StandardCharsets.UTF_8);
      Gson gson = new Gson();
      PubSubBody body = gson.fromJson(cloudEventData, PubSubBody.class);
      // Retrieve and decode PubSub message data
      String encodedData = body.getMessage().getData();
      String decodedData =
          new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
      logger.info("Hello, " + decodedData + "!");
    }
  }
}
