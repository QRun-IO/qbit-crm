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
 ** POST_INSERT customizer for form submissions. Auto-links contactId if a
 ** contact with a matching email address already exists.
 *******************************************************************************/

package com.kingsrook.qbits.crm.core.customizers;


import java.util.List;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qbits.crm.core.model.FormSubmission;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** On form submission insert, auto-link the contactId if a contact with the
 ** same email address already exists in the CRM.
 *******************************************************************************/
public class FormSubmissionAutoLinkCustomizer extends AbstractPostInsertCustomizer
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      for(QRecord record : records)
      {
         String email = record.getValueString("email");
         if(!StringUtils.hasContent(email))
         {
            continue;
         }

         email = email.trim().toLowerCase();

         ///////////////////////////////////////////////////////////
         // skip if contactId is already set (manual or re-insert) //
         ///////////////////////////////////////////////////////////
         if(record.getValue("contactId") != null)
         {
            continue;
         }

         /////////////////////////////////////////////////////
         // query for an existing contact with this email    //
         /////////////////////////////////////////////////////
         QueryOutput contactQuery = new QueryAction().execute(
            new QueryInput(Contact.TABLE_NAME)
               .withFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("email", QCriteriaOperator.EQUALS, email))));

         if(!contactQuery.getRecords().isEmpty())
         {
            Integer contactId = contactQuery.getRecords().get(0).getValueInteger("id");

            /////////////////////////////////////////
            // update the form submission record    //
            /////////////////////////////////////////
            QRecord updateRecord = new QRecord();
            updateRecord.setValue("id", record.getValueInteger("id"));
            updateRecord.setValue("contactId", contactId);

            new UpdateAction().execute(
               new UpdateInput(FormSubmission.TABLE_NAME)
                  .withRecords(List.of(updateRecord)));
         }
      }

      return (records);
   }

}
