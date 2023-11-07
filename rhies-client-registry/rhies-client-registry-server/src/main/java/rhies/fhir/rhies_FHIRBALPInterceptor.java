package rhies.fhir;

import Utils.CommonDbConnection;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Period;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.Date;

import Utils.CommonDbConnection;

public class rhies_FHIRBALPInterceptor extends InterceptorAdapter {

   static FhirContext fhirContext = FhirContext.forR4();
   static IParser iParser = fhirContext.newJsonParser();

    @Override
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        AuditEvent auditEvent = new AuditEvent();

        // Populate audit event details
        auditEvent.setAction(AuditEvent.AuditEventAction.E);

        //RequestDetails requestDetails = (RequestDetails) theRequest;

        //auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/audit-event-action").setCode(String.valueOf(theResponse.getStatus()) ).  (theRequest.getQueryString()));
        //auditEvent.setType(new Coding(theRequest.getQueryString(), String.valueOf(theResponse.getStatus()), theRequest.getQueryString()));

        // Populate other relevant audit event fields
       auditEvent.setPeriod(new Period());
       auditEvent.setOutcomeDesc(theRequest.getMethod());
       auditEvent.setRecorded(new Date());

        // Store the audit event in your audit log storage
       /*try {
          storeAuditEvent(auditEvent);
       } catch (UnknownHostException uhe){
          uhe.printStackTrace();
       }*/

        return super.incomingRequestPreProcessed(theRequest, theResponse);
    }


   @Override
   public boolean outgoingResponse(RequestDetails theRequestDetails, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws AuthenticationException {
       AuditEvent auditEvent = new AuditEvent();
       auditEvent.setAction(AuditEvent.AuditEventAction.R);
       auditEvent.setType(new Coding().setDisplay(theRequestDetails.getCompleteUrl()).setCode(String.valueOf(theServletResponse.getStatus()) ).setSystem("http://hl7.org/fhir/audit-event-action") );
       auditEvent.setRecorded(new Date());

       /*try{
          storeAuditEvent(auditEvent);
       } catch (UnknownHostException uhe){
          uhe.printStackTrace();
       }*/
       return super.outgoingResponse(theRequestDetails, theServletRequest, theServletResponse);
   }

   public static void storeAuditEvent(AuditEvent auditEvent) throws UnknownHostException {
        // Implement the logic to store the audit event in your audit log storage
        // This can be a database, a log file, an external service, etc.

       System.out.println("\n=========================================\n" +
          auditEvent.getOutcomeDesc() + "\n" +
          auditEvent.getAction().name() + "\n" +
          auditEvent.getOutcomeDesc() + "\n" +
          "\n=========================================\n");

       DBCollection auditEventCollection = CommonDbConnection.dbConnection().getCollection("audit_events");

       BasicDBObject query = null;
       String auditEventEncoded = iParser.encodeResourceToString(auditEvent);

       DBObject dbObject = (DBObject) JSON.parse(auditEventEncoded);

       auditEventCollection.insert(dbObject);
    }
}
