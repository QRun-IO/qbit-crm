/*******************************************************************************
 ** Possible value source enum for CRM activity participant roles.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM activity participant roles (Organizer, Attendee, Optional).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmParticipantRole implements PossibleValueEnum<Integer>
{
   ORGANIZER(1, "Organizer"),
   ATTENDEE(2, "Attendee"),
   OPTIONAL(3, "Optional");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmParticipantRole";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmParticipantRole(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmParticipantRole getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmParticipantRole value : CrmParticipantRole.values())
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
