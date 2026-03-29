/*******************************************************************************
 ** MetaData producer for the CRM Quota Attainment widget.
 **
 ** Provides a MULTI_STATISTICS view showing quota attainment percentage for
 ** the current user and current period.
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
 ** Produces metadata for the crmQuotaAttainment widget (MULTI_STATISTICS type).
 *******************************************************************************/
public class CrmQuotaAttainmentWidgetProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "crmQuotaAttainment";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.MULTI_STATISTICS.getType())
         .withGridColumns(6)
         .withIsCard(true)
         .withLabel("Quota Attainment")
         .withTooltip("Your quota attainment for the current period")
         .withShowReloadButton(true)
         .withIcon("topRightInsideCard", new QIcon("flag"))
         .withCodeReference(new QCodeReference(CrmQuotaAttainmentRenderer.class));
   }

}
