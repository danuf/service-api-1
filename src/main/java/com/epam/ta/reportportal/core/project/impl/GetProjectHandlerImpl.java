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

package com.epam.ta.reportportal.core.project.impl;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.querygen.Condition;
import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.commons.querygen.FilterCondition;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.commons.validation.Suppliers;
import com.epam.ta.reportportal.core.project.GetProjectHandler;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.converter.PagedResourcesAssembler;
import com.epam.ta.reportportal.ws.converter.converters.ProjectConverter;
import com.epam.ta.reportportal.ws.converter.converters.UserConverter;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.project.ProjectResource;
import com.epam.ta.reportportal.ws.model.user.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.epam.ta.reportportal.commons.querygen.constant.UserCriteriaConstant.CRITERIA_PROJECT_ID;

/**
 * @author Pavel Bortnik
 */
@Service
public class GetProjectHandlerImpl implements GetProjectHandler {

	private final ProjectRepository projectRepository;

	private final UserRepository userRepository;

	@Autowired
	public GetProjectHandlerImpl(ProjectRepository projectRepository, UserRepository userRepository) {
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
	}

	@Override
	public Iterable<UserResource> getProjectUsers(ReportPortalUser.ProjectDetails projectDetails, Filter filter, Pageable pageable) {
		Project project = projectRepository.findById(projectDetails.getProjectId())
				.orElseThrow(() -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, projectDetails.getProjectId()));
		if (CollectionUtils.isEmpty(project.getUsers())) {
			return Collections.emptyList();
		}
		filter.withCondition(new FilterCondition(Condition.EQUALS,
				false,
				String.valueOf(projectDetails.getProjectId()),
				CRITERIA_PROJECT_ID
		));

		Page<User> users = userRepository.findByFilterExcluding(filter, pageable, "email");
		return PagedResourcesAssembler.pageConverter(UserConverter.TO_RESOURCE).apply(users);
	}

	@Override
	public ProjectResource getProject(String projectName) {
		return ProjectConverter.TO_PROJECT_RESOURCE.apply(projectRepository.findByName(projectName)
				.orElseThrow(() -> new ReportPortalException(ErrorType.PROJECT_NOT_FOUND, projectName)));
	}

	@Override
	public List<String> getUserNames(ReportPortalUser.ProjectDetails projectDetails, String value) {
		BusinessRule.expect(value.length() > 2, Predicates.equalTo(true)).verify(ErrorType.INCORRECT_FILTER_PARAMETERS,
				Suppliers.formattedSupplier("Length of the filtering string '{}' is less than 3 symbols", value)
		);
		return userRepository.findNamesByProject(projectDetails.getProjectId(), value);
	}

	@Override
	public Iterable<UserResource> getUserNames(String value, Pageable pageable) {
		BusinessRule.expect(value.length() >= 1, Predicates.equalTo(true)).verify(
				ErrorType.INCORRECT_FILTER_PARAMETERS,
				Suppliers.formattedSupplier("Length of the filtering string '{}' is less than 1 symbol", value)
		);
		return PagedResourcesAssembler.pageConverter(UserConverter.TO_RESOURCE).apply(userRepository.searchForUser(value, pageable));
	}

	@Override
	public OperationCompletionRS isProjectsAvailable() {
		return null;
	}

	@Override
	public List<String> getAllProjectNames() {
		return projectRepository.findAllProjectNames();
	}

	@Override
	public Map<String, Boolean> getAnalyzerIndexingStatus() {
		return null;
	}
}