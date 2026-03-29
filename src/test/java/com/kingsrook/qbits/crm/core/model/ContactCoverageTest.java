/*******************************************************************************
 ** Coverage tests for Contact entity -- exercises every getter/setter/withX.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercises every fluent setter and getter on Contact.
 *******************************************************************************/
class ContactCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** Test all fluent setters and getters.
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      Instant now = Instant.now();

      Contact contact = new Contact()
         .withId(1)
         .withFirstName("John")
         .withLastName("Doe")
         .withEmail("john@example.com")
         .withSecondaryEmail("john2@example.com")
         .withPhone("555-0100")
         .withMobilePhone("555-0101")
         .withJobTitle("VP Sales")
         .withDepartment("Sales")
         .withCompanyId(10)
         .withLifecycleStageId(2)
         .withLeadSourceId(3)
         .withOwnerUserId("user-001")
         .withAddressLine1("123 Main St")
         .withAddressLine2("Suite 100")
         .withCity("Denver")
         .withState("CO")
         .withPostalCode("80202")
         .withCountry("US")
         .withTimezone("America/Denver")
         .withPreferredLanguage("en")
         .withWebsite("https://example.com")
         .withLinkedinUrl("https://linkedin.com/in/johndoe")
         .withDescription("Test contact")
         .withLeadScore(75)
         .withDoNotEmail(true)
         .withDoNotCall(false)
         .withUnsubscribeDate(now)
         .withLastActivityDate(now)
         .withLastContactedDate(now)
         .withCreateDate(now)
         .withModifyDate(now);

      assertThat(contact.getId()).isEqualTo(1);
      assertThat(contact.getFirstName()).isEqualTo("John");
      assertThat(contact.getLastName()).isEqualTo("Doe");
      assertThat(contact.getEmail()).isEqualTo("john@example.com");
      assertThat(contact.getSecondaryEmail()).isEqualTo("john2@example.com");
      assertThat(contact.getPhone()).isEqualTo("555-0100");
      assertThat(contact.getMobilePhone()).isEqualTo("555-0101");
      assertThat(contact.getJobTitle()).isEqualTo("VP Sales");
      assertThat(contact.getDepartment()).isEqualTo("Sales");
      assertThat(contact.getCompanyId()).isEqualTo(10);
      assertThat(contact.getLifecycleStageId()).isEqualTo(2);
      assertThat(contact.getLeadSourceId()).isEqualTo(3);
      assertThat(contact.getOwnerUserId()).isEqualTo("user-001");
      assertThat(contact.getAddressLine1()).isEqualTo("123 Main St");
      assertThat(contact.getAddressLine2()).isEqualTo("Suite 100");
      assertThat(contact.getCity()).isEqualTo("Denver");
      assertThat(contact.getState()).isEqualTo("CO");
      assertThat(contact.getPostalCode()).isEqualTo("80202");
      assertThat(contact.getCountry()).isEqualTo("US");
      assertThat(contact.getTimezone()).isEqualTo("America/Denver");
      assertThat(contact.getPreferredLanguage()).isEqualTo("en");
      assertThat(contact.getWebsite()).isEqualTo("https://example.com");
      assertThat(contact.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/johndoe");
      assertThat(contact.getDescription()).isEqualTo("Test contact");
      assertThat(contact.getLeadScore()).isEqualTo(75);
      assertThat(contact.getDoNotEmail()).isTrue();
      assertThat(contact.getDoNotCall()).isFalse();
      assertThat(contact.getUnsubscribeDate()).isEqualTo(now);
      assertThat(contact.getLastActivityDate()).isEqualTo(now);
      assertThat(contact.getLastContactedDate()).isEqualTo(now);
      assertThat(contact.getCreateDate()).isEqualTo(now);
      assertThat(contact.getModifyDate()).isEqualTo(now);
   }



   /*******************************************************************************
    ** Test all plain setters.
    *******************************************************************************/
   @Test
   void testPlainSetters()
   {
      Instant now = Instant.now();
      Contact contact = new Contact();

      contact.setId(42);
      contact.setFirstName("Jane");
      contact.setLastName("Smith");
      contact.setEmail("jane@example.com");
      contact.setSecondaryEmail("jane2@example.com");
      contact.setPhone("555-0200");
      contact.setMobilePhone("555-0201");
      contact.setJobTitle("CTO");
      contact.setDepartment("Engineering");
      contact.setCompanyId(20);
      contact.setLifecycleStageId(5);
      contact.setLeadSourceId(6);
      contact.setOwnerUserId("user-002");
      contact.setAddressLine1("456 Oak Ave");
      contact.setAddressLine2("Floor 3");
      contact.setCity("Boulder");
      contact.setState("CO");
      contact.setPostalCode("80301");
      contact.setCountry("US");
      contact.setTimezone("America/Chicago");
      contact.setPreferredLanguage("es");
      contact.setWebsite("https://jane.com");
      contact.setLinkedinUrl("https://linkedin.com/in/jane");
      contact.setDescription("Another contact");
      contact.setLeadScore(50);
      contact.setDoNotEmail(false);
      contact.setDoNotCall(true);
      contact.setUnsubscribeDate(now);
      contact.setLastActivityDate(now);
      contact.setLastContactedDate(now);
      contact.setCreateDate(now);
      contact.setModifyDate(now);

      assertThat(contact.getId()).isEqualTo(42);
      assertThat(contact.getFirstName()).isEqualTo("Jane");
      assertThat(contact.getLastName()).isEqualTo("Smith");
      assertThat(contact.getEmail()).isEqualTo("jane@example.com");
      assertThat(contact.getSecondaryEmail()).isEqualTo("jane2@example.com");
      assertThat(contact.getPhone()).isEqualTo("555-0200");
      assertThat(contact.getMobilePhone()).isEqualTo("555-0201");
      assertThat(contact.getJobTitle()).isEqualTo("CTO");
      assertThat(contact.getDepartment()).isEqualTo("Engineering");
      assertThat(contact.getCompanyId()).isEqualTo(20);
      assertThat(contact.getLifecycleStageId()).isEqualTo(5);
      assertThat(contact.getLeadSourceId()).isEqualTo(6);
      assertThat(contact.getOwnerUserId()).isEqualTo("user-002");
      assertThat(contact.getAddressLine1()).isEqualTo("456 Oak Ave");
      assertThat(contact.getAddressLine2()).isEqualTo("Floor 3");
      assertThat(contact.getCity()).isEqualTo("Boulder");
      assertThat(contact.getState()).isEqualTo("CO");
      assertThat(contact.getPostalCode()).isEqualTo("80301");
      assertThat(contact.getCountry()).isEqualTo("US");
      assertThat(contact.getTimezone()).isEqualTo("America/Chicago");
      assertThat(contact.getPreferredLanguage()).isEqualTo("es");
      assertThat(contact.getWebsite()).isEqualTo("https://jane.com");
      assertThat(contact.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/jane");
      assertThat(contact.getDescription()).isEqualTo("Another contact");
      assertThat(contact.getLeadScore()).isEqualTo(50);
      assertThat(contact.getDoNotEmail()).isFalse();
      assertThat(contact.getDoNotCall()).isTrue();
      assertThat(contact.getUnsubscribeDate()).isEqualTo(now);
      assertThat(contact.getLastActivityDate()).isEqualTo(now);
      assertThat(contact.getLastContactedDate()).isEqualTo(now);
      assertThat(contact.getCreateDate()).isEqualTo(now);
      assertThat(contact.getModifyDate()).isEqualTo(now);
   }

}
