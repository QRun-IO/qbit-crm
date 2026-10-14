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

package com.kingsrook.qbits.crm.core;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Checked writes for background work: record errors must not advance checkpoints.
 *******************************************************************************/
public final class CrmRecords
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private CrmRecords()
   {
   }

   /*******************************************************************************
    ** Insert and require a successful persisted row.
    *******************************************************************************/
   public static QRecord insert(String table, QRecord row) throws QException
   {
      return (check(new InsertAction().execute(new InsertInput(table).withRecord(row)).getRecords()));
   }

   /*******************************************************************************
    ** Update and require a successful persisted row.
    *******************************************************************************/
   public static QRecord update(String table, QRecord row) throws QException
   {
      return (check(new UpdateAction().execute(new UpdateInput(table).withRecord(row)).getRecords()));
   }

   /*******************************************************************************
    ** DML errors are failed work, never successful job progress.
    *******************************************************************************/
   private static QRecord check(List<QRecord> rows) throws QException
   {
      if(rows.isEmpty() || rows.stream().anyMatch(row -> row.getErrors() != null && !row.getErrors().isEmpty()))
      {
         throw (new QException("CRM write was rejected"));
      }
      return (rows.getFirst());
   }
}
