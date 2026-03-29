/*******************************************************************************
 ** Possible value source enum for CRM direction (inbound/outbound).
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM directions (Inbound, Outbound).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmDirection implements PossibleValueEnum<Integer>
{
   INBOUND(1, "Inbound"),
   OUTBOUND(2, "Outbound");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmDirection";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmDirection(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmDirection getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmDirection value : CrmDirection.values())
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
