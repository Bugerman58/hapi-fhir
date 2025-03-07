/*-
 * #%L
 * HAPI FHIR - Master Data Management
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.mdm.svc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.subscription.channel.api.ChannelProducerSettings;
import ca.uhn.fhir.jpa.subscription.channel.api.IChannelFactory;
import ca.uhn.fhir.jpa.subscription.model.ResourceModifiedJsonMessage;
import ca.uhn.fhir.jpa.subscription.model.ResourceModifiedMessage;
import ca.uhn.fhir.mdm.api.IMdmChannelSubmitterSvc;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.rest.api.Constants;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;

import static ca.uhn.fhir.mdm.api.IMdmSettings.EMPI_CHANNEL_NAME;

/**
 * This class is responsible for manual submissions of {@link IAnyResource} resources onto the MDM Channel.
 */
public class MdmChannelSubmitterSvcImpl implements IMdmChannelSubmitterSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	private MessageChannel myMdmChannelProducer;

	private final FhirContext myFhirContext;

	private final IChannelFactory myChannelFactory;

	@Autowired
	private IInterceptorBroadcaster myInterceptorBroadcaster;

	@Override
	public void submitResourceToMdmChannel(IBaseResource theResource) {
		ResourceModifiedJsonMessage resourceModifiedJsonMessage = new ResourceModifiedJsonMessage();
		ResourceModifiedMessage resourceModifiedMessage = new ResourceModifiedMessage(
				myFhirContext,
				theResource,
				ResourceModifiedMessage.OperationTypeEnum.MANUALLY_TRIGGERED,
				null,
				(RequestPartitionId) theResource.getUserData(Constants.RESOURCE_PARTITION_ID));
		resourceModifiedMessage.setOperationType(ResourceModifiedMessage.OperationTypeEnum.MANUALLY_TRIGGERED);
		resourceModifiedJsonMessage.setPayload(resourceModifiedMessage);
		if (myInterceptorBroadcaster.hasHooks(Pointcut.MDM_SUBMIT_PRE_MESSAGE_DELIVERY)) {
			final HookParams params =
					new HookParams().add(ResourceModifiedJsonMessage.class, resourceModifiedJsonMessage);
			myInterceptorBroadcaster.callHooks(Pointcut.MDM_SUBMIT_PRE_MESSAGE_DELIVERY, params);
		}
		boolean success = getMdmChannelProducer().send(resourceModifiedJsonMessage);
		if (!success) {
			ourLog.error("Failed to submit {} to MDM Channel.", resourceModifiedMessage.getPayloadId());
		}
	}

	@Autowired
	public MdmChannelSubmitterSvcImpl(FhirContext theFhirContext, IChannelFactory theIChannelFactory) {
		myFhirContext = theFhirContext;
		myChannelFactory = theIChannelFactory;
	}

	protected ChannelProducerSettings getChannelProducerSettings() {
		return new ChannelProducerSettings();
	}

	private void init() {
		ChannelProducerSettings channelSettings = getChannelProducerSettings();
		myMdmChannelProducer = myChannelFactory.getOrCreateProducer(
				EMPI_CHANNEL_NAME, ResourceModifiedJsonMessage.class, channelSettings);
	}

	private MessageChannel getMdmChannelProducer() {
		if (myMdmChannelProducer == null) {
			init();
		}
		return myMdmChannelProducer;
	}
}
