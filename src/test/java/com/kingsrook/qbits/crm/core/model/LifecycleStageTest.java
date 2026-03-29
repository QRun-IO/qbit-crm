/*******************************************************************************
 ** Unit test for LifecycleStage entity
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for LifecycleStage entity -- verifies field definitions, table
 ** metadata production, PVS generation, and insert/query round-trip.
 *******************************************************************************/
class LifecycleStageTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(LifecycleStage.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("sortOrder");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("trending_up");
   }



   /*******************************************************************************
    ** Verify the PVS is produced for this table.
    *******************************************************************************/
   @Test
   void testPossibleValueSourceProduced()
   {
      assertThat(QContext.getQInstance().getPossibleValueSource(LifecycleStage.TABLE_NAME)).isNotNull();
   }



   /*******************************************************************************
    ** Verify insert and query round-trip.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(LifecycleStage.TABLE_NAME);
      insertInput.setRecords(List.of(new LifecycleStage()
         .withName("Lead")
         .withSortOrder(1)
         .withIsActive(true)
         .withDescription("Initial lead stage")
         .toQRecord()));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertThat(insertOutput.getRecords()).hasSize(1);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(LifecycleStage.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(1);

      LifecycleStage fetched = new LifecycleStage(queryOutput.getRecords().get(0));
      assertThat(fetched.getName()).isEqualTo("Lead");
      assertThat(fetched.getSortOrder()).isEqualTo(1);
      assertThat(fetched.getIsActive()).isTrue();
      assertThat(fetched.getDescription()).isEqualTo("Initial lead stage");
   }

}
