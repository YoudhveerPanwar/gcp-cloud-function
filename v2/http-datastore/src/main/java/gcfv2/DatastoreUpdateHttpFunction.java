package gcfv2;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.List;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Value;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


/***
 * This is a http based function which can connect google datastore/firestore
 *   and update all email ids in datastore entity User from old domain to new domain
 *   Example -> youdhveer_singh@yahoo.com -> youdhveer.singh@google.com
 * 
 * @author youdhveer
 *
 */
public class DatastoreUpdateHttpFunction implements HttpFunction {
  
  private static final Gson gson = new Gson();

  //Instantiates a client
  Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private static final String OLD_DOMAIN="yahoo.com";
  private static final String NEW_DOMAIN="google.com";
  
  
  /***
   *     curl -m 70 -X POST https://<function-URI> \
   *      -H "Authorization: bearer $(gcloud auth print-identity-token)" \
   *      -H "Content-Type: application/json" \
   *      -d ' {  "kind": "user",  "oldDomain":"yahoo.com",  "newDomain":"google.com",  "property": "email",  "limit":100,  "offset":0  }'  
   */
  public void service(final HttpRequest request, final HttpResponse response) throws Exception {
    final BufferedWriter writer = response.getWriter();
   
    JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
    String kind="User";
    String property="email";
    String oldDomain=OLD_DOMAIN;
    String newDomain=NEW_DOMAIN;
    int offset=0;
    int limit = 1000;
    String type=DatatStoreType.STRING.name();
    
    
    if (body.has("kind")) {
      kind = body.get("kind").getAsString();
    }
    if (body.has("property")) {
      property = body.get("property").getAsString();
    }
    if (body.has("offset")) {
      offset = body.get("offset").getAsInt();
    }
    if (body.has("limit")) {
      limit = body.get("limit").getAsInt();
    }
    if (body.has("oldDomain")) {
      oldDomain = body.get("oldDomain").getAsString();
    }
    if (body.has("newDomain")) {
      newDomain = body.get("newDomain").getAsString();
    }

    if (body.has("type")) {
      type = body.get("type").getAsString();
    }
    readDatastoreEntity(kind, property, offset, limit,oldDomain,newDomain, type);
    
    writer.write("Entities updated");
    
  }
  
  /****
   * This method will update all email ids in datastore entity User from old domain to new domain
   *  Example -> youdhveer_singh@yahoo.com -> youdhveer.singh@google.com
   * 
   * @param kind
   * @param propertyName
   * @param offset
   * @param limit
   * @param oldDomain
   * @param newDomain
   * @param type
   * @throws Exception
   */
  public void readDatastoreEntity(String kind, String propertyName, int offset, int limit, String oldDomain, String newDomain, String type) throws Exception {

    System.out.println("read datastore entities : "+kind+" and propertyName :"+propertyName+" and type:"+type);
   
   
    Iterator<Entity> results =  listEntities(kind, offset, limit);
    

    if (!results.hasNext()) {
      throw new Exception("query yielded no results");
    }

    while (results.hasNext()) {
      Entity entity = results.next();
      String email =null;
      List<Value> fieldType=null;
      if(DatatStoreType.ARRAY.name().equalsIgnoreCase(type)) {
       // fieldType = entity.getList(propertyName);
        String subEntityName =propertyName.split("\\.")[0];
        String subPropertyName= propertyName.split("\\.")[1];
        FullEntity<IncompleteKey> obj = entity.getEntity(subEntityName);
        System.out.printf("FullEntity with : %s%n", obj);
        String senderEmail= obj.getString(subPropertyName);
        System.out.println("Child entity prop email : "+senderEmail);
        if(senderEmail.indexOf(oldDomain)!=-1) {
          String newEmail = senderEmail.replaceAll(oldDomain, newDomain).replaceAll("_", ".");
          System.out.println("email : "+senderEmail +" is now "+newEmail);
          FullEntity<IncompleteKey> subEntityObj = Entity.newBuilder(obj).set(subPropertyName, newEmail).build();
          Entity entityObj = Entity.newBuilder(entity).set(subEntityName, subEntityObj).build();
          datastore.update(entityObj);
          System.out.println("Entity updated with sub entity.");
        }
        
        
      }else if(DatatStoreType.STRING.name().equalsIgnoreCase(type)) {
        email = entity.getString(propertyName);
        
        if(email.indexOf(oldDomain)!=-1) {
          String newEmail = email.replaceAll(oldDomain, newDomain).replaceAll("_", ".");
          System.out.println("email : "+email +" is now "+newEmail);
          Entity entityObj = Entity.newBuilder(entity).set(propertyName, newEmail).build();
          //System.out.printf("Entity with : %s%n", entityObj);
          datastore.update(entityObj);
          System.out.println("Entity updated.");
        }else {
          System.out.println(">>>>> skipped with email "+email);
        }
        
      }else {
        throw new Exception("Invalid type of property");
      }
      
    }
  
  
  }

  
  
  public Iterator<Entity> listEntities(String kind, int offset, int limit) {
    Query<Entity> query =
        Query.newEntityQueryBuilder()
        .setKind(kind)
        .setOffset(offset)
        .setLimit(limit)
        //.setOrderBy(OrderBy.asc("created"))
        .build();
    return datastore.run(query);
  }
  

}

enum DatatStoreType {
  STRING, ENTITY, ARRAY
}

