/*******************************************************************************
 ** Possible value source enum for CRM email statuses.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM email statuses (Draft, Sent, Delivered, Failed).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmEmailStatus implements PossibleValueEnum<Integer>
{
   DRAFT(1, "Draft"),
   SENT(2, "Sent"),
   DELIVERED(3, "Delivered"),
   FAILED(4, "Failed");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmEmailStatus";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmEmailStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmEmailStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmEmailStatus value : CrmEmailStatus.values())
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
