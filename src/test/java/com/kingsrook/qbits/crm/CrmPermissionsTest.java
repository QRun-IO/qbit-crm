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


package com.kingsrook.qbits.crm;


import java.util.Set;
import com.kingsrook.qbits.crm.security.CrmPermissions;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise the core permission checker with ordinary, management and admin grants.
 *******************************************************************************/
class CrmPermissionsTest extends BaseTest
{
   /*******************************************************************************
    ** Legacy hosts retain management access; the shared catalog is opt-in.
    *******************************************************************************/
   @Test
   void testDefaultCatalogPermissions()
   {
      QueryInput input = new QueryInput("crm_product");
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.MANAGE_READ, CrmPermissions.MANAGE_WRITE));
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", TablePermissionSubType.READ)).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", TablePermissionSubType.EDIT)).isTrue();
      QContext.getQSession().setPermissions(Set.of("crm.product.read"));
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", TablePermissionSubType.READ)).isFalse();
   }



   /*******************************************************************************
    ** Native catalog read grants cannot write or open unrelated CRM settings.
    *******************************************************************************/
   @Test
   void testSharedCatalogPermissions() throws Exception
   {
      com.kingsrook.qqq.backend.core.model.metadata.QInstance instance = new com.kingsrook.qqq.backend.core.model.metadata.QInstance();
      instance.withInstanceDefaultAuthentication(new com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData()
         .withName("anonymous").withType(com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType.FULLY_ANONYMOUS));
      instance.addBackend(QContext.getQInstance().getBackend(BACKEND_NAME));
      new CrmQBitProducer().withQBitConfig(new CrmQBitConfig().withDefaultBackendNameForTables(BACKEND_NAME)
         .withProductPermissionBase("crm.product")).produce(instance).addSelfToInstance(instance);
      QContext.init(instance, QContext.getQSession());
      QueryInput input = new QueryInput("crm_product");
      QContext.getQSession().setPermissions(Set.of("crm.product.read"));
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", TablePermissionSubType.READ)).isTrue();
      for(TablePermissionSubType write : Set.of(TablePermissionSubType.INSERT, TablePermissionSubType.EDIT, TablePermissionSubType.DELETE))
      {
         assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", write)).isFalse();
      }
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_pipeline", TablePermissionSubType.READ)).isFalse();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_contact", TablePermissionSubType.READ)).isFalse();
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_merge_contacts")).isFalse();
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.MANAGE_READ, CrmPermissions.MANAGE_WRITE, "crm.product.read", "crm.product.write"));
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_product", TablePermissionSubType.EDIT)).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_pipeline", TablePermissionSubType.EDIT)).isTrue();
   }



   /*******************************************************************************
    ** A read grant does not allow writes or settings access.
    *******************************************************************************/
   @Test
   void testReadOnlyAccess()
   {
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.READ));
      QueryInput input = new QueryInput("crm_contact");
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_contact", TablePermissionSubType.READ)).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_contact", TablePermissionSubType.EDIT)).isFalse();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_pipeline", TablePermissionSubType.READ)).isFalse();
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_gdpr_anonymize_contact")).isFalse();
      assertThat(PermissionsHelper.hasWidgetPermission(input, "crm_recent_web_leads")).isFalse();
   }



   /*******************************************************************************
    ** Settings and erasure grants are independent of everyday CRM access.
    *******************************************************************************/
   @Test
   void testManagementAndAdminAccess()
   {
      QueryInput input = new QueryInput("crm_contact");
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.APP_ACCESS, CrmPermissions.MANAGE_READ, CrmPermissions.MANAGE_WRITE, CrmPermissions.MANAGE));
      assertThat(PermissionsHelper.hasWidgetPermission(input, "crm_recent_web_leads")).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_pipeline", TablePermissionSubType.EDIT)).isTrue();
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_merge_contacts")).isTrue();
      assertThat(PermissionsHelper.hasTablePermission(input, "crm_currency", TablePermissionSubType.READ)).isTrue();
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_gdpr_anonymize_contact")).isFalse();
      QContext.getQSession().setPermissions(Set.of(CrmPermissions.ADMIN, CrmPermissions.ADMIN_READ, CrmPermissions.ADMIN_WRITE));
      assertThat(PermissionsHelper.hasProcessPermission(input, "crm_gdpr_anonymize_contact")).isTrue();
   }

}
