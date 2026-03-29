/*******************************************************************************
 ** Possible value source enum for CRM activity priority levels.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM activity priority levels (Low, Medium, High).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmActivityPriority implements PossibleValueEnum<Integer>
{
   LOW(1, "Low"),
   MEDIUM(2, "Medium"),
   HIGH(3, "High");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmActivityPriority";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmActivityPriority(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmActivityPriority getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmActivityPriority value : CrmActivityPriority.values())
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
