/*******************************************************************************
 ** Process to merge two companies. Re-links contacts, deals, activities, and
 ** tags from the secondary to the primary, then deletes the secondary company
 ** and logs audit entries.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.processes;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.crm.activities.model.Activity;
import com.kingsrook.qbits.crm.audit.model.AuditLog;
import com.kingsrook.qbits.crm.core.model.Company;
import com.kingsrook.qbits.crm.core.model.CompanyTag;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.enums.CrmAuditAction;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.deals.model.Deal;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
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


/*******************************************************************************
 ** MetaDataProducer + BackendStep for merging two companies into one.
 *******************************************************************************/
public class MergeCompaniesProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "mergeCompanies";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Merge Companies")
         .withTableName(Company.TABLE_NAME)
         .withIcon(new QIcon().withName("merge"))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(MergeCompaniesProcess.class))
               .withInputData(new QFunctionInputMetaData().withFieldList(List.of(
                  new QFieldMetaData("primaryCompanyId", QFieldType.INTEGER).withIsRequired(true),
                  new QFieldMetaData("secondaryCompanyId", QFieldType.INTEGER).withIsRequired(true)
               )))
         ));
   }



   /*******************************************************************************
    ** Execute the merge companies process.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      Integer primaryCompanyId   = input.getValueInteger("primaryCompanyId");
      Integer secondaryCompanyId = input.getValueInteger("secondaryCompanyId");
      String  sessionUserId      = CrmSessionUtils.getCurrentUserId();

      /////////////////////////////////////////
      // validate both companies exist       //
      /////////////////////////////////////////
      if(GetAction.execute(Company.TABLE_NAME, primaryCompanyId) == null)
      {
         throw new QException("Primary company not found: " + primaryCompanyId);
      }
      if(GetAction.execute(Company.TABLE_NAME, secondaryCompanyId) == null)
      {
         throw new QException("Secondary company not found: " + secondaryCompanyId);
      }

      ///////////////////////////////////////////////
      // Re-link contacts from secondary           //
      ///////////////////////////////////////////////
      relinkRecords(Contact.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);

      ///////////////////////////////////////////////
      // Re-link deals from secondary              //
      ///////////////////////////////////////////////
      relinkRecords(Deal.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);

      ///////////////////////////////////////////////
      // Re-link activities from secondary         //
      ///////////////////////////////////////////////
      relinkRecords(Activity.TABLE_NAME, "companyId", secondaryCompanyId, primaryCompanyId);

      ///////////////////////////////////////////////
      // Re-link CompanyTag rows (skip duplicates) //
      ///////////////////////////////////////////////
      relinkCompanyTags(primaryCompanyId, secondaryCompanyId);

      ///////////////////////////////////////////////
      // Delete the secondary company              //
      ///////////////////////////////////////////////
      new DeleteAction().execute(
         new DeleteInput(Company.TABLE_NAME).withPrimaryKeys(List.of(secondaryCompanyId)));

      ///////////////////////////////////////////////
      // Log audit entries                         //
      ///////////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
            new AuditLog()
               .withEntityType(CrmEntityType.COMPANY.getPossibleValueId())
               .withEntityId(primaryCompanyId)
               .withAction(CrmAuditAction.MERGED.getPossibleValueId())
               .withUserId(sessionUserId)
               .withMessage("Merged company " + secondaryCompanyId + " into this company")));

      new InsertAction().execute(
         new InsertInput(AuditLog.TABLE_NAME).withRecordEntity(
            new AuditLog()
               .withEntityType(CrmEntityType.COMPANY.getPossibleValueId())
               .withEntityId(secondaryCompanyId)
               .withAction(CrmAuditAction.DELETED.getPossibleValueId())
               .withUserId(sessionUserId)
               .withMessage("Company deleted during merge into company " + primaryCompanyId)));

      output.addValue("primaryCompanyId", primaryCompanyId);
   }



   /***************************************************************************
    ** Re-link simple FK records from secondary to primary.
    ***************************************************************************/
   private void relinkRecords(String tableName, String fkField, Integer fromId, Integer toId) throws QException
   {
      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(tableName)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria(fkField, QCriteriaOperator.EQUALS, fromId))));

      for(QRecord record : queryOutput.getRecords())
      {
         QRecord updateRecord = new QRecord()
            .withValue("id", record.getValueInteger("id"))
            .withValue(fkField, toId);

         new UpdateAction().execute(
            new UpdateInput(tableName).withRecord(updateRecord));
      }
   }



   /***************************************************************************
    ** Re-link CompanyTag rows: skip duplicates (same tagId already on primary).
    ***************************************************************************/
   private void relinkCompanyTags(Integer primaryCompanyId, Integer secondaryCompanyId) throws QException
   {
      QueryOutput primaryTags = new QueryAction().execute(
         new QueryInput(CompanyTag.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("companyId", QCriteriaOperator.EQUALS, primaryCompanyId))));

      Set<Integer> primaryTagIds = new HashSet<>();
      for(QRecord record : primaryTags.getRecords())
      {
         primaryTagIds.add(record.getValueInteger("tagId"));
      }

      QueryOutput secondaryTags = new QueryAction().execute(
         new QueryInput(CompanyTag.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("companyId", QCriteriaOperator.EQUALS, secondaryCompanyId))));

      for(QRecord record : secondaryTags.getRecords())
      {
         Integer tagId = record.getValueInteger("tagId");
         if(primaryTagIds.contains(tagId))
         {
            new DeleteAction().execute(
               new DeleteInput(CompanyTag.TABLE_NAME).withPrimaryKeys(List.of(record.getValueInteger("id"))));
         }
         else
         {
            QRecord updateRecord = new QRecord()
               .withValue("id", record.getValueInteger("id"))
               .withValue("companyId", primaryCompanyId);
            new UpdateAction().execute(
               new UpdateInput(CompanyTag.TABLE_NAME).withRecord(updateRecord));
         }
      }
   }

}
