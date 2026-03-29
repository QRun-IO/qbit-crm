/*******************************************************************************
 ** Possible value source enum for CRM goal periods.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM goal periods (Monthly, Quarterly, Annual).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmGoalPeriod implements PossibleValueEnum<Integer>
{
   MONTHLY(1, "Monthly"),
   QUARTERLY(2, "Quarterly"),
   ANNUAL(3, "Annual");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmGoalPeriod";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmGoalPeriod(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmGoalPeriod getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmGoalPeriod value : CrmGoalPeriod.values())
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
