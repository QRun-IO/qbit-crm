/*******************************************************************************
 ** Possible value source enum for CRM list member statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM list member statuses (Subscribed, Unsubscribed, Bounced, Cleaned).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmListMemberStatus implements PossibleValueEnum<Integer>
{
   SUBSCRIBED(1, "Subscribed"),
   UNSUBSCRIBED(2, "Unsubscribed"),
   BOUNCED(3, "Bounced"),
   CLEANED(4, "Cleaned");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmListMemberStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmListMemberStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmListMemberStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmListMemberStatus value : CrmListMemberStatus.values())
      {
         if(Objects.equals(value.id, id))
         {
            return (value);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {
      return (getId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (getLabel());
   }
}
