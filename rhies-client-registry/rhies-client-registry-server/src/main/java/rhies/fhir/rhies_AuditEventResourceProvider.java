package rhies.fhir;

import Utils.CommonDbConnection;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.mongodb.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.IdType;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.time.Instant;

public class rhies_AuditEventResourceProvider implements IResourceProvider {

   FhirContext fhirContext = FhirContext.forR4();
   IParser iParser = fhirContext.newJsonParser();
   @Override
   public Class<? extends IBaseResource> getResourceType() {
      return AuditEvent.class;
   }

   public rhies_AuditEventResourceProvider(){
      System.out.println("AuditEventResourceProviderInitialized!");
   }

   @Read
   public AuditEvent read(@IdParam IdType theId) throws ResourceNotFoundException, SecurityException, IOException{
      AuditEvent auditEvent = new AuditEvent();

      DBCollection dbCollection = CommonDbConnection.dbConnection().getCollection("audit_events");

      BasicDBObject query = new BasicDBObject("id", theId.getValue().split("/")[1]);
      DBCursor cursor = dbCollection.find(query);
      DBObject dbObject = cursor.one();

      if(dbObject != null){
         String auditEventData = dbObject.toString();
         auditEvent = iParser.parseResource(AuditEvent.class, auditEventData);
      } else {
         utils.error("Audit Event not Found " + theId);
      }

      return auditEvent;
   }

   @Search
   public List<AuditEvent> search(
      @OptionalParam(name = "fromDate") String fromDate,
      @OptionalParam(name = "toDate") String toDate,
      @OptionalParam(name = "type") String type,
      @OptionalParam(name = "page") StringParam page,
      @OptionalParam(name = "size") StringParam size
   ) throws UnknownHostException{

      List<AuditEvent> auditEvents = new ArrayList<AuditEvent>();

      BasicDBObject query = new BasicDBObject();
      DBCollection auditEventCollection = CommonDbConnection.dbConnection().getCollection("audit_events");
      Integer pageNumber = 1;
      if (page != null) {
          pageNumber = Integer.parseInt(page.getValue());
          if (pageNumber == 0) {
              pageNumber = 1;
          }
      }
      Integer nPerPage = 10;
      if (size != null) {
          nPerPage = Integer.parseInt(size.getValue());
          if (nPerPage == 0) {
              nPerPage = 10;
          }
      }

      String startDate = "";
      String endDate = "";
      if (fromDate != null && !fromDate.isEmpty() ) {
         fromDate = fromDate+"T00:00:00.000Z";
         Instant instant1 = Instant.parse(fromDate);
         startDate = instant1.toString();
      }

      if (toDate != null && !toDate.isEmpty() ) {
         toDate = toDate+"T23:59:59.000Z";
         Instant instant2 = Instant.parse(toDate);
         endDate = instant2.toString();
      }

      if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
         query.append("recorded", BasicDBObjectBuilder.start("$gte", startDate).add("$lte", endDate).get());
      }

      if (startDate != null && !startDate.isEmpty() && (endDate == null || endDate.isEmpty())) {
         query.append("recorded", new BasicDBObject("$gte", startDate));
      }

      if ((startDate == null || startDate.isEmpty()) && endDate != null && !endDate.isEmpty()) {
         query.append("recorded", new BasicDBObject("$lte", endDate));
      }

      if(type!= null){
         String action = "";
         switch(type)
         {
            case "CREATE":
               action = "C";
               break;
            case "UPDATE":
               action = "U";
               break;
            case "DELETE":
               action = "D";
               break;
            case "SEARCH":
               action = "R";
               break;
            case "EXECUTE":
               action = "E";
               break;
         }
         query.append("action", action);
      }

      long recordCount = auditEventCollection.count(query);
      DBCursor cursor = auditEventCollection.find(query).skip(pageNumber > 0 ? ((pageNumber - 1) * nPerPage) : 0).limit(nPerPage);
      Integer logicalId = 0;
      for (Iterator iterator = cursor.iterator(); iterator.hasNext();){
         String auditEventData = iterator.next().toString();
         AuditEvent auditEvent = iParser.parseResource(AuditEvent.class, auditEventData);
         String versionId = String.valueOf(recordCount);
         auditEvent.setId(new IdType("AuditEvent",logicalId.toString(),versionId));
         auditEvents.add(auditEvent);
         logicalId++;
      }
      return auditEvents;
   }

}
