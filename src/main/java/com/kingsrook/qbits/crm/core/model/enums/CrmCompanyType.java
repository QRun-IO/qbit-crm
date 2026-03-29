/*******************************************************************************
 ** Possible value source enum for CRM company types.
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM company types (Prospect, Customer, Partner, Competitor, Other).
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmCompanyType implements PossibleValueEnum<Integer>
{
   PROSPECT(1, "Prospect"),
   CUSTOMER(2, "Customer"),
   PARTNER(3, "Partner"),
   COMPETITOR(4, "Competitor"),
   OTHER(5, "Other");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmCompanyType";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmCompanyType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmCompanyType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmCompanyType value : CrmCompanyType.values())
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
