/*******************************************************************************
 ** Unit test for Company entity
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model;


import java.math.BigDecimal;
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
 ** Unit test for Company entity -- verifies field definitions, self-referential
 ** parentCompanyId, and insert/query round-trip.
 *******************************************************************************/
class CompanyTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the table is registered and has expected fields.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Company.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("domain");
      assertThat(table.getFields()).containsKey("companyType");
      assertThat(table.getFields()).containsKey("industryId");
      assertThat(table.getFields()).containsKey("employeeCount");
      assertThat(table.getFields()).containsKey("annualRevenue");
      assertThat(table.getFields()).containsKey("annualRevenueCurrencyCode");
      assertThat(table.getFields()).containsKey("phone");
      assertThat(table.getFields()).containsKey("website");
      assertThat(table.getFields()).containsKey("ownerUserId");
      assertThat(table.getFields()).containsKey("parentCompanyId");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
      assertThat(table.getIcon().getName()).isEqualTo("business");
   }



   /*******************************************************************************
    ** Verify self-referential parentCompanyId field references the company PVS.
    *******************************************************************************/
   @Test
   void testSelfReferentialParentCompanyId()
   {
      QTableMetaData table = QContext.getQInstance().getTable(Company.TABLE_NAME);
      assertThat(table.getField("parentCompanyId").getPossibleValueSourceName())
         .isEqualTo(Company.TABLE_NAME);
   }



   /*******************************************************************************
    ** Verify insert and query round-trip with parent-child hierarchy.
    *******************************************************************************/
   @Test
   void testInsertAndQueryWithParentCompany() throws Exception
   {
      ///////////////////////////////////
      // insert parent company         //
      ///////////////////////////////////
      InsertInput parentInsert = new InsertInput();
      parentInsert.setTableName(Company.TABLE_NAME);
      parentInsert.setRecords(List.of(new Company()
         .withName("Acme Corp")
         .withDomain("acme.com")
         .withOwnerUserId("user1")
         .withAnnualRevenue(new BigDecimal("5000000"))
         .toQRecord()));
      InsertOutput parentOutput = new InsertAction().execute(parentInsert);
      Integer parentId = new Company(parentOutput.getRecords().get(0)).getId();

      ///////////////////////////////////
      // insert child company          //
      ///////////////////////////////////
      InsertInput childInsert = new InsertInput();
      childInsert.setTableName(Company.TABLE_NAME);
      childInsert.setRecords(List.of(new Company()
         .withName("Acme West")
         .withOwnerUserId("user2")
         .withParentCompanyId(parentId)
         .toQRecord()));
      new InsertAction().execute(childInsert);

      ///////////////////////////////////
      // query and verify              //
      ///////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(Company.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(2);

      Company child = queryOutput.getRecords().stream()
         .map(Company::new)
         .filter(c -> "Acme West".equals(c.getName()))
         .findFirst()
         .orElseThrow();

      assertThat(child.getParentCompanyId()).isEqualTo(parentId);
   }

}
