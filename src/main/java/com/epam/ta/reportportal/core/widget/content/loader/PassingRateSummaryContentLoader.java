package com.epam.ta.reportportal.core.widget.content.loader;

import com.epam.ta.reportportal.commons.querygen.Filter;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.core.widget.content.LoadContentStrategy;
import com.epam.ta.reportportal.dao.WidgetContentRepository;
import com.epam.ta.reportportal.entity.widget.WidgetOption;
import com.epam.ta.reportportal.entity.widget.content.PassStatisticsResult;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.epam.ta.reportportal.commons.Predicates.notNull;
import static com.epam.ta.reportportal.core.widget.content.WidgetContentUtils.GROUP_CONTENT_FIELDS;
import static java.util.Collections.singletonMap;

/**
 * @author Ivan Budayeu
 */
@Service
public class PassingRateSummaryContentLoader implements LoadContentStrategy {

	@Autowired
	private WidgetContentRepository widgetContentRepository;

	@Override
	public Map<String, ?> loadContent(List<String> contentFields, Filter filter, Set<WidgetOption> widgetOptions, int limit) {

		Map<String, List<String>> fields = GROUP_CONTENT_FIELDS.apply(contentFields);
		validateContentFields(fields);

		PassStatisticsResult result = widgetContentRepository.summaryPassStatistics(filter, fields);
		return singletonMap(RESULT, result);
	}

	/**
	 * Validate provided content fields.
	 *
	 * @param contentFields Map of provided content.
	 */
	private void validateContentFields(Map<String, List<String>> contentFields) {
		BusinessRule.expect(contentFields, notNull()).verify(ErrorType.BAD_REQUEST_ERROR, "Content fields should not be null.");
	}
}
