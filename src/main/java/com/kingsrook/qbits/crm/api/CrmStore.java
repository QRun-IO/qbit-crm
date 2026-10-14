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

package com.kingsrook.qbits.crm.api;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Internal table-action boundary. Always enforces caller permissions and record locks.
 *******************************************************************************/
final class CrmStore
{
   /*******************************************************************************
    ** No instances.
    *******************************************************************************/
   private CrmStore()
   {
   }

   /*******************************************************************************
    ** Query through core actions.
    *******************************************************************************/
   static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      QueryInput input = new QueryInput(table).withFilter(filter);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }

   /*******************************************************************************
    ** Query a single field.
    *******************************************************************************/
   static List<QRecord> by(String table, String field, Serializable value) throws QException
   {
      return (query(table, new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value))));
   }

   /*******************************************************************************
    ** Retrieve a visible record or fail without disclosing its contents.
    *******************************************************************************/
   static QRecord get(String table, Integer id) throws QException
   {
      if(id == null)
      {
         throw (new QUserFacingException("A record identifier is required."));
      }
      List<QRecord> rows = by(table, "id", id);
      if(rows.isEmpty())
      {
         throw (new QUserFacingException("The CRM record is unavailable."));
      }
      return (rows.get(0));
   }

   /*******************************************************************************
    ** Insert and surface record-level validation failures.
    *******************************************************************************/
   static QRecord insert(String table, QRecord record) throws QException
   {
      InsertInput input = new InsertInput(table).withRecord(record);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (checked(new InsertAction().execute(input).getRecords()));
   }

   /*******************************************************************************
    ** Update without bypassing customizers, audit or locks.
    *******************************************************************************/
   static QRecord update(String table, QRecord record) throws QException
   {
      UpdateInput input = new UpdateInput(table).withRecords(List.of(record));
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (checked(new UpdateAction().execute(input).getRecords()));
   }

   /*******************************************************************************
    ** Never return success when the action refused a record.
    *******************************************************************************/
   private static QRecord checked(List<QRecord> records) throws QException
   {
      if(records.isEmpty() || (records.get(0).getErrors() != null && !records.get(0).getErrors().isEmpty()))
      {
         throw (new QUserFacingException("CRM could not save the record. Check required fields and permissions."));
      }
      return (records.get(0));
   }
}
