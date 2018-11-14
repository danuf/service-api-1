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

package com.epam.ta.reportportal.ws.controller;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.commons.querygen.Condition;
import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.commons.querygen.FilterCondition;
import com.epam.ta.reportportal.core.item.*;
import com.epam.ta.reportportal.core.item.history.TestItemsHistoryHandler;
import com.epam.ta.reportportal.dao.TestItemRepository;
import com.epam.ta.reportportal.entity.item.TestItem;
import com.epam.ta.reportportal.ws.converter.PagedResourcesAssembler;
import com.epam.ta.reportportal.ws.converter.TestItemResourceAssembler;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.issue.DefineIssueRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.item.LinkExternalIssueRQ;
import com.epam.ta.reportportal.ws.model.item.MergeTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.UnlinkExternalIssueRq;
import com.epam.ta.reportportal.ws.model.item.UpdateTestItemRQ;
import com.epam.ta.reportportal.ws.resolver.FilterFor;
import com.epam.ta.reportportal.ws.resolver.SortFor;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.epam.ta.reportportal.auth.permissions.Permissions.ALLOWED_TO_REPORT;
import static com.epam.ta.reportportal.commons.querygen.constant.TestItemCriteriaConstant.CRITERIA_NAME;
import static com.epam.ta.reportportal.util.ProjectExtractor.extractProjectDetails;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/{projectName}/item")
//@PreAuthorize(ASSIGNED_TO_PROJECT)
public class TestItemController {

	private final StartTestItemHandler startTestItemHandler;
	private final DeleteTestItemHandler deleteTestItemHandler;
	private final FinishTestItemHandler finishTestItemHandler;
	private final UpdateTestItemHandler updateTestItemHandler;
	private final GetTestItemHandler getTestItemHandler;
	private final TestItemsHistoryHandler testItemsHistoryHandler;
	private final MergeTestItemHandler mergeTestItemHandler;

	@Autowired
	public TestItemController(StartTestItemHandler startTestItemHandler, DeleteTestItemHandler deleteTestItemHandler,
			FinishTestItemHandler finishTestItemHandler, UpdateTestItemHandler updateTestItemHandler, GetTestItemHandler getTestItemHandler,
			TestItemsHistoryHandler testItemsHistoryHandler, MergeTestItemHandler mergeTestItemHandler) {
		this.startTestItemHandler = startTestItemHandler;
		this.deleteTestItemHandler = deleteTestItemHandler;
		this.finishTestItemHandler = finishTestItemHandler;
		this.updateTestItemHandler = updateTestItemHandler;
		this.getTestItemHandler = getTestItemHandler;
		this.testItemsHistoryHandler = testItemsHistoryHandler;
		this.mergeTestItemHandler = mergeTestItemHandler;
	}

