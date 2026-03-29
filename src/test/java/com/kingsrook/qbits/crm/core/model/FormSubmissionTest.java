/*******************************************************************************
 ** Unit test for FormSubmission entity
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormConversionStatus;
import com.kingsrook.qbits.crm.core.model.enums.CrmFormType;
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
 ** Unit test for FormSubmission entity -- verifies field definitions, UTM
 ** fields, conversion status, and insert/query round-trip.
 *******************************************************************************/
class FormSubmissionTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(FormSubmission.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("formType");
      assertThat(table.getFields()).containsKey("email");
      assertThat(table.getFields()).containsKey("firstName");
      assertThat(table.getFields()).containsKey("lastName");
      assertThat(table.getFields()).containsKey("company");
      assertThat(table.getFields()).containsKey("phone");
      assertThat(table.getFields()).containsKey("message");

      ///////////////////////////
      // UTM fields            //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("sourceUrl");
      assertThat(table.getFields()).containsKey("utmSource");
      assertThat(table.getFields()).containsKey("utmMedium");
      assertThat(table.getFields()).containsKey("utmCampaign");
      assertThat(table.getFields()).containsKey("utmTerm");
      assertThat(table.getFields()).containsKey("utmContent");
      assertThat(table.getFields()).containsKey("referrer");

      ///////////////////////////
      // conversion fields     //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("contactId");
      assertThat(table.getFields()).containsKey("conversionStatus");
      assertThat(table.getFields()).containsKey("convertedDate");
      assertThat(table.getFields()).containsKey("convertedByUserId");

      ///////////////////////////
      // technical fields      //
      ///////////////////////////
      assertThat(table.getFields()).containsKey("ipAddress");
      assertThat(table.getFields()).containsKey("userAgent");
      assertThat(table.getFields()).containsKey("metadata");

      assertThat(table.getIcon().getName()).isEqualTo("dynamic_form");
   }



   /*******************************************************************************
    ** Verify insert and query round-trip with UTM data.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws Exception
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(FormSubmission.TABLE_NAME);
      insertInput.setRecords(List.of(new FormSubmission()
         .withFormType(CrmFormType.DEMO_REQUEST.getId())
         .withEmail("test@example.com")
         .withFirstName("Jane")
         .withLastName("Smith")
         .withCompany("Widgets Inc")
         .withUtmSource("google")
         .withUtmMedium("cpc")
         .withUtmCampaign("spring2026")
         .withConversionStatus(CrmFormConversionStatus.NEW.getId())
         .withIpAddress("192.168.1.1")
         .toQRecord()));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertThat(insertOutput.getRecords()).hasSize(1);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(FormSubmission.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(1);

      FormSubmission fetched = new FormSubmission(queryOutput.getRecords().get(0));
      assertThat(fetched.getId()).isNotNull();
      assertThat(fetched.getEmail()).isEqualTo("test@example.com");
      assertThat(fetched.getFirstName()).isEqualTo("Jane");
      assertThat(fetched.getCompany()).isEqualTo("Widgets Inc");
      assertThat(fetched.getUtmSource()).isEqualTo("google");
      assertThat(fetched.getUtmMedium()).isEqualTo("cpc");
      assertThat(fetched.getUtmCampaign()).isEqualTo("spring2026");
      assertThat(fetched.getIpAddress()).isEqualTo("192.168.1.1");
   }

}
