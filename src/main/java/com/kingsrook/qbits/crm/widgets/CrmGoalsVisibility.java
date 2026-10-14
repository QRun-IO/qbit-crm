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


package com.kingsrook.qbits.crm.widgets;


import java.time.LocalDate;
import java.time.ZoneOffset;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;


/*******************************************************************************
 ** Hide Goals in per-session frontend metadata unless a current goal is visible.
 ** This is evaluated at request time, never while building shared app metadata.
 *******************************************************************************/
public class CrmGoalsVisibility implements CustomPermissionChecker
{
   /*******************************************************************************
    ** Preserve crm.hasAccess and check existence through a bounded scoped query.
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
   {
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(CrmPermissions.APP_ACCESS))
      {
         throw (new QPermissionDeniedException("CRM access is required."));
      }
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("periodStart", QCriteriaOperator.LESS_THAN_OR_EQUALS, today))
         .withCriteria(new QFilterCriteria("periodEnd", QCriteriaOperator.GREATER_THAN_OR_EQUALS, today));
      try
      {
         if(!CrmHomeRenderer.query("crm_sales_goal", filter, "id", true, 1).isEmpty())
         {
            return;
         }
      }
      catch(QException e)
      {
         throw (new QPermissionDeniedException("Current goals are not available.", e));
      }
      throw (new QPermissionDeniedException("No goals for the current period."));
   }
}
