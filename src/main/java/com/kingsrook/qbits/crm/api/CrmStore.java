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
import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.crm.CrmSessionUtils;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Internal table-action boundary. Explicitly selects trusted host work or caller permission enforcement.
 *******************************************************************************/
final class CrmStore
{
   private final PermissionEnforcement permissionEnforcement;

   /*******************************************************************************
    ** Select permission enforcement for all actions in this store.
    *******************************************************************************/
   private CrmStore(PermissionEnforcement permissionEnforcement)
   {
      this.permissionEnforcement = permissionEnforcement;
   }

   /*******************************************************************************
    ** Validated host intake is internal work, independent of the visitor's permissions.
    *******************************************************************************/
   static CrmStore trusted()
   {
      return (new CrmStore(PermissionEnforcement.SKIP));
   }

   /*******************************************************************************
    ** Signed-in users and marketing workers must hold the permissions for their work.
    *******************************************************************************/
   static CrmStore asUser()
   {
      return (new CrmStore(PermissionEnforcement.ENFORCE));
   }

   /*******************************************************************************
    ** Core requires an explicit reason when selecting SKIP for internal work.
    *******************************************************************************/
   private void applyPermissions(AbstractActionInput input)
   {
      if(permissionEnforcement == PermissionEnforcement.SKIP)
      {
         input.withPermissionBypass("CRM intake is trusted host work after public request validation");
      }
      else
      {
         input.withPermissionEnforcement(permissionEnforcement);
      }
   }

   /*******************************************************************************
    ** Query through core actions.
    *******************************************************************************/
   List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      QueryInput input = new QueryInput(table).withFilter(filter);
      applyPermissions(input);
      return (new QueryAction().execute(input).getRecords());
   }

   /*******************************************************************************
    ** Query a single field.
    *******************************************************************************/
   List<QRecord> by(String table, String field, Serializable value) throws QException
   {
      return (query(table, new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value))));
   }

   /*******************************************************************************
    ** Retrieve a visible record or fail without disclosing its contents.
    *******************************************************************************/
   QRecord get(String table, Integer id) throws QException
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
   QRecord insert(String table, QRecord record) throws QException
   {
      InsertInput input = new InsertInput(table).withRecord(record);
      applyPermissions(input);
      return (checked(new InsertAction().execute(input).getRecords()));
   }

   /*******************************************************************************
    ** Update without bypassing customizers, audit or locks.
    *******************************************************************************/
   QRecord update(String table, QRecord record) throws QException
   {
      UpdateInput input = new UpdateInput(table).withRecords(List.of(record));
      applyPermissions(input);
      return (checked(new UpdateAction().execute(input).getRecords()));
   }

   /*******************************************************************************
    ** Record an interaction with the same permission mode as the initiating operation.
    *******************************************************************************/
   void recordActivity(Integer contactId, ActivityKind kind, String subject, String description) throws QException
   {
      get("crm_contact", contactId);
      if(kind == null || subject == null || subject.isBlank())
      {
         throw (new QUserFacingException("An activity kind and subject are required."));
      }
      CrmStarterData.ensure(this);
      Integer typeId = by("crm_activity_type", "code", kind.name()).get(0).getValueInteger("id");
      insert("crm_activity", new QRecord().withValue("contactId", contactId).withValue("activityTypeId", typeId)
         .withValue("subject", subject).withValue("description", description).withValue("ownerUserId", CrmSessionUtils.getCurrentUserId())
         .withValue("isCompleted", true).withValue("completedDate", Instant.now()));
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
