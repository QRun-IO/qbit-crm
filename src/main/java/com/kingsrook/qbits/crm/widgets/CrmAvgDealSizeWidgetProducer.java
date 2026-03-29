/*******************************************************************************
 ** MetaData producer for the Average Deal Size widget.
 **
 ** Shows the average amount of closed-won deals.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmAvgDealSize widget (TABLE type).
 *******************************************************************************/
public class CrmAvgDealSizeWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmAvgDealSize";



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
         .withLabel("Average Deal Size")
         .withTooltip("Average amount of closed-won deals")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("bar_chart"))
         .withCodeReference(new QCodeReference(CrmAvgDealSizeRenderer.class));
   }

}
