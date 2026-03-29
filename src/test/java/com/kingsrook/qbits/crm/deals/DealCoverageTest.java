/*******************************************************************************
 ** Coverage tests for Deal entity -- exercises every getter/setter/withX.
 *******************************************************************************/
package com.kingsrook.qbits.crm.deals;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.deals.model.Deal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercises every fluent setter and getter on Deal.
 *******************************************************************************/
class DealCoverageTest extends BaseTest
{

   /*******************************************************************************
    ** Test all fluent setters and getters.
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      Instant now = Instant.now();
      LocalDate today = LocalDate.now();

      Deal deal = new Deal()
         .withId(1)
         .withName("Big Deal")
         .withPipelineId(10)
         .withPipelineStageId(20)
         .withAmount(new BigDecimal("50000.00"))
         .withCurrencyCode("USD")
         .withAmountInBaseCurrency(new BigDecimal("50000.00"))
         .withProbabilityOverridePct(80)
         .withWeightedAmount(new BigDecimal("40000.00"))
         .withCompanyId(5)
         .withOwnerUserId("user-001")
         .withLeadSourceId(3)
         .withExpectedCloseDate(today)
         .withActualCloseDate(today)
         .withWinLossReasonId(1)
         .withStageEnteredDate(now)
         .withDescription("Important deal")
         .withPriority(2)
         .withLastActivityDate(now)
         .withCreateDate(now)
         .withModifyDate(now);

      assertThat(deal.getId()).isEqualTo(1);
      assertThat(deal.getName()).isEqualTo("Big Deal");
      assertThat(deal.getPipelineId()).isEqualTo(10);
      assertThat(deal.getPipelineStageId()).isEqualTo(20);
      assertThat(deal.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
      assertThat(deal.getCurrencyCode()).isEqualTo("USD");
      assertThat(deal.getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("50000.00"));
      assertThat(deal.getProbabilityOverridePct()).isEqualTo(80);
      assertThat(deal.getWeightedAmount()).isEqualByComparingTo(new BigDecimal("40000.00"));
      assertThat(deal.getCompanyId()).isEqualTo(5);
      assertThat(deal.getOwnerUserId()).isEqualTo("user-001");
      assertThat(deal.getLeadSourceId()).isEqualTo(3);
      assertThat(deal.getExpectedCloseDate()).isEqualTo(today);
      assertThat(deal.getActualCloseDate()).isEqualTo(today);
      assertThat(deal.getWinLossReasonId()).isEqualTo(1);
      assertThat(deal.getStageEnteredDate()).isEqualTo(now);
      assertThat(deal.getDescription()).isEqualTo("Important deal");
      assertThat(deal.getPriority()).isEqualTo(2);
      assertThat(deal.getLastActivityDate()).isEqualTo(now);
      assertThat(deal.getCreateDate()).isEqualTo(now);
      assertThat(deal.getModifyDate()).isEqualTo(now);
   }



   /*******************************************************************************
    ** Test all plain setters.
    *******************************************************************************/
   @Test
   void testPlainSetters()
   {
      Instant now = Instant.now();
      LocalDate today = LocalDate.now();
      Deal deal = new Deal();

      deal.setId(42);
      deal.setName("Small Deal");
      deal.setPipelineId(11);
      deal.setPipelineStageId(21);
      deal.setAmount(new BigDecimal("1000.00"));
      deal.setCurrencyCode("EUR");
      deal.setAmountInBaseCurrency(new BigDecimal("1100.00"));
      deal.setProbabilityOverridePct(50);
      deal.setWeightedAmount(new BigDecimal("550.00"));
      deal.setCompanyId(6);
      deal.setOwnerUserId("user-002");
      deal.setLeadSourceId(4);
      deal.setExpectedCloseDate(today);
      deal.setActualCloseDate(today);
      deal.setWinLossReasonId(2);
      deal.setStageEnteredDate(now);
      deal.setDescription("A small deal");
      deal.setPriority(1);
      deal.setLastActivityDate(now);
      deal.setCreateDate(now);
      deal.setModifyDate(now);

      assertThat(deal.getId()).isEqualTo(42);
      assertThat(deal.getName()).isEqualTo("Small Deal");
      assertThat(deal.getPipelineId()).isEqualTo(11);
      assertThat(deal.getPipelineStageId()).isEqualTo(21);
      assertThat(deal.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
      assertThat(deal.getCurrencyCode()).isEqualTo("EUR");
      assertThat(deal.getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("1100.00"));
      assertThat(deal.getProbabilityOverridePct()).isEqualTo(50);
      assertThat(deal.getWeightedAmount()).isEqualByComparingTo(new BigDecimal("550.00"));
      assertThat(deal.getCompanyId()).isEqualTo(6);
      assertThat(deal.getOwnerUserId()).isEqualTo("user-002");
      assertThat(deal.getLeadSourceId()).isEqualTo(4);
      assertThat(deal.getExpectedCloseDate()).isEqualTo(today);
      assertThat(deal.getActualCloseDate()).isEqualTo(today);
      assertThat(deal.getWinLossReasonId()).isEqualTo(2);
      assertThat(deal.getStageEnteredDate()).isEqualTo(now);
      assertThat(deal.getDescription()).isEqualTo("A small deal");
      assertThat(deal.getPriority()).isEqualTo(1);
      assertThat(deal.getLastActivityDate()).isEqualTo(now);
      assertThat(deal.getCreateDate()).isEqualTo(now);
      assertThat(deal.getModifyDate()).isEqualTo(now);
   }

}
