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


package com.kingsrook.qbits.crm.core.customizers;


import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.crm.core.model.Contact;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.actions.permissions.FieldSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** RC.6 recordLabelFormat is printf-only; preserve a computed query/get label.
 *******************************************************************************/
public class ContactLabelCustomizer extends AbstractPostQueryCustomizer
{
   /*******************************************************************************
    ** Keep missing names absent in storage while giving records useful labels.
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records)
   {
      for(QRecord record : records)
      {
         QRecord visible = new QRecord().withValue("id", record.getValue("id"));
         for(String name : List.of("firstName", "lastName", "email"))
         {
            var field = QContext.getQInstance().getTable(Contact.TABLE_NAME).getField(name);
            if(FieldSecurityLockHelper.isFieldAccessible(field)
               && !(input != null && input.getShouldOmitHiddenFields() && field.getIsHidden())
               && !(input != null && input.getShouldMaskPasswords() && field.getType().needsMasked()))
            {
               visible.setValue(name, record.getValue(name));
            }
         }
         record.setRecordLabel(label(visible));
      }
      return (records);
   }


   /*******************************************************************************
    ** Prefer the supplied name, then email, then the record identifier.
    *******************************************************************************/
   public static String label(QRecord record)
   {
      String name = (Objects.toString(record.getValue("firstName"), "").strip() + " "
         + Objects.toString(record.getValue("lastName"), "").strip()).strip();
      return (name.isBlank() ? Objects.toString(record.getValue("email"), Objects.toString(record.getValue("id"), "Contact")) : name);
   }
}
