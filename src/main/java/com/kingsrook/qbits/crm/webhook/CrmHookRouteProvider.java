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

package com.kingsrook.qbits.crm.webhook;


import com.kingsrook.qbits.crm.api.CrmApi;
import com.kingsrook.qbits.crm.api.CrmIntakeApi;
import com.kingsrook.qbits.crm.security.CrmTokens;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;


/*******************************************************************************
 ** Signed machine routes outside the UI API. GET never mutates records, and pages never display personal data or tokens.
 *******************************************************************************/
public class CrmHookRouteProvider implements QJavalinRouteProviderInterface
{
   private QInstance instance;

   /*******************************************************************************
    ** Register once when the host has Javalin middleware.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      QJavalinMetaData metadata = QJavalinMetaData.ofOrWithNew(instance);
      if(CollectionUtils.nonNullList(metadata.getAdditionalRouteProviderReferences()).stream()
         .noneMatch(reference -> reference != null && CrmHookRouteProvider.class.getName().equals(reference.getName())))
      {
         metadata.withAdditionalRouteProviderReference(new QCodeReference(CrmHookRouteProvider.class));
      }
   }

   @Override
   public void setQInstance(QInstance instance)
   {
      this.instance = instance;
   }

   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.routes.get("/qqq/hooks/crm/confirm/{token}", context -> handle(context, "confirm", false));
      config.routes.post("/qqq/hooks/crm/confirm/{token}", context -> handle(context, "confirm", true));
      config.routes.get("/qqq/hooks/crm/unsubscribe/{token}", context -> handle(context, "unsubscribe", false));
      config.routes.post("/qqq/hooks/crm/unsubscribe/{token}", context -> handle(context, "unsubscribe", true));
   }

   /*******************************************************************************
    ** The signed token authorizes only the identified operation; restore the caller context afterward.
    *******************************************************************************/
   private void handle(Context context, String purpose, boolean post)
   {
      CapturedContext previous = QContext.capture();
      try
      {
         QContext.init(instance, new QSystemUserSession(instance));
         context.header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
            .header("Content-Security-Policy", "default-src 'none'; form-action 'self'; frame-ancestors 'none'");
         String token = context.pathParam("token");
         new CrmTokens(CrmApi.config()).verify(token, purpose);
         if(post)
         {
            CrmIntakeApi api = new CrmIntakeApi();
            if("confirm".equals(purpose))
            {
               api.confirm(token);
            }
            else
            {
               api.unsubscribe(token);
            }
            page(context, 200, "Preference saved", "Your email preference has been saved.", null);
         }
         else
         {
            String label = "confirm".equals(purpose) ? "Confirm subscription" : "Unsubscribe";
            page(context, 200, label, "Choose the button below to save your email preference.", label);
         }
      }
      catch(QUserFacingException e)
      {
         page(context, 400, "Link unavailable", "This link is invalid or has expired. Please request a new link.", null);
      }
      catch(Exception e)
      {
         page(context, 503, "Please try again", "We could not save your preference. Please try again later.", null);
      }
      finally
      {
         QContext.init(previous);
      }
   }

   /*******************************************************************************
    ** All content is fixed text; the browser posts to its current URL without embedding the token.
    *******************************************************************************/
   private void page(Context context, int status, String title, String message, String button)
   {
      context.status(status).contentType("text/html; charset=utf-8").result("<!doctype html><html lang=\"en\"><head>"
         + "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + title
         + "</title></head><body><main><h1>" + title + "</h1><p>" + message + "</p>"
         + (button == null ? "" : "<form method=\"post\"><button type=\"submit\">" + button + "</button></form>")
         + "</main></body></html>");
   }
}
