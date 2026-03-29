/*******************************************************************************
 ** Unit test for Contact entity
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
 ** Unit test for Contact entity -- verifies field definitions, section layout,
 ** security lock injection point, and insert/query round-trip.
 *******************************************************************************/
class ContactTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has all expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Contact.TABLE_NAME);
      assertThat(table).isNotNull();

      ///////////////////////////
      // identity fields       //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("firstName");
      assertThat(table.getFields()).containsKey("lastName");
      assertThat(table.getFields()).containsKey("email");
      assertThat(table.getFields()).containsKey("phone");

      ///////////////////////////
      // detail fields         //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("companyId");
      assertThat(table.getFields()).containsKey("jobTitle");
      assertThat(table.getFields()).containsKey("department");
      assertThat(table.getFields()).containsKey("lifecycleStageId");
      assertThat(table.getFields()).containsKey("leadSourceId");
      assertThat(table.getFields()).containsKey("ownerUserId");
      assertThat(table.getFields()).containsKey("leadScore");

      ///////////////////////////
      // address fields        //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("addressLine1");
      assertThat(table.getFields()).containsKey("city");
      assertThat(table.getFields()).containsKey("state");
      assertThat(table.getFields()).containsKey("postalCode");
      assertThat(table.getFields()).containsKey("country");

      ///////////////////////////
      // communication fields  //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("doNotEmail");
      assertThat(table.getFields()).containsKey("doNotCall");
      assertThat(table.getFields()).containsKey("unsubscribeDate");

      ///////////////////////////
      // online fields         //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("secondaryEmail");
      assertThat(table.getFields()).containsKey("mobilePhone");
      assertThat(table.getFields()).containsKey("website");
      assertThat(table.getFields()).containsKey("linkedinUrl");

      ///////////////////////////
      // date fields           //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
      assertThat(table.getFields()).containsKey("lastActivityDate");
      assertThat(table.getFields()).containsKey("lastContactedDate");

      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("person");
   }



   /*******************************************************************************
    ** Verify the table has sections defined.
    *******************************************************************************/
   @Test
   void testSections()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Contact.TABLE_NAME);
      assertThat(table.getSections()).isNotNull();
      assertThat(table.getSections().size()).isGreaterThanOrEqualTo(6);

      ///////////////////////////////////
      // verify T1 identity section    //
      ///////////////////////////////////
      assertThat(table.getSections().get(0).getFieldNames()).contains("id", "firstName", "lastName", "email", "phone");
   }



   /*******************************************************************************
    ** Verify insert and query round-trip with entity fields.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(Contact.TABLE_NAME);
      insertInput.setRecords(List.of(new Contact()
         .withFirstName("John")
         .withLastName("Doe")
         .withEmail("john.doe@example.com")
         .withPhone("555-0100")
         .withOwnerUserId("user1")
         .withJobTitle("VP Sales")
         .withCity("Denver")
         .withState("CO")
         .withCountry("US")
         .withLeadScore(50)
         .withDoNotEmail(false)
         .withDoNotCall(false)
         .toQRecord()));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertThat(insertOutput.getRecords()).hasSize(1);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(Contact.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(1);

      Contact fetched = new Contact(queryOutput.getRecords().get(0));
      assertThat(fetched.getId()).isNotNull();
      assertThat(fetched.getFirstName()).isEqualTo("John");
      assertThat(fetched.getLastName()).isEqualTo("Doe");
      assertThat(fetched.getEmail()).isEqualTo("john.doe@example.com");
      assertThat(fetched.getPhone()).isEqualTo("555-0100");
      assertThat(fetched.getOwnerUserId()).isEqualTo("user1");
      assertThat(fetched.getJobTitle()).isEqualTo("VP Sales");
      assertThat(fetched.getCity()).isEqualTo("Denver");
      assertThat(fetched.getLeadScore()).isEqualTo(50);
      assertThat(fetched.getDoNotEmail()).isFalse();
   }

}
