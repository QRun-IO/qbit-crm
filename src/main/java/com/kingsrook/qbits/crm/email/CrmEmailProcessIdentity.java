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

package com.kingsrook.qbits.crm.email;


import com.kingsrook.qqq.backend.core.actions.processes.listeners.ProcessLifecycleListenerInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;


/*******************************************************************************
 ** Carry the core process UUID across Review/Result and retries in process state.
 *******************************************************************************/
public class CrmEmailProcessIdentity implements ProcessLifecycleListenerInterface
{
   @Override
   public boolean appliesTo(String processName)
   {
      return ("crm_send_email".equals(processName));
   }

   @Override
   public void onProcessStarted(RunProcessInput input)
   {
      input.getProcessState().getValues().put("crmEmailProcessUUID", input.getProcessUUID());
   }

   @Override
   public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
   {
   }

   @Override
   public void onProcessFailed(RunProcessInput input, Exception exception)
   {
   }
}
