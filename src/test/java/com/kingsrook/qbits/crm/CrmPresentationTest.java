/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.kingsrook.qbits.crm;


import java.util.List;
import com.kingsrook.qbits.crm.core.customizers.ContactLabelCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Presentation contracts remain stable across all discovered CRM metadata.
 *******************************************************************************/
class CrmPresentationTest extends BaseTest
{
   /*******************************************************************************
    ** Every table has an explicit human label and user identifiers read naturally.
    *******************************************************************************/
   @Test
   void labelsAndMoney()
   {
      var instance = QContext.getQInstance();
      for(var table : instance.getTables().values())
      {
         assertThat(table.getLabel()).isNotBlank().doesNotContain("_").isNotEqualToIgnoringCase(table.getName());
         table.getFields().values().stream().filter(field -> field.getName().endsWith("UserId"))
            .forEach(field -> assertThat(field.getLabel()).doesNotContain("User Id"));
      }
      assertThat(instance.getTable("crm_contact").getLabel()).isEqualTo("Contacts");
      assertThat(instance.getTable("crm_deal").getField("amount").getDisplayFormat()).isEqualTo(DisplayFormat.CURRENCY);
      assertThat(instance.getTable("crm_deal_product").getField("totalAmount").getDisplayFormat()).isEqualTo(DisplayFormat.CURRENCY);
      assertThat(instance.getTable("crm_product").getField("unitPrice").getDisplayFormat()).isEqualTo(DisplayFormat.CURRENCY);
      assertThat(instance.getTable("crm_sales_goal").getField("targetAmount").getDisplayFormat()).isEqualTo(DisplayFormat.CURRENCY);
      for(String table : List.of("crm_contact", "crm_deal"))
      {
         assertThat(instance.getTable(table).getField("ownerUserId").getIsRequired()).isFalse();
         assertThat(instance.getTable(table).getField("ownerUserId").getLabel()).isEqualTo("Owner");
      }
   }


   /*******************************************************************************
    ** All record-screen child lists have at most four valid, deliberate columns.
    *******************************************************************************/
   @Test
   void sectionsAndChildLists()
   {
      var instance = QContext.getQInstance();
      var contact = instance.getTable("crm_contact");
      assertThat(contact.getSections()).extracting(section -> section.getName())
         .contains("details", "contactInformation", "attribution", "system").doesNotContain("online", "address", "communication", "data", "dates");
      assertThat(contact.getSections().stream().filter(section -> section.getName().equals("system")).findFirst().orElseThrow().getTier()).isEqualTo(Tier.T3);
      for(String parent : List.of("crm_contact", "crm_company", "crm_deal"))
      {
         instance.getTable(parent).getSections().stream().filter(section -> section.getWidgetName() != null).forEach(section ->
         {
            var widget = instance.getWidget(section.getWidgetName());
            if(widget.getDefaultValues() != null && widget.getDefaultValues().get("joinName") instanceof String joinName)
            {
               var child = instance.getTable(instance.getJoin(joinName).getRightTable());
               List<?> fields = (List<?>) widget.getDefaultValues().get("onlyIncludeFieldNames");
               assertThat(fields).isNotEmpty().hasSizeLessThanOrEqualTo(4);
               fields.forEach(field -> assertThat(child.getFields()).containsKey(field.toString()));
            }
         });
      }
   }


   /*******************************************************************************
    ** A computed label must not re-expose fields omitted by the record view.
    *******************************************************************************/
   @Test
   void hiddenNamesStayOutOfLabels() throws Exception
   {
      QRecord inserted = new InsertAction().execute(new InsertInput("crm_contact").withRecord(new QRecord()
         .withValue("firstName", "Private").withValue("lastName", "Name").withValue("email", "visible@example.test"))).getRecords().getFirst();
      var table = QContext.getQInstance().getTable("crm_contact");
      table.getField("firstName").setIsHidden(true);
      table.getField("lastName").setIsHidden(true);
      var input = new com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput("crm_contact").withPrimaryKey(inserted.getValue("id"));
      input.setShouldOmitHiddenFields(true);
      QRecord visible = new GetAction().execute(input).getRecord();
      assertThat(visible.getRecordLabel()).isEqualTo("visible@example.test");
      assertThat(visible.getValue("firstName")).isNull();
   }


   /*******************************************************************************
    ** Missing names remain null while get and query return useful record labels.
    *******************************************************************************/
   @Test
   void contactNameFallbacks() throws Exception
   {
      for(String[] values : new String[][] {{"Ada", "Lovelace", "Ada Lovelace"}, {"Ada", null, "Ada"}, {null, null, "ada@example.test"}})
      {
         QRecord inserted = new InsertAction().execute(new InsertInput("crm_contact").withRecord(new QRecord()
            .withValue("firstName", values[0]).withValue("lastName", values[1]).withValue("email", "ada@example.test"))).getRecords().getFirst();
         assertThat(inserted.getErrors()).isNullOrEmpty();
         QRecord record = GetAction.execute("crm_contact", inserted.getValue("id"));
         assertThat(record.getRecordLabel()).isEqualTo(values[2]);
         assertThat(record.getValue("lastName")).isEqualTo(values[1]);
      }
      assertThat(new QueryAction().execute(new QueryInput("crm_contact")).getRecords()).extracting(QRecord::getRecordLabel)
         .containsExactly("Ada Lovelace", "Ada", "ada@example.test");
      assertThat(ContactLabelCustomizer.label(new QRecord().withValue("firstName", "  ").withValue("lastName", " ").withValue("email", "mail@example.test")))
         .isEqualTo("mail@example.test");
   }
}
