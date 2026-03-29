/*******************************************************************************
 ** MetaData producer for the CRM Rotting Deals widget.
 **
 ** Provides a TABLE view showing deals that have exceeded their current
 ** stage's rotDays threshold, indicating they need attention.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmRottingDeals widget (TABLE type).
 *******************************************************************************/
public class CrmRottingDealsWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmRottingDeals";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.TABLE.getType())
         .withGridColumns(6)
         .withIsCard(true)
         .withLabel("Rotting Deals")
         .withTooltip("Deals that have exceeded their stage's rot days threshold")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("warning"))
         .withCodeReference(new QCodeReference(CrmRottingDealsRenderer.class));
   }

}
