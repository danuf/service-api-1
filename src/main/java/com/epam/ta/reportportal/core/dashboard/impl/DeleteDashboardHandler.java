/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.ta.reportportal.core.dashboard.impl;

import com.epam.ta.reportportal.commons.Predicates;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.core.acl.AclUtils;
import com.epam.ta.reportportal.core.dashboard.IDeleteDashboardHandler;
import com.epam.ta.reportportal.database.dao.DashboardRepository;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.entity.Dashboard;
import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.database.entity.ProjectRole;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link IDeleteDashboardHandler}
 *
 * @author Aliaksei_Makayed
 */
@Service
public class DeleteDashboardHandler implements IDeleteDashboardHandler {

    private final DashboardRepository dashboardRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public DeleteDashboardHandler(DashboardRepository dashboardRepository, ProjectRepository projectRepository) {
        this.dashboardRepository = dashboardRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public OperationCompletionRS deleteDashboard(String dashboardId, String userName, String projectName) {

        Dashboard dashboard = dashboardRepository.findOne(dashboardId);

        BusinessRule.expect(dashboard, Predicates.notNull()).verify(ErrorType.DASHBOARD_NOT_FOUND, dashboardId);

        final List<Project> userProjects = projectRepository.findUserProjects(userName);
        Map<String, ProjectRole> roles = userProjects
                .stream().collect(Collectors.toMap(Project::getName, p -> p.getUsers().get(userName).getProjectRole()));

        AclUtils.isAllowedToEdit(dashboard.getAcl(), userName, roles, dashboard.getName());

        BusinessRule.expect(dashboard.getProjectName(), Predicates.equalTo(projectName))
                .verify(ErrorType.ACCESS_DENIED);

        try {
            dashboardRepository.delete(dashboardId);
        } catch (Exception e) {
            throw new ReportPortalException("Error during deleting dashboard item", e);
        }
        OperationCompletionRS response = new OperationCompletionRS();
        StringBuilder msg = new StringBuilder("Dashboard with ID = '");
        msg.append(dashboardId);
        msg.append("' successfully deleted.");
        response.setResultMessage(msg.toString());

        return response;
    }
}
