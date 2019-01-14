/*
 * Copyright 2018 EPAM Systems
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

package com.epam.ta.reportportal.core.launch.impl;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.core.events.MessageBus;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.enums.LaunchModeEnum;
import com.epam.ta.reportportal.entity.enums.StatusEnum;
import com.epam.ta.reportportal.entity.launch.Launch;
import com.epam.ta.reportportal.entity.project.ProjectRole;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;

import static com.epam.ta.reportportal.core.launch.impl.LaunchTestUtil.getLaunch;
import static com.epam.ta.reportportal.core.launch.impl.LaunchTestUtil.getReportPortalUser;
import static com.epam.ta.reportportal.util.ProjectExtractor.extractProjectDetails;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class StartLaunchHandlerTest {

	private LaunchRepository launchRepository = mock(LaunchRepository.class);

	private MessageBus messageBus = mock(MessageBus.class);

	private StartLaunchHandler startLaunchHandler = new StartLaunchHandler(launchRepository, messageBus);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void startLaunch() {
		final ReportPortalUser rpUser = getReportPortalUser("test", UserRole.ADMINISTRATOR, ProjectRole.PROJECT_MANAGER, 1L);

		StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
		startLaunchRQ.setStartTime(new Date());
		startLaunchRQ.setName("test");

		when(launchRepository.save(any(Launch.class))).thenReturn(getLaunch(StatusEnum.IN_PROGRESS, LaunchModeEnum.DEFAULT).get());

		final StartLaunchRS startLaunchRS = startLaunchHandler.startLaunch(rpUser,
				extractProjectDetails(rpUser, "superadmin_personal"),
				startLaunchRQ
		);

		assertNotNull(startLaunchRS);
		assertEquals(1L, (long) startLaunchRS.getId());
		assertEquals(1L, (long) startLaunchRS.getNumber());
	}

	@Test
	public void accessDeniedForCustomerRoleAndDebugMode() {
		thrown.expect(ReportPortalException.class);
		thrown.expectMessage("You do not have enough permissions.");

		final ReportPortalUser rpUser = getReportPortalUser("test", UserRole.USER, ProjectRole.CUSTOMER, 1L);

		StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
		startLaunchRQ.setStartTime(new Date());
		startLaunchRQ.setMode(Mode.DEBUG);

		startLaunchHandler.startLaunch(rpUser, extractProjectDetails(rpUser, "superadmin_personal"), startLaunchRQ);
	}
}