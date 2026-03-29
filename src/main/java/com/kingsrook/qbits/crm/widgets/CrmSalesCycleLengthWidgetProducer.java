/*******************************************************************************
 ** MetaData producer for the Sales Cycle Length widget.
 **
 ** Shows the average number of days from deal creation to close for won deals.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmSalesCycleLength widget (TABLE type).
 *******************************************************************************/
public class CrmSalesCycleLengthWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmSalesCycleLength";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.TABLE.getType())
         .withGridColumns(3)
         .withIsCard(true)
         .withLabel("Sales Cycle Length")
         .withTooltip("Average days from creation to close for won deals")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("schedule"))
         .withCodeReference(new QCodeReference(CrmSalesCycleLengthRenderer.class));
   }

}
