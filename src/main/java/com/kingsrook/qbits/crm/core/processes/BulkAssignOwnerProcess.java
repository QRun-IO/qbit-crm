/*******************************************************************************
 ** Bulk action process to assign a new owner to selected records. Works on
 ** any table that has an ownerUserId field.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** MetaDataProducer for the BulkAssignOwner table-level process. Uses the
 ** StreamedETL pattern with a transform step that sets ownerUserId on each
 ** record to the newOwnerUserId input value.
 *******************************************************************************/
public class BulkAssignOwnerProcess implements MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "bulkAssignOwner";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData process = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(NAME)
         .withLabel("Bulk Assign Owner")
         .withTableName(Contact.TABLE_NAME)
         .withIcon(new QIcon().withName("person_pin"))
         .withTransformStepClass(BulkAssignOwnerTransformStep.class)
         .withPreviewMessage("Records to be assigned a new owner")
         .withReviewStepRecordFields(List.of(
            new QFieldMetaData("id", QFieldType.INTEGER),
            new QFieldMetaData("ownerUserId", QFieldType.STRING).withLabel("Current Owner")
         ))
         .getProcessMetaData();

      return (process);
   }



   /*******************************************************************************
    ** Transform step that sets ownerUserId on each record.
    *******************************************************************************/
   public static class BulkAssignOwnerTransformStep extends AbstractTransformStep
   {
      private ProcessSummaryLine okLine = new ProcessSummaryLine(Status.OK)
         .withSingularFutureMessage("will be assigned a new owner")
         .withPluralFutureMessage("will be assigned a new owner")
         .withSingularPastMessage("was assigned a new owner")
         .withPluralPastMessage("were assigned a new owner");



      /*******************************************************************************
       ** Set ownerUserId on each record from the newOwnerUserId input.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String newOwnerUserId = input.getValueString("newOwnerUserId");

         for(QRecord record : input.getRecords())
         {
            record.setValue("ownerUserId", newOwnerUserId);
            output.addRecord(record);
            okLine.incrementCountAndAddPrimaryKey(record.getValueInteger("id"));
         }
      }



      /*******************************************************************************
       ** Provide process summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();
         okLine.addSelfToListIfAnyCount(rs);
         return (rs);
      }
   }

}
