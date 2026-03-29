/*******************************************************************************
 ** Base test class for the CRM QBit. Sets up QInstance with all CRM entities
 ** using MemoryRecordStore and anonymous auth for testing.
 *******************************************************************************/
package com.kingsrook.qbits.crm;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Base test for CRM QBit tests. Initializes QContext with a full QInstance
 ** containing all CRM metadata before each test.
 *******************************************************************************/
public class BaseTest
{
   public static final String BACKEND_NAME    = "memory";
   public static final String TEST_USER_ID    = "test-user-1";



   /*******************************************************************************
    ** Set up QContext with a validated QInstance before each test.
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach() throws Exception
   {
      QInstance qInstance = defineQInstance();
      new QInstanceValidator().validate(qInstance);

      QSession session = new QSession();
      session.setIdReference(TEST_USER_ID);
      QContext.init(qInstance, session);

      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Clear QContext and reset MemoryRecordStore after each test.
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      QContext.clear();
      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Define the QInstance with the CRM QBit configured for in-memory testing.
    *******************************************************************************/
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = new QInstance();

      qInstance.setAuthentication(new QAuthenticationMetaData()
         .withType(QAuthenticationType.FULLY_ANONYMOUS));

      qInstance.addBackend(new QBackendMetaData()
         .withName(BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class));

      CrmQBitConfig config = new CrmQBitConfig()
         .withDefaultBackendNameForTables(BACKEND_NAME);

      MetaDataProducerMultiOutput output = new CrmQBitProducer()
         .withQBitConfig(config)
         .produce(qInstance);
      output.addSelfToInstance(qInstance);

      return (qInstance);
   }



   /*******************************************************************************
    ** Verify the QBit produces metadata and registers with the QInstance.
    *******************************************************************************/
   @Test
   void testQBitProduces() throws QException
   {
      assertThat(QContext.getQInstance().getQBits()).isNotEmpty();
      assertThat(QContext.getQInstance().getQBits()).containsKey(
         CrmQBitProducer.GROUP_ID + ":" + CrmQBitProducer.ARTIFACT_ID);
   }

}
