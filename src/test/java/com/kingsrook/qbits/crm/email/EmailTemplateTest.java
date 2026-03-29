/*******************************************************************************
 ** Unit tests for the EmailTemplate entity.
 *******************************************************************************/
package com.kingsrook.qbits.crm.email;


import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.email.model.EmailTemplate;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for EmailTemplate CRUD, field persistence, PVS registration, and
 ** section layout.
 *******************************************************************************/
class EmailTemplateTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has a PVS.
    *******************************************************************************/
   @Test
   void testTableAndPvsRegistered()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailTemplate.TABLE_NAME);
      assertNotNull(table);
      assertThat(table.getIcon()).isNotNull();
      assertThat(table.getIcon().getName()).isEqualTo("description");

      /////////////////////////////////////////////////////////////
      // verify PVS was produced for table-backed possible value //
      /////////////////////////////////////////////////////////////
      assertNotNull(QContext.getQInstance().getPossibleValueSource(EmailTemplate.TABLE_NAME));
   }



   /*******************************************************************************
    ** Verify all fields are present in the table metadata.
    *******************************************************************************/
   @Test
   void testFieldsPresent()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailTemplate.TABLE_NAME);

      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("subject");
      assertThat(table.getFields()).containsKey("bodyHtml");
      assertThat(table.getFields()).containsKey("bodyText");
      assertThat(table.getFields()).containsKey("category");
      assertThat(table.getFields()).containsKey("ownerUserId");
      assertThat(table.getFields()).containsKey("isShared");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
   }



   /*******************************************************************************
    ** Insert a template and verify round-trip query.
    *******************************************************************************/
   @Test
   void testInsertAndQuery() throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(
         new InsertInput(EmailTemplate.TABLE_NAME)
            .withRecordEntity(new EmailTemplate()
               .withName("Welcome Email")
               .withSubject("Welcome to {{company.name}}")
               .withBodyHtml("<h1>Hello {{contact.firstName}}!</h1>")
               .withBodyText("Hello {{contact.firstName}}!")
               .withCategory("Onboarding")
               .withOwnerUserId("user-001")
               .withIsShared(true)));

      assertNotNull(insertOutput.getRecords().get(0).getValueInteger("id"));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailTemplate.TABLE_NAME));

      assertEquals(1, queryOutput.getRecords().size());

      EmailTemplate fetched = new EmailTemplate(queryOutput.getRecords().get(0));
      assertNotNull(fetched.getId());
      assertEquals("Welcome Email", fetched.getName());
      assertEquals("Welcome to {{company.name}}", fetched.getSubject());
      assertEquals("<h1>Hello {{contact.firstName}}!</h1>", fetched.getBodyHtml());
      assertEquals("Hello {{contact.firstName}}!", fetched.getBodyText());
      assertEquals("Onboarding", fetched.getCategory());
      assertEquals("user-001", fetched.getOwnerUserId());
      assertThat(fetched.getIsShared()).isTrue();
   }



   /*******************************************************************************
    ** Insert a minimal template (required fields only) with defaults.
    *******************************************************************************/
   @Test
   void testInsertMinimalWithDefaults() throws QException
   {
      new InsertAction().execute(
         new InsertInput(EmailTemplate.TABLE_NAME)
            .withRecordEntity(new EmailTemplate()
               .withName("Quick Template")
               .withSubject("Quick Subject")
               .withBodyHtml("<p>Content</p>")
               .withOwnerUserId("user-002")
               .withIsShared(false)));

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(EmailTemplate.TABLE_NAME));

      EmailTemplate fetched = new EmailTemplate(queryOutput.getRecords().get(0));
      assertFalse(fetched.getIsShared());
   }

}
