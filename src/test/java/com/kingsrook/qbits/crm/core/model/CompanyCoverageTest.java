/*******************************************************************************
 ** Coverage tests for Company entity -- exercises every getter/setter/withX.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
import java.time.Instant;
import com.kingsrook.qbits.crm.BaseTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercises every fluent setter and getter on Company.
 *******************************************************************************/
class CompanyCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** Test all fluent setters and getters.
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      Instant now = Instant.now();

      Company company = new Company()
         .withId(1)
         .withName("Acme Corp")
         .withDomain("acme.com")
         .withCompanyType(1)
         .withIndustryId(5)
         .withEmployeeCount(500)
         .withAnnualRevenue(new BigDecimal("10000000.00"))
         .withAnnualRevenueCurrencyCode("EUR")
         .withPhone("555-1000")
         .withWebsite("https://acme.com")
         .withAddressLine1("100 Industrial Way")
         .withAddressLine2("Building B")
         .withCity("Chicago")
         .withState("IL")
         .withPostalCode("60601")
         .withCountry("US")
         .withOwnerUserId("user-001")
         .withParentCompanyId(99)
         .withDescription("Test company")
         .withLastActivityDate(now)
         .withLastContactedDate(now)
         .withCreateDate(now)
         .withModifyDate(now);

      assertThat(company.getId()).isEqualTo(1);
      assertThat(company.getName()).isEqualTo("Acme Corp");
      assertThat(company.getDomain()).isEqualTo("acme.com");
      assertThat(company.getCompanyType()).isEqualTo(1);
      assertThat(company.getIndustryId()).isEqualTo(5);
      assertThat(company.getEmployeeCount()).isEqualTo(500);
      assertThat(company.getAnnualRevenue()).isEqualByComparingTo(new BigDecimal("10000000.00"));
      assertThat(company.getAnnualRevenueCurrencyCode()).isEqualTo("EUR");
      assertThat(company.getPhone()).isEqualTo("555-1000");
      assertThat(company.getWebsite()).isEqualTo("https://acme.com");
      assertThat(company.getAddressLine1()).isEqualTo("100 Industrial Way");
      assertThat(company.getAddressLine2()).isEqualTo("Building B");
      assertThat(company.getCity()).isEqualTo("Chicago");
      assertThat(company.getState()).isEqualTo("IL");
      assertThat(company.getPostalCode()).isEqualTo("60601");
      assertThat(company.getCountry()).isEqualTo("US");
      assertThat(company.getOwnerUserId()).isEqualTo("user-001");
      assertThat(company.getParentCompanyId()).isEqualTo(99);
      assertThat(company.getDescription()).isEqualTo("Test company");
      assertThat(company.getLastActivityDate()).isEqualTo(now);
      assertThat(company.getLastContactedDate()).isEqualTo(now);
      assertThat(company.getCreateDate()).isEqualTo(now);
      assertThat(company.getModifyDate()).isEqualTo(now);
   }



   /*******************************************************************************
    ** Test all plain setters.
    *******************************************************************************/
   @Test
   void testPlainSetters()
   {
      Instant now = Instant.now();
      Company company = new Company();

      company.setId(42);
      company.setName("Widget Inc");
      company.setDomain("widget.com");
      company.setCompanyType(2);
      company.setIndustryId(10);
      company.setEmployeeCount(100);
      company.setAnnualRevenue(new BigDecimal("5000000.00"));
      company.setAnnualRevenueCurrencyCode("GBP");
      company.setPhone("555-2000");
      company.setWebsite("https://widget.com");
      company.setAddressLine1("200 Tech Park");
      company.setAddressLine2("Unit 5");
      company.setCity("Austin");
      company.setState("TX");
      company.setPostalCode("73301");
      company.setCountry("US");
      company.setOwnerUserId("user-002");
      company.setParentCompanyId(50);
      company.setDescription("Another company");
      company.setLastActivityDate(now);
      company.setLastContactedDate(now);
      company.setCreateDate(now);
      company.setModifyDate(now);

      assertThat(company.getId()).isEqualTo(42);
      assertThat(company.getName()).isEqualTo("Widget Inc");
      assertThat(company.getDomain()).isEqualTo("widget.com");
      assertThat(company.getCompanyType()).isEqualTo(2);
      assertThat(company.getIndustryId()).isEqualTo(10);
      assertThat(company.getEmployeeCount()).isEqualTo(100);
      assertThat(company.getAnnualRevenue()).isEqualByComparingTo(new BigDecimal("5000000.00"));
      assertThat(company.getAnnualRevenueCurrencyCode()).isEqualTo("GBP");
      assertThat(company.getPhone()).isEqualTo("555-2000");
      assertThat(company.getWebsite()).isEqualTo("https://widget.com");
      assertThat(company.getAddressLine1()).isEqualTo("200 Tech Park");
      assertThat(company.getAddressLine2()).isEqualTo("Unit 5");
      assertThat(company.getCity()).isEqualTo("Austin");
      assertThat(company.getState()).isEqualTo("TX");
      assertThat(company.getPostalCode()).isEqualTo("73301");
      assertThat(company.getCountry()).isEqualTo("US");
      assertThat(company.getOwnerUserId()).isEqualTo("user-002");
      assertThat(company.getParentCompanyId()).isEqualTo(50);
      assertThat(company.getDescription()).isEqualTo("Another company");
      assertThat(company.getLastActivityDate()).isEqualTo(now);
      assertThat(company.getLastContactedDate()).isEqualTo(now);
      assertThat(company.getCreateDate()).isEqualTo(now);
      assertThat(company.getModifyDate()).isEqualTo(now);
   }

}
