/*******************************************************************************
 ** Utility class for CRM currency conversion. Queries the Currency table for
 ** the exchange rate and converts an amount to the base currency.
 *******************************************************************************/
package com.kingsrook.qbits.crm.sync;


import java.math.BigDecimal;
import java.math.RoundingMode;
import com.kingsrook.qbits.crm.core.model.Currency;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;


/*******************************************************************************
 ** Converts monetary amounts to the base currency using exchange rates from
 ** the Currency table. Returns the amount unchanged if no currency code is
 ** provided or if the currency is not found.
 *******************************************************************************/
public final class CrmCurrencyUtils
{

   /*******************************************************************************
    ** Private constructor -- utility class.
    *******************************************************************************/
   private CrmCurrencyUtils()
   {
   }



   /*******************************************************************************
    ** Convert an amount to base currency using the exchange rate for the given
    ** currency code. Returns null if amount is null. Returns the amount unchanged
    ** if currencyCode is null/empty or not found in the Currency table.
    *******************************************************************************/
   public static BigDecimal convertToBaseCurrency(BigDecimal amount, String currencyCode) throws QException
   {
      if(amount == null)
      {
         return (null);
      }

      if(currencyCode == null || currencyCode.isBlank())
      {
         return (amount);
      }

      QueryOutput queryOutput = new QueryAction().execute(
         new QueryInput(Currency.TABLE_NAME)
            .withFilter(new QQueryFilter()
               .withCriteria(new QFilterCriteria("currencyCode", QCriteriaOperator.EQUALS, currencyCode))));

      if(queryOutput.getRecords().isEmpty())
      {
         return (amount);
      }

      Currency currency = new Currency(queryOutput.getRecords().get(0));
      BigDecimal exchangeRate = currency.getExchangeRateToBase();
      if(exchangeRate == null)
      {
         exchangeRate = BigDecimal.ONE;
      }

      return (amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
   }

}
