/*******************************************************************************
 ** MetaData producer for the Win Rate widget.
 **
 ** Shows won/lost counts and win rate percentage grouped by month.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Produces metadata for the crmWinRate widget (TABLE type).
 *******************************************************************************/
public class CrmWinRateWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmWinRate";



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
         .withLabel("Win Rate")
         .withTooltip("Monthly win/loss counts and win rate")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon("analytics"))
         .withCodeReference(new QCodeReference(CrmWinRateRenderer.class));
   }

}
