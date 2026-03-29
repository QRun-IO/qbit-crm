/*******************************************************************************
 ** MetaData producer for the CRM Sales Leaderboard widget.
 **
 ** Provides a TABLE view showing deals grouped by owner with total revenue
 ** from closed-won deals, ranked descending.
 *******************************************************************************/
package com.kingsrook.qbits.crm.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** Produces metadata for the crmSalesLeaderboard widget (TABLE type).
 *******************************************************************************/
public class CrmSalesLeaderboardWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmSalesLeaderboard";



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
         .withLabel("Sales Leaderboard")
         .withTooltip("Top performers by closed-won deal revenue")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new QIcon("leaderboard"))
         .withCodeReference(new QCodeReference(CrmSalesLeaderboardRenderer.class));
   }

}
