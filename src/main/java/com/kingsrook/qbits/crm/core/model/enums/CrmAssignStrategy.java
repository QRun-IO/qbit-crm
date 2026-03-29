/*******************************************************************************
 ** Possible value source enum for CRM assignment strategies.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM assignment strategies (Specific User, Round Robin, Least Active).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmAssignStrategy implements PossibleValueEnum<Integer>
{
   SPECIFIC_USER(1, "Specific User"),
   ROUND_ROBIN(2, "Round Robin"),
   LEAST_ACTIVE(3, "Least Active");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmAssignStrategy";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmAssignStrategy(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmAssignStrategy getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmAssignStrategy value : CrmAssignStrategy.values())
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
