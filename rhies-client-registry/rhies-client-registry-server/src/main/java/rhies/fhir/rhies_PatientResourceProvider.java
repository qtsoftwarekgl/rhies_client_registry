package rhies.fhir;
import java.io.IOException;
import java.net.UnknownHostException;

import com.google.gson.JsonParser;
import com.mongodb.*;
import com.mongodb.util.JSON;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.AuditEvent.AuditEventEntityComponent;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.time.LocalDate;

import Utils.CommonDbConnection;

public class rhies_PatientResourceProvider implements IResourceProvider {

    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    public rhies_PatientResourceProvider() {
        try {
            DBCollection userCollection = CommonDbConnection.dbConnection().getCollection("users");
            BasicDBObject user = new BasicDBObject("username", "rhiesEMR").append("password", "YWRtaW5QYXNzMTIzNA==");
            DBCursor cursor = userCollection.find(user);
            DBObject dbobject = cursor.one();
            if (dbobject == null) {
                userCollection.insert(user);
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }
    /**
     * Implementation of the "read" (Get) method
     *
     * @throws IOException
     * @throws SecurityException
     */
    @Read
    public Patient read(@IdParam IdType theId) throws ResourceNotFoundException, SecurityException, IOException {
        Patient fhirPatient = new Patient();

        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("patients");

        BasicDBObject query = new BasicDBObject("id", theId.getValue().split("/")[1]);
        DBCursor cursor = patientCollection.find(query);
        DBObject dbobject = cursor.one();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction(AuditEvent.AuditEventAction.R);
        auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("vread").setDisplay("search"));
        auditEvent.setRecorded(new Date());
        
        List<DBObject> entities = new ArrayList<DBObject>();
        entities.add(dbobject);

        if (dbobject != null) {
            String patientData = dbobject.toString();
            fhirPatient = parser.parseResource(Patient.class, patientData);

            auditEvent.setOutcomeDesc("Patient search based on id: " + theId);
            rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
        } else {
            auditEvent.setOutcomeDesc("Patient with nida " + theId + " not found!");
            rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
            utils.error("Patient with nida " + theId + " not found!");
        }

        //Here single patient operation try logging the request the

        return fhirPatient;
    }

    /**
     * Implementation of the "delete" (Get) method
     *
     * @throws UnknownHostException
     */
    @Delete
    public MethodOutcome delete(@IdParam IdType theId) throws UnknownHostException, ResourceNotFoundException {
        MethodOutcome method = new MethodOutcome();

        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("patients");
        BasicDBObject query = new BasicDBObject("id", theId.getValue().split("/")[1]);
        patientCollection.remove(query);

        method.setCreated(true);
        return method;
    }

    @Search
    public List<Patient> search(
            @OptionalParam(name = "nida") StringParam nida,
            @OptionalParam(name = "pcid") StringParam pcid,
            @OptionalParam(name = "age") StringParam age,
            @OptionalParam(name = "page") StringParam page,
            @OptionalParam(name = "size") StringParam size,
            @OptionalParam(name = "fromDate") StringParam fromDate,
            @OptionalParam(name = "endDate") StringParam endDate,
            @OptionalParam(name = "facility") StringParam facility,
            @OptionalParam(name = Patient.SP_FAMILY) StringParam FamilyName,
            @OptionalParam(name = Patient.SP_GIVEN) StringParam givenName,
            @OptionalParam(name = Patient.SP_GENDER) StringParam gender,
            @OptionalParam(name = Patient.SP_BIRTHDATE) StringParam birthDate,
            @OptionalParam(name = Patient.SP_ACTIVE) StringParam active,
            @OptionalParam(name = Patient.SP_IDENTIFIER) StringParam identifier
            
    ) throws UnknownHostException {
        BasicDBObject query = new BasicDBObject();

        if (pcid != null) {
            query.append("id", buildSearchPattern(pcid.getValue().toString()));
        }

        if (FamilyName != null) {
            query.append("name.family", buildSearchPattern(FamilyName.getValue().toString()));
        }

        if (givenName != null) {
            query.append("name.given", buildSearchPattern(givenName.getValue().toString()));
        }

        if (gender != null) {
            query.append("gender", gender.getValue().toString());
        }

        if (birthDate != null) {
            query.append("birthDate", buildSearchPattern(birthDate.getValue().toString()));
        }

        if (active != null) {
            query.append("active", buildSearchPattern(active.getValue().toString()));
        }

        if (identifier != null) {
            query.append("identifier.value", buildSearchPattern(identifier.getValue().toString()));
        }

        if (nida != null) {
            query.append("identifier.value", buildSearchPattern(nida.getValue().toString()));
        }

       if (facility != null) {
          query.append("managingOrganization.display", buildSearchPattern(facility.getValue().toString()));
          System.out.println("facility query: "+query);
       }

       if(fromDate != null && endDate != null){
          query.append("extension", new BasicDBObject("$elemMatch", new BasicDBObject()
             .append("valueDate", new BasicDBObject("$gte", fromDate.getValue().toString()).append("$lte", endDate.getValue().toString())))
          );
       }

        if (age != null) {
            LocalDate date = LocalDate.now();
            date = date.minusYears(Integer.parseInt(age.getValue()));
            String from_date = date.minusYears(1).toString();
            String to_date = date.toString();
            query.append("birthDate", BasicDBObjectBuilder.start("$gte", from_date).add("$lte", to_date).get());
        }

        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("patients");
        List<Patient> retVal = new ArrayList<Patient>();
        long recordCount = patientCollection.count(query);
        String[] logicalId = null;

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

        DBCursor cursor = patientCollection.find(query).skip(pageNumber > 0 ? ((pageNumber - 1) * nPerPage) : 0).limit(nPerPage);

        for (Iterator iterator = cursor.iterator(); iterator.hasNext();) {
            Object next = iterator.next();
            String patientData = next.toString();
            Patient patient = parser.parseResource(Patient.class, patientData);
            String versionId = String.valueOf(recordCount);
            logicalId = patient.getId().split("/");
            patient.setId(new IdType("Patient", logicalId[1], versionId));
            retVal.add(patient);
        }

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction(AuditEvent.AuditEventAction.R);
        auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("vread").setDisplay("search"));
        auditEvent.setRecorded(new Date());
        auditEvent.setOutcomeDesc( retVal.size() + " Patient Resource found");
        
        // auditEvent.addEntity( retVal);

        rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);

