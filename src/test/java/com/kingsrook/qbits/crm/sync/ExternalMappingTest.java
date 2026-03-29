/*******************************************************************************
 ** Unit tests for the ExternalMapping entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.sync;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmEntityType;
import com.kingsrook.qbits.crm.core.model.enums.CrmSyncStatus;
import com.kingsrook.qbits.crm.sync.model.ExternalMapping;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for ExternalMapping CRUD, unique key enforcement, and enum PVS
 ** references for entityType and syncStatus.
 *******************************************************************************/
class ExternalMappingTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered with the correct icon.
    *******************************************************************************/
   @Test
   void testTableRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(ExternalMapping.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("sync");
   }



   /*******************************************************************************
    ** Insert an external mapping and verify round-trip.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      Instant now = Instant.now();

      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(42)
               .withExternalSystem("google_calendar")
               .withExternalId("gcal-event-abc-123")
               .withLastSyncDate(now)
               .withSyncStatus(CrmSyncStatus.SYNCED.getId())));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ExternalMapping.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      ExternalMapping fetched = new ExternalMapping(queryOutput.getRecords().get(0));
      assertNotNull(fetched.getId());
      assertEquals(CrmEntityType.CONTACT.getId(), fetched.getEntityType());
      assertEquals(42, fetched.getEntityId());
      assertEquals("google_calendar", fetched.getExternalSystem());
      assertEquals("gcal-event-abc-123", fetched.getExternalId());
      assertNotNull(fetched.getLastSyncDate());
      assertEquals(CrmSyncStatus.SYNCED.getId(), fetched.getSyncStatus());
   }



   /*******************************************************************************
    ** Verify unique key (entityType, entityId, externalSystem) rejects duplicates.
    *******************************************************************************/
   @Test
   void testUniqueKeyEnforcement() throws QException
   {
      ///////////////////////////////////////////
      // insert first mapping                  //
      ///////////////////////////////////////////
      new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.DEAL.getId())
               .withEntityId(100)
               .withExternalSystem("hubspot")
               .withExternalId("hs-deal-999")
               .withSyncStatus(CrmSyncStatus.SYNCED.getId())));

      ///////////////////////////////////////////////////////////
      // insert duplicate -- same (entityType, entityId,       //
      // externalSystem) should fail with unique key violation  //
      ///////////////////////////////////////////////////////////
      InsertOutput dupeOutput = new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.DEAL.getId())
               .withEntityId(100)
               .withExternalSystem("hubspot")
               .withExternalId("hs-deal-different")
               .withSyncStatus(CrmSyncStatus.PENDING.getId())));

      QRecord dupeRecord = dupeOutput.getRecords().get(0);
      assertThat(dupeRecord.getErrors()).isNotEmpty();
   }



   /*******************************************************************************
    ** Verify same entity can be mapped to different external systems.
    *******************************************************************************/
   @Test
   void testSameEntityDifferentSystems() throws QException
   {
      new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(1)
               .withExternalSystem("google_calendar")
               .withExternalId("gcal-1")
               .withSyncStatus(CrmSyncStatus.SYNCED.getId())));

      new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.CONTACT.getId())
               .withEntityId(1)
               .withExternalSystem("outlook")
               .withExternalId("outlook-1")
               .withSyncStatus(CrmSyncStatus.PENDING.getId())));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ExternalMapping.TABLE_NAME));

      assertEquals(2, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    ** Verify error status mapping.
    *******************************************************************************/
   @Test
   void testErrorSyncStatus() throws QException
   {
      new InsertAction().execute(
         new InsertInput(ExternalMapping.TABLE_NAME)
            .withRecordEntity(new ExternalMapping()
               .withEntityType(CrmEntityType.ACTIVITY.getId())
               .withEntityId(77)
               .withExternalSystem("salesforce")
               .withExternalId("sf-activity-77")
               .withSyncStatus(CrmSyncStatus.ERROR.getId())));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(ExternalMapping.TABLE_NAME));

      ExternalMapping fetched = new ExternalMapping(queryOutput.getRecords().get(0));
      assertEquals(CrmSyncStatus.ERROR.getId(), fetched.getSyncStatus());
   }

}
