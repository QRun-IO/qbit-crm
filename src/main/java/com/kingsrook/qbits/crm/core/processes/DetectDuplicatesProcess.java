/*******************************************************************************
 ** Process to detect duplicate contacts (by email) or companies (by domain).
 ** Returns groups where count > 1.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** MetaDataProducer + BackendStep for detecting duplicate contacts or companies.
 *******************************************************************************/
public class DetectDuplicatesProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "detectDuplicates";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Detect Duplicates")
         .withIcon(new QIcon().withName("find_replace"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(DetectDuplicatesProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("entityType", QFieldType.STRING).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the detect duplicates process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String entityType = input.getValueString("entityType");

      if("CONTACT".equalsIgnoreCase(entityType))
      {
         detectContactDuplicates(output);
      }
      else if("COMPANY".equalsIgnoreCase(entityType))
      {
         detectCompanyDuplicates(output);
      }
      else
      {
         throw new QException("Unknown entity type: " + entityType + ". Must be CONTACT or COMPANY.");
      }
   }



   /***************************************************************************
    ** Detect contact duplicates by email.
    ***************************************************************************/
   private void detectContactDuplicates(RunBackendStepOutput output) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Contact.TABLE_NAME));

      Map<String, List<Integer>> emailToIds = new HashMap<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String email = record.getValueString("email");
         if(StringUtils.hasContent(email))
         {
            emailToIds.computeIfAbsent(email.toLowerCase(), k -> new ArrayList<>())
               .add(record.getValueInteger("id"));
         }
      }

      List<QRecord> duplicateGroups = new ArrayList<>();
      for(Map.Entry<String, List<Integer>> entry : emailToIds.entrySet())
      {
         if(entry.getValue().size() > 1)
         {
            duplicateGroups.add(new QRecord()
               .withValue("key", entry.getKey())
               .withValue("ids", entry.getValue().toString())
               .withValue("count", entry.getValue().size()));
         }
      }

      output.setRecords(duplicateGroups);
      output.addValue("duplicateGroupCount", duplicateGroups.size());
   }



   /***************************************************************************
    ** Detect company duplicates by domain.
    ***************************************************************************/
   private void detectCompanyDuplicates(RunBackendStepOutput output) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Company.TABLE_NAME));

      Map<String, List<Integer>> domainToIds = new HashMap<>();
      for(QRecord record : queryOutput.getRecords())
      {
         String domain = record.getValueString("domain");
         if(StringUtils.hasContent(domain))
         {
            domainToIds.computeIfAbsent(domain.toLowerCase(), k -> new ArrayList<>())
               .add(record.getValueInteger("id"));
         }
      }

      List<QRecord> duplicateGroups = new ArrayList<>();
      for(Map.Entry<String, List<Integer>> entry : domainToIds.entrySet())
      {
         if(entry.getValue().size() > 1)
         {
            duplicateGroups.add(new QRecord()
               .withValue("key", entry.getKey())
               .withValue("ids", entry.getValue().toString())
               .withValue("count", entry.getValue().size()));
         }
      }

      output.setRecords(duplicateGroups);
      output.addValue("duplicateGroupCount", duplicateGroups.size());
   }

}