        return retVal;
    }

    private BasicDBObject buildSearchPattern(String value) {
        return new BasicDBObject("$regex", ".*" + value + ".*").append("$options", "i");
    }

    /**
     * Implementation of the "Create" (Post) method
     *
     * @throws IOException
     */
    @Create
    public MethodOutcome create(@ResourceParam String incomingPatient) throws NullPointerException, IOException {
        MethodOutcome method = new MethodOutcome();
        IParser par = ctx.newJsonParser();
        JsonParser parser = new JsonParser();
        String nida = "";

        //errors handling
        if (incomingPatient == null || incomingPatient.trim().equals("")) {
            utils.error(Constants.ERROR_PATIENT_EMPTY);
            return method;
        }

        Patient patient = new Patient();

        patient = par.parseResource(Patient.class, incomingPatient);
        //PCID existance
        if (patient.getId() == null || patient.getId().equals("")) {
            utils.error(patient, Constants.ERROR_PATIENT_NO_PCID);
            return method;
        }

        //NIDA existance, Notify no nida but save into CR
        // if (patient.getIdentifier() == null || patient.getIdentifier().isEmpty()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_NIDA);
        // } else {
        //     if (!patient.getIdentifier().get(0).getSystem().equals("NIDA")) {
        //         utils.error(patient, Constants.ERROR_PATIENT_NO_NIDA);
        //     } else {
        //         nida = patient.getIdentifier().get(0).getValue();
        //     }
        // }
        //name existance
        // if (patient.getName() == null || patient.getName().isEmpty() || (!patient.getName().get(0).hasFamily() || !patient.getName().get(0).hasGiven())) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_NAME);
        //     return method;
        // }
        //BirthDate existance
        // if (patient.getBirthDate() == null) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_BIRTHDATE);
        //     return method;
        // }
        //Gender existance
        // if (patient.getGender() == null) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_GENDER);
        //     return method;
        // }

        //fatherName existance
        // if (!patient.getContact().get(0).getExtensionByUrl("fatherName").hasValue()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_FATHERNAME);
        //     return method;
        // }

        //motherName existance
        // if (!patient.getContact().get(0).getExtensionByUrl("motherName").hasValue()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_MOTHERNAME);
        //     return method;
        // }

        //address existance
        // if (patient.getAddress() == null || patient.getAddress().isEmpty()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_ADDRESS);
        //     return method;
        // }

        //country existance
        // if (patient.getAddress().get(0).getCountry() == null || patient.getAddress().get(0).getCountry().equals("")) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_COUNTRY);
        //     return method;
        // }

        //Province/State existance
        // if (patient.getAddress().get(0).getState() == null || patient.getAddress().get(0).getState().equals("")) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_PROVINCE);
        //     return method;
        // }

        //district existance
        // if (patient.getAddress().get(0).getDistrict() == null || patient.getAddress().get(0).getDistrict().equals("")) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_DISTRICT);
        //     return method;
        // }

        //city/sector existance
        // if (!patient.getAddress().get(0).getExtensionByUrl("sector").hasValue()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_SECTOR);
        //     return method;
        // }

        //cell existance
        // if (!patient.getAddress().get(0).getExtensionByUrl("cell").hasValue()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_CELL);
        //     return method;
        // }

        //umudugudu existance
        // if (!patient.getAddress().get(0).getExtensionByUrl("umudugudu").hasValue()) {
        //     utils.error(patient, Constants.ERROR_PATIENT_NO_UMUDUGUDU);
        //     return method;
        // }

        //Initiaze the Auditing Log Information
        AuditEvent auditEvent = new AuditEvent();
        // auditEvent.setAction(AuditEvent.AuditEventAction.R);
        //auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("create").setDisplay("create"));
        auditEvent.setRecorded(new Date());
        // auditEvent.setOutcomeDesc( retVal.size() + " Patient Resource found");

        //Everything is ok,  we can save
        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("patients");
        BasicDBObject query = null;

        // NIDA as first criteria, PCID comes next. But PCID is mandatory. so PCID is main id
        if (nida != null && !nida.trim().equals("")) { //nida exists
            BasicDBObject value = new BasicDBObject("system", "NIDA");
            value.put("value", nida);
            query = new BasicDBObject("identifier", value);
            DBCursor cursor = patientCollection.find(query);

            DBObject dbobject = cursor.one();
            String encoded = par.encodeResourceToString(patient);
            DBObject doc = (DBObject) JSON.parse(encoded);

            String[] entityValues = getValuesEntities(doc);
            String id = entityValues[0];
            String refer = entityValues[1];
            String refer_type = entityValues[2];
            if((id != null && !id.isEmpty()) || (refer != null && !refer.isEmpty()) || (refer_type != null && !refer_type.isEmpty())){
               AuditEvent.AuditEventEntityComponent event = getAuditEventEntityComponent(id, refer, refer_type);
               auditEvent.addEntity(event);
            }

            String[] values = getValues(doc);
            if(values != null && values.length >= 2){
               String disp = values[0];
               String ids = values[1];
               String role_type = values[2];
               if((disp != null && !disp.isEmpty()) || (ids != null && !ids.isEmpty()) || (role_type != null && !role_type.isEmpty())){
                  AuditEvent.AuditEventAgentComponent event = getAuditEventAgentComponent(ids, disp, role_type);
                  auditEvent.addAgent(event);
               }
            }

            if (dbobject != null) {
                patientCollection.update(query, doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.U);
                auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
                auditEvent.setOutcomeDesc("Client Records Updated: NID: " + value);
            } else {
                //NIDA not exists, check if not exists with PCID
                query = new BasicDBObject("id", patient.getId().split("/")[1]);
                cursor = patientCollection.find(query);

                dbobject = cursor.one();
                encoded = par.encodeResourceToString(patient);
                doc = (DBObject) JSON.parse(encoded);

                if (dbobject != null) {
                    patientCollection.update(query, doc);
                    auditEvent.setAction(AuditEvent.AuditEventAction.U);
                    auditEvent.setOutcomeDesc("Client Records Updated: id: " + patient.getId().split("/")[1]);
                    auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
                } else {
                    patientCollection.insert(doc);
                    auditEvent.setAction(AuditEvent.AuditEventAction.C);
                    auditEvent.setOutcomeDesc("Client Records Created: id: " + patient.getId().split("/")[1]);
                    auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("create").setDisplay("create"));
                }
            }
        } else {
            query = new BasicDBObject("id", patient.getId().split("/")[1]);
            DBCursor cursor = patientCollection.find(query);

            DBObject dbobject = cursor.one();
            String encoded = par.encodeResourceToString(patient);
            DBObject doc = (DBObject) JSON.parse(encoded);

            String[] entityValues = getValuesEntities(doc);
            String id = entityValues[0];
            String refer = entityValues[1];
            String refer_type = entityValues[2];
            if((id != null && !id.isEmpty()) || (refer != null && !refer.isEmpty()) || (refer_type != null && !refer_type.isEmpty())){
               AuditEvent.AuditEventEntityComponent event = getAuditEventEntityComponent(id, refer, refer_type);
               auditEvent.addEntity(event);
           }

           String[] values = getValues(doc);
           if(values != null && values.length >= 2) {
              String disp = values[0];
              String ids = values[1];
              String role_type = values[2];
              if ((disp != null && !disp.isEmpty()) || (ids != null && !ids.isEmpty()) || (role_type != null && !role_type.isEmpty())) {
                 AuditEvent.AuditEventAgentComponent event = getAuditEventAgentComponent(ids, disp, role_type);
                 auditEvent.addAgent(event);
              }
           }

            if (dbobject != null) {
                patientCollection.update(query, doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.U);
                auditEvent.setOutcomeDesc("Client Records Updated: id: " + patient.getId().split("/")[1]);
                auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
            } else {
                patientCollection.insert(doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.C);
                auditEvent.setOutcomeDesc("Client Records Created: id: " + patient.getId().split("/")[1]);
                 auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("create").setDisplay("create"));
            }
        }

        method.setCreated(true);

        // Here add the required information 
        rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
        return method;
    }

   private static String[] getValuesEntities(DBObject doc){
      String[] values = new String[3];
      String id = (String) doc.get("id");
      String type = (String) doc.get("resourceType");
      String reference = type+'/'+id;
      values[0] = id;
      values[1] = reference;
      values[2] = type;
      return values;
   }

   private static AuditEvent.AuditEventEntityComponent getAuditEventEntityComponent(String id, String refer, String refer_type) {
      AuditEvent.AuditEventEntityComponent event = new AuditEvent.AuditEventEntityComponent();
      Reference what = new Reference();
      what.setReference(refer);
      Identifier identifier = new Identifier();
      identifier.setValue(id);
      what.setIdentifier(identifier);

      Coding detail_type= new Coding();
      detail_type.setSystem("http://dicom.nema.org/resources/ontology/DCM");
      detail_type.setCode("110110");
      detail_type.setDisplay("Patient Record");


      Coding theCoding= new Coding();
      theCoding.setSystem("http://terminology.hl7.org/CodeSystem/object-role");
      theCoding.setCode("4");
      theCoding.setDisplay("Domain Resource");

      event.setType(detail_type);
      event.setWhat(what);
      event.setRole(theCoding);
      event.setName(refer_type);
      return event;
   }

   private static String[] getValues(DBObject doc){
      String[] values = new String[3];
      BasicDBList performer = (BasicDBList) doc.get("generalPractitioner");
      String id = "" ;
      String display= "";
      String role_type= "";
      if(performer != null){
         for (Object scenario : performer) {
            BasicDBObject identifier = (BasicDBObject) ((BasicDBObject) scenario).get("identifier");
            if (identifier != null) {
               id = (String) identifier.get("value");
            }
            display = (String) ((BasicDBObject) scenario).get("display");
            role_type = (String) ((BasicDBObject) scenario).get("type");
         }
         values[0] = display;
         values[1] = id;
         values[2] = role_type;
         return values;
      }
      return  null;
   }
   private static AuditEvent.AuditEventAgentComponent getAuditEventAgentComponent(String id, String display, String role_type) {
      AuditEvent.AuditEventAgentComponent event = new AuditEvent.AuditEventAgentComponent();
      Identifier identifier = new Identifier();
      identifier.setValue(id);
      Reference who = new Reference();
      who.setDisplay(display);
      who.setIdentifier(identifier);

      Coding theCoding= new Coding();
      theCoding.setSystem("http://terminology.hl7.org/CodeSystem/extra-security-role-type");
      theCoding.setCode("humanuser");
      theCoding.setDisplay("human user");
      CodeableConcept codeableConcept = new CodeableConcept();
      List <Coding> codeList = new ArrayList<Coding>();
      codeList.add(theCoding);
      codeableConcept.setCoding(codeList);

      CodeableConcept codeableConcept2 = new CodeableConcept();
      codeableConcept2.setText(role_type);
      List <CodeableConcept> roleList = new ArrayList<CodeableConcept>();
      roleList.add(codeableConcept2);

      event.setRole(roleList);
      event.setType(codeableConcept);
      event.setWho(who);
      event.setRequestor(true);
      return event;
   }
}
