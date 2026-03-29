/*******************************************************************************
 ** Utility class for null-safe session user ID resolution. Used by processes
 ** and customizers that need the current user ID for audit logging.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Null-safe session utilities for the CRM QBit. Returns "system" when no
 ** session or user is available (e.g., scheduled/system context).
 *******************************************************************************/
public final class CrmSessionUtils
{
   private static final QLogger LOG = QLogger.getLogger(CrmSessionUtils.class);



   /*******************************************************************************
    ** Private constructor -- utility class.
    *******************************************************************************/
   private CrmSessionUtils()
   {
   }



   /*******************************************************************************
    ** Get the current user ID from the QContext session, falling back to "system"
    ** when no session or user is available.
    *******************************************************************************/
   public static String getCurrentUserId()
   {
      try
      {
         if(QContext.getQSession() != null)
         {
            if(QContext.getQSession().getUser() != null
               && StringUtils.hasContent(QContext.getQSession().getUser().getIdReference()))
            {
               return (QContext.getQSession().getUser().getIdReference());
            }

            if(StringUtils.hasContent(QContext.getQSession().getIdReference()))
            {
               return (QContext.getQSession().getIdReference());
            }
         }
      }
      catch(Exception e)
      {
         LOG.debug("Could not resolve current user ID from session", e);
      }

      return ("system");
   }

}
