/*
 * Copyright (C) 2018 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.ta.reportportal.ws.converter.converters;

import com.epam.ta.reportportal.entity.launch.Launch;
import com.epam.ta.reportportal.entity.launch.LaunchTag;
import com.epam.ta.reportportal.entity.project.email.EmailSenderCase;
import com.epam.ta.reportportal.ws.model.project.email.EmailSenderCaseDTO;
import com.google.common.base.Preconditions;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts internal DB model from/to DTO
 *
 * @author Pavel Bortnik
 */
public final class EmailConfigConverters {

	private EmailConfigConverters() {
		//static only
	}

	public final static Function<EmailSenderCase, EmailSenderCaseDTO> TO_CASE_RESOURCE = model -> {
		Preconditions.checkNotNull(model);
		EmailSenderCaseDTO resource = new EmailSenderCaseDTO();
		resource.setLaunchNames(model.getLaunches().stream().map(Launch::getName).collect(Collectors.toList()));
		resource.setTags(model.getTags().stream().map(LaunchTag::getValue).collect(Collectors.toList()));
		resource.setSendCase(model.getSendCase().getCaseString());
		resource.setRecipients(model.getRecipients());
		return resource;
	};
}