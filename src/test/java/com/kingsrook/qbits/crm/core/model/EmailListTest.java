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


/*******************************************************************************
 ** Unit test for EmailList and EmailListMember entities
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.model;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.BaseTest;
import com.kingsrook.qbits.crm.core.model.enums.CrmListMemberStatus;
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
 ** Unit test for EmailList entity -- verifies fields, PVS, and the
 ** EmailListMember junction table relationship.
 *******************************************************************************/
class EmailListTest extends BaseTest
{

   /*******************************************************************************
    ** Verify the EmailList table has expected fields and icon.
    *******************************************************************************/
   @Test
   void testEmailListTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailList.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("name");
      assertThat(table.getFields()).containsKey("slug");
      assertThat(table.getFields()).containsKey("description");
      assertThat(table.getFields()).containsKey("isActive");
      assertThat(table.getFields()).containsKey("isPublic");
      assertThat(table.getFields()).containsKey("sortOrder");
      assertThat(table.getFields()).containsKey("memberCount");
      assertThat(table.getFields()).containsKey("createDate");
      assertThat(table.getFields()).containsKey("modifyDate");
      assertThat(table.getIcon().getName()).isEqualTo("mail");
   }



   /*******************************************************************************
    ** Verify EmailList PVS is produced.
    *******************************************************************************/
   @Test
   void testEmailListPossibleValueSource()
   {
      assertThat(QContext.getQInstance().getPossibleValueSource(EmailList.TABLE_NAME)).isNotNull();
   }



   /*******************************************************************************
    ** Verify EmailListMember table has expected fields.
    *******************************************************************************/
   @Test
   void testEmailListMemberTableMetaData()
   {
      QTableMetaData table = QContext.getQInstance().getTable(EmailListMember.TABLE_NAME);
      assertThat(table).isNotNull();
      assertThat(table.getFields()).containsKey("id");
      assertThat(table.getFields()).containsKey("emailListId");
      assertThat(table.getFields()).containsKey("contactId");
      assertThat(table.getFields()).containsKey("email");
      assertThat(table.getFields()).containsKey("firstName");
      assertThat(table.getFields()).containsKey("status");
      assertThat(table.getFields()).containsKey("subscribedDate");
      assertThat(table.getFields()).containsKey("unsubscribedDate");
      assertThat(table.getFields()).containsKey("subscriptionSource");
      assertThat(table.getFields()).containsKey("formSubmissionId");
      assertThat(table.getIcon().getName()).isEqualTo("person_add");
   }



   /*******************************************************************************
    ** Verify inserting an EmailList and adding members to it.
    *******************************************************************************/
   @Test
   void testInsertListAndMember() throws Exception
   {
      ///////////////////////////////////
      // insert email list             //
      ///////////////////////////////////
      InsertInput listInsert = new InsertInput();
      listInsert.setTableName(EmailList.TABLE_NAME);
      listInsert.setRecords(List.of(new EmailList()
         .withName("Newsletter")
         .withListKey("newsletter").withSlug("newsletter")
         .withIsActive(true)
         .withIsPublic(true)
         .withSortOrder(1)
         .toQRecord()));
      InsertOutput listOutput = new InsertAction().execute(listInsert);
      Integer listId = new EmailList(listOutput.getRecords().get(0)).getId();
      assertThat(listId).isNotNull();

      ///////////////////////////////////
      // insert list member            //
      ///////////////////////////////////
      InsertInput memberInsert = new InsertInput();
      memberInsert.setTableName(EmailListMember.TABLE_NAME);
      memberInsert.setRecords(List.of(new EmailListMember()
         .withEmailListId(listId)
         .withEmail("subscriber@example.com")
         .withFirstName("Alice")
         .withStatus(CrmListMemberStatus.SUBSCRIBED.getId())
         .withSubscribedDate(Instant.now())
         .withSubscriptionSource("website-footer")
         .toQRecord()));
      InsertOutput memberOutput = new InsertAction().execute(memberInsert);
      assertThat(memberOutput.getRecords()).hasSize(1);

      ///////////////////////////////////
      // query and verify member       //
      ///////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(EmailListMember.TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).hasSize(1);

      EmailListMember fetched = new EmailListMember(queryOutput.getRecords().get(0));
      assertThat(fetched.getEmailListId()).isEqualTo(listId);
      assertThat(fetched.getEmail()).isEqualTo("subscriber@example.com");
      assertThat(fetched.getFirstName()).isEqualTo("Alice");
      assertThat(fetched.getSubscriptionSource()).isEqualTo("website-footer");
   }

}
