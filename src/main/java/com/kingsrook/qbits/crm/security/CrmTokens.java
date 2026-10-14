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

package com.kingsrook.qbits.crm.security;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import com.kingsrook.qbits.crm.CrmQBitConfig;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;


/*******************************************************************************
 ** Purpose-bound HMAC-SHA256 links. Payloads contain identifiers, expiry and a nonce, never personal data.
 *******************************************************************************/
public class CrmTokens
{
   private final CrmQBitConfig config;
   private final Clock clock;

   /*******************************************************************************
    ** Use the current UTC clock.
    *******************************************************************************/
   public CrmTokens(CrmQBitConfig config) throws QUserFacingException
   {
      this(config, Clock.systemUTC());
   }

   /*******************************************************************************
    ** Explicit clock permits deterministic expiry verification.
    *******************************************************************************/
   public CrmTokens(CrmQBitConfig config, Clock clock) throws QUserFacingException
   {
      if(config.getTokenSecret() == null || config.getTokenSecret().isBlank())
      {
         throw (new QUserFacingException("CRM intake requires CrmQBitConfig.withTokenSecret(String)."));
      }
      this.config = config;
      this.clock = clock;
   }

   /*******************************************************************************
    ** Issue a link valid for ninety days. Confirm ids identify members; unsubscribe ids identify contacts.
    *******************************************************************************/
   public String issue(String purpose, Integer id, Integer listId) throws QUserFacingException
   {
      if(!("confirm".equals(purpose) || "unsubscribe".equals(purpose)) || id == null || id <= 0
         || (listId != null && listId <= 0) || ("confirm".equals(purpose) && listId == null))
      {
         throw invalid();
      }
      String payload = purpose + ":" + id + ":" + (listId == null ? "" : listId) + ":"
         + clock.instant().plus(Duration.ofDays(90)).getEpochSecond() + ":" + UUID.randomUUID();
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
      return (encoded + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(encoded)));
   }

   /*******************************************************************************
    ** Verify signature in constant time before parsing purpose or identifiers.
    *******************************************************************************/
   public Claims verify(String token, String purpose) throws QUserFacingException
   {
      try
      {
         if(token == null || token.length() > 1024)
         {
            throw invalid();
         }
         String[] parts = token.split("\\.", -1);
         if(parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), Base64.getUrlDecoder().decode(parts[1])))
         {
            throw invalid();
         }
         String[] values = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split(":", -1);
         if(values.length != 5 || !purpose.equals(values[0]))
         {
            throw invalid();
         }
         Integer id = Integer.valueOf(values[1]);
         Integer listId = values[2].isEmpty() ? null : Integer.valueOf(values[2]);
         Instant expiry = Instant.ofEpochSecond(Long.parseLong(values[3]));
         if(id <= 0 || (listId != null && listId <= 0) || ("confirm".equals(purpose) && listId == null) || !expiry.isAfter(clock.instant()))
         {
            throw invalid();
         }
         return (new Claims(purpose, id, listId, expiry));
      }
      catch(IllegalArgumentException e)
      {
         throw invalid();
      }
   }

   /*******************************************************************************
    ** Hash the entire confirmation token before persistence.
    *******************************************************************************/
   public static String hash(String token)
   {
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
      }
      catch(java.security.NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is unavailable", e));
      }
   }

   /*******************************************************************************
    ** Build a link from the host's explicitly configured public origin/path.
    *******************************************************************************/
   public String url(String purpose, String token) throws QUserFacingException
   {
      String base = config.getPublicBaseUrl();
      if(base == null || !base.matches("https?://[^\\s?#]+"))
      {
         throw (new QUserFacingException("CRM links require CrmQBitConfig.withPublicBaseUrl(String)."));
      }
      return (base.replaceAll("/+$", "") + "/qqq/hooks/crm/" + purpose + "/" + token);
   }

   /*******************************************************************************
    ** Compute an authentication code without exposing the secret.
    *******************************************************************************/
   private byte[] sign(String payload) throws QUserFacingException
   {
      try
      {
         Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(config.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
         return (mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
      }
      catch(java.security.GeneralSecurityException e)
      {
         throw (new QUserFacingException("CRM token signing is unavailable."));
      }
   }

   /*******************************************************************************
    ** Deliberately does not reveal the token or validation detail.
    *******************************************************************************/
   private static QUserFacingException invalid()
   {
      return (new QUserFacingException("This link is invalid or has expired. Please request a new link."));
   }

   /*******************************************************************************
    ** Authenticated token payload.
    *******************************************************************************/
   public record Claims(String purpose, Integer id, Integer listId, Instant expiresAt)
   {
   }
}
