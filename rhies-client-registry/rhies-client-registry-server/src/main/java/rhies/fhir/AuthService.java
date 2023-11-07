package rhies.fhir;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.net.UnknownHostException;
import Utils.CommonDbConnection;
public class AuthService {
   public static boolean authenticate(String username, String password) throws UnknownHostException {
      DBCollection userCollection = CommonDbConnection.dbConnection().getCollection("users");

      BasicDBObject query =  new BasicDBObject("username", username).append("password", password);
      DBCursor cursor = userCollection.find(query);
      DBObject dbobject = cursor.one();

      if (dbobject != null &&  dbobject.get("_id") !=null) {
         return true;
      } else {
         return false;
      }
   }
}
