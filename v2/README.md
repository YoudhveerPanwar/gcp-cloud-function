<img src="https://avatars2.githubusercontent.com/u/2810941?v=3&s=96" alt="Google Cloud Platform logo" title="Google Cloud Platform" align="right" height="96" width="96"/>

# Google Cloud Functions Java Samples

[Cloud Functions][functions_docs] is a lightweight, event-based, asynchronous
compute solution that allows you to create small, single-purpose functions that
respond to Cloud events without the need to manage a server or a runtime
environment.

[functions_docs]: https://cloud.google.com/functions/docs/

## Samples

* [Cloud Pub/Sub](pubsub/)
This sample will stop/start cloud sql instance using cloud function based on pubsub trigger. You can trigger pubsub message as startInstance or stopInstance (using cloud schedular or some other script/api).

## Running Functions Locally
The [Java Functions Framework](https://github.com/GoogleCloudPlatform/functions-framework-java)
Maven plugin (`com.google.cloud.functions:function-maven-plugin`) allows you to run Java Cloud
Functions code on your local machine for local development and testing purposes. Use the following
Maven command to run a function locally:

```
mvn function:run -Drun.functionTarget=functions.PubSubFunction
```

Now run localhost via any http client for stopping sql instance, here data is in base64 decoded format
```
curl localhost:8080 \
  -X POST \
  -H "Content-Type: application/json" \
  -H "ce-id: 123451234512345" \
  -H "ce-specversion: 1.0" \
  -H "ce-time: 2024-16-05T18:14:56.789Z" \
  -H "ce-type: google.cloud.pubsub.topic.v1.messagePublished" \
  -H "ce-source: //pubsub.googleapis.com/projects/MY-PROJECT/topics/MY-TOPIC" \
  -d '{"message": {"data": "c3RvcEluc3RhbmNl", "attributes": { "attr1":"attr1-value"  }},  "subscription": "projects/MY-PROJECT/subscriptions/MY-SUB"  }'

```

## Running Functions on google project
1. Make sure you deployed the function with service account having appropriate permissions
2. This function will accepth pubsub message from a trigger as startInstance or stopInstance
3. Send this message from cron schedular to pubsub to stop or start sql instance accordingly



* [http](http/)

## HttpBased trigger

Now run http based function for stopping sql instance,
```
curl -m 70 -X POST https://<functionURI> \
-H "Authorization: bearer $(gcloud auth print-identity-token)" \
-H "Content-Type: application/json" \
-d '{
  "action": "stopInstance"
}'

```