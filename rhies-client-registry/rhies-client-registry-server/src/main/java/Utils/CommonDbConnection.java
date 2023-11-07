package Utils;

import com.mongodb.*;

import java.net.UnknownHostException;

public class CommonDbConnection {
   static String URL;
   static String PORT;
   static String DBNAME;
   private static DB database;
   private static MongoClient mongoClient;

   public static synchronized DB dbConnection() throws UnknownHostException {
      getProperty();
      if (database == null) {
         synchronized (CommonDbConnection.class) {
            MongoClientURI uri = new MongoClientURI(URL);
            mongoClient = new MongoClient(uri);
            database = mongoClient.getDB(DBNAME);
         }
      }
      return database;
   }

   private static void getProperty() {
      URL = System.getenv("dbUrl");
      PORT = System.getenv("dbPort");
      DBNAME = System.getenv("dbName");
   }
}
