/*******************************************************************************
 ** Possible value source enum for CRM legal basis types (GDPR).
 *******************************************************************************/
package com.kingsrook.qbits.crm.core.model.enums;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Enum of CRM legal basis types for data processing consent.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum CrmLegalBasis implements PossibleValueEnum<Integer>
{
   CONSENT(1, "Consent"),
   LEGITIMATE_INTEREST(2, "Legitimate Interest"),
   CONTRACT(3, "Contract"),
   LEGAL_OBLIGATION(4, "Legal Obligation");

   private final Integer id;
   private final String  label;

   public static final String NAME = "CrmLegalBasis";



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   CrmLegalBasis(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    *******************************************************************************/
   public static CrmLegalBasis getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(CrmLegalBasis value : CrmLegalBasis.values())
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
