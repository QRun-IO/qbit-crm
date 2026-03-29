/*******************************************************************************
 ** Possible value source enum for CRM assignable entity types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM entities that support ownership assignment (Contact, Deal).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmAssignableEntity implements PossibleValueEnum<Integer>
{
   CONTACT(1, "Contact"),
   DEAL(2, "Deal");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmAssignableEntity";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmAssignableEntity(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmAssignableEntity getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmAssignableEntity value : CrmAssignableEntity.values())
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