	@Transactional
	@PostMapping
	@ResponseStatus(CREATED)
	@ApiOperation("Start a root test item")
	@PreAuthorize(ALLOWED_TO_REPORT)
	public EntryCreatedRS startRootItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestBody @Validated StartTestItemRQ startTestItemRQ) {
		return startTestItemHandler.startRootItem(user, extractProjectDetails(user, projectName), startTestItemRQ);
	}

	@Transactional(readOnly = true)
	@GetMapping("/{itemId}")
	@ResponseStatus(OK)
	@ApiOperation("Find test item by ID")
	public TestItemResource getTestItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long itemId) {
		return getTestItemHandler.getTestItem(itemId, extractProjectDetails(user, projectName), user);

	}

	@Transactional
	@PostMapping("/{parentItem}")
	@ResponseStatus(CREATED)
	@ApiOperation("Start a child test item")
	@PreAuthorize(ALLOWED_TO_REPORT)
	public EntryCreatedRS startChildItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long parentItem, @RequestBody @Validated StartTestItemRQ startTestItemRQ) {
		return startTestItemHandler.startChildItem(user, extractProjectDetails(user, projectName), startTestItemRQ, parentItem);
	}

	@Transactional
	@PutMapping("/{testItemId}")
	@ResponseStatus(OK)
	@ApiOperation("Finish test item")
	@PreAuthorize(ALLOWED_TO_REPORT)
	public OperationCompletionRS finishTestItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long testItemId, @RequestBody @Validated FinishTestItemRQ finishExecutionRQ) {
		return finishTestItemHandler.finishTestItem(user, extractProjectDetails(user, projectName), testItemId, finishExecutionRQ);
	}

	//TODO check pre-defined filter
	@Transactional(readOnly = true)
	@GetMapping
	@ResponseStatus(OK)
	@ApiOperation("Find test items by specified filter")
	public Iterable<TestItemResource> getTestItems(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@FilterFor(TestItem.class) Filter filter, @FilterFor(TestItem.class) Filter predefinedFilter,
			@SortFor(TestItem.class) Pageable pageable) {
		return getTestItemHandler.getTestItems(new Filter(filter, predefinedFilter),
				pageable,
				extractProjectDetails(user, projectName),
				user
		);
	}

	@Autowired
	private TestItemRepository testItemRepository;
	@Autowired
	private TestItemResourceAssembler itemResourceAssembler;

	@Transactional(readOnly = true)
	@GetMapping("/test")
	@ResponseStatus(OK)
	public Iterable<TestItem> getTestItems() {
		Filter filter = new Filter(TestItem.class, Sets.newHashSet(new FilterCondition(Condition.CONTAINS, false, "Step", CRITERIA_NAME)));
		PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "item_id"));
		return testItemRepository.findByFilter(filter, pageRequest);
	}

	@Transactional(readOnly = true)
	@GetMapping("/hiber")
	@ResponseStatus(OK)
	public Iterable<TestItemResource> testItemsHiber() {
		Page<TestItem> page = testItemRepository.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "itemId")));
		return PagedResourcesAssembler.pageConverter(itemResourceAssembler::toResource).apply(page);
	}

	@Transactional
	@DeleteMapping("/{itemId}")
	@ResponseStatus(OK)
	@ApiOperation("Delete test item")
	public OperationCompletionRS deleteTestItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long itemId) {
		return deleteTestItemHandler.deleteTestItem(itemId, extractProjectDetails(user, projectName), user);
	}

	@Transactional
	@DeleteMapping
	@ResponseStatus(OK)
	@ApiOperation("Delete test items by specified ids")
	public List<OperationCompletionRS> deleteTestItems(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestParam(value = "ids") Long[] ids) {
		return deleteTestItemHandler.deleteTestItem(ids, extractProjectDetails(user, projectName), user);
	}

	@Transactional
	@PutMapping
	@ResponseStatus(OK)
	@ApiOperation("Update issues of specified test items")
	public List<Issue> defineTestItemIssueType(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestBody @Validated DefineIssueRQ request) {
		return updateTestItemHandler.defineTestItemsIssues(extractProjectDetails(user, projectName), request, user);
	}

	@Transactional(readOnly = true)
	@GetMapping("/history")
	@ResponseStatus(OK)
	@ApiOperation("Load history of test items")
	public List<TestItemHistoryElement> getItemsHistory(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestParam(value = "history_depth", required = false, defaultValue = "5") int historyDepth,
			@RequestParam(value = "ids") Long[] ids,
			@RequestParam(value = "is_full", required = false, defaultValue = "false") boolean showBrokenLaunches) {
		return testItemsHistoryHandler.getItemsHistory(extractProjectDetails(user, projectName), ids, historyDepth, showBrokenLaunches);
	}

	@Transactional(readOnly = true)
	@GetMapping("/tags")
	@ResponseStatus(OK)
	@ApiOperation("Get all unique tags of specified launch")
	public List<String> getAllTags(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestParam(value = "launch") Long id, @RequestParam(value = "filter." + "cnt." + "tags") String value) {
		return getTestItemHandler.getTags(id, value, extractProjectDetails(user, projectName), user);
	}

	@Transactional
	@PutMapping("/{itemId}/update")
	@ResponseStatus(OK)
	@ApiOperation("Update test item")
	public OperationCompletionRS updateTestItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long itemId, @RequestBody @Validated UpdateTestItemRQ rq) {
		return updateTestItemHandler.updateTestItem(extractProjectDetails(user, projectName), itemId, rq, user);
	}

	@Transactional
	@PutMapping("/issue/link")
	@ResponseStatus(OK)
	@ApiOperation("Attach external issue for specified test items")
	public List<OperationCompletionRS> addExternalIssues(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestBody @Validated LinkExternalIssueRQ rq) {
		return updateTestItemHandler.linkExternalIssues(extractProjectDetails(user, projectName), rq, user);
	}

	@Transactional
	@PutMapping("/issue/unlink")
	@ResponseStatus(OK)
	@ApiOperation("Unlink external issue for specified test items")
	public List<OperationCompletionRS> unlinkExternalIssues(@PathVariable String projectName,
			@AuthenticationPrincipal ReportPortalUser user, @RequestBody @Validated UnlinkExternalIssueRq rq) {
		return updateTestItemHandler.unlinkExternalIssues(extractProjectDetails(user, projectName), rq, user);
	}

	@Transactional(readOnly = true)
	@GetMapping("/items")
	@ResponseStatus(OK)
	@ApiOperation("Get test items by specified ids")
	public List<TestItemResource> getTestItems(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@RequestParam(value = "ids") Long[] ids) {
		return getTestItemHandler.getTestItems(ids, extractProjectDetails(user, projectName), user);
	}

	@Transactional
	@PutMapping("/{item}/merge")
	@ResponseStatus(OK)
	@ApiOperation("Merge test item")
	public OperationCompletionRS mergeTestItem(@PathVariable String projectName, @AuthenticationPrincipal ReportPortalUser user,
			@PathVariable Long item, @RequestBody @Validated MergeTestItemRQ rq) {
		return mergeTestItemHandler.mergeTestItem(extractProjectDetails(user, projectName), item, rq, user.getUsername());
	}
}
