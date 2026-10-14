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


import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qbits.crm.CrmQBitProducer;
import com.kingsrook.qbits.crm.email.CrmEmailTransport;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** CRM installation and email readiness for hosts and integrations.
 *******************************************************************************/
public final class CrmApi
{
   /*******************************************************************************
    ** CRM facade cannot be instantiated.
    *******************************************************************************/
   private CrmApi()
   {
   }

   /*******************************************************************************
    ** Whether the CRM metadata is installed in the supplied instance.
    *******************************************************************************/
   public static boolean isInstalled(QInstance instance)
   {
      return (instance != null && instance.getTable("crm_contact") != null && instance.getTable("crm_consent_record") != null
         && instance.getTable("crm_email_list_member") != null && instance.getTable("crm_form_submission") != null);
   }

   /*******************************************************************************
    ** Report the currently configured email transport.
    *******************************************************************************/
   public static CrmReadiness readiness() throws QException
   {
      return (isInstalled(QContext.getQInstance()) ? new CrmEmailTransport(config()).readiness() : CrmReadiness.NOT_CONFIGURED);
   }

   /*******************************************************************************
    ** Resolve configuration from the current instance, never global mutable state.
    *******************************************************************************/
   public static CrmQBitConfig config() throws QUserFacingException
   {
      var bit = QContext.getQInstance().getQBits().get(CrmQBitProducer.GROUP_ID + ":" + CrmQBitProducer.ARTIFACT_ID);
      if(bit == null || !(bit.getConfig() instanceof CrmQBitConfig config))
      {
         throw (new QUserFacingException("CRM is not installed in this application."));
      }
      return (config);
   }
}
