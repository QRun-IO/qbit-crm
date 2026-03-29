/*******************************************************************************
 ** Possible value source enum for CRM scorable entity types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM entities that can be scored (Contact).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmScorableEntity implements PossibleValueEnum<Integer>
{
   CONTACT(1, "Contact");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmScorableEntity";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmScorableEntity(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmScorableEntity getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmScorableEntity value : CrmScorableEntity.values())
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
