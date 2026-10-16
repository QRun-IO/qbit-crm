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


import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;


/*******************************************************************************
 ** Show the Setup card only to CRM settings holders (crm.manage) and only while
 ** a business setup step is still missing. Evaluated at request time, so a
 ** completed setup drops the card from per-session metadata and from render.
 *******************************************************************************/
public class CrmSetupVisibility implements CustomPermissionChecker
{
   /*******************************************************************************
    ** Require crm.hasAccess and crm.manage.hasAccess, then an incomplete setup.
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
   {
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(CrmPermissions.APP_ACCESS) || !QContext.getQSession().hasPermission(CrmPermissions.MANAGE))
      {
         throw (new QPermissionDeniedException("CRM settings access is required."));
      }
      try
      {
         if(CrmSetupStatus.isIncomplete())
         {
            return;
         }
      }
      catch(QException e)
      {
         throw (new QPermissionDeniedException("Setup status is not available.", e));
      }
      throw (new QPermissionDeniedException("CRM setup is complete."));
   }
}
