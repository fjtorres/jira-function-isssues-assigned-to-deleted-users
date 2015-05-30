package es.fjtorres.jira.function.issuesAssignedToDeletedUsers;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

public class IssuesAssignedToDeletedUsers extends AbstractJqlFunction {

	private static final String MESSAGES_LIMIT_NUMBER = "issues-with-deleted-assignee-function.error.limit.integer";
	private static final String MESSAGES_LIMIT_MAX_VALUE = "issues-with-deleted-assignee-function.error.limit.maxValue";
	private static final String MESSAGES_LIMIT_MIN_VALUE = "issues-with-deleted-assignee-function.error.limit.minValue";

	public static final int MINIMUM_PARAMETERS = 0;
	public static final int MAX_PARAMETERS = 1;

	public static final int MAX_LIMIT_VALUE = 500;
	public static final int MIN_LIMIT_VALUE = 1;
	private static final int DEFAULT_LIMIT_VALUE = 100;

	private final SearchService searchService;

	public IssuesAssignedToDeletedUsers(final SearchService pSearchService) {
		this.searchService = pSearchService;
	}

	public MessageSet validate(final User searcher,
			final FunctionOperand operand, final TerminalClause terminalClause) {
		final List<String> args = operand.getArgs();
		MessageSet messages = null;
		if (CollectionUtils.isEmpty(args)) {
			messages = validateNumberOfArgs(operand, MINIMUM_PARAMETERS);
		} else {
			messages = validateNumberOfArgs(operand, MAX_PARAMETERS);

			if (!messages.hasAnyErrors()) {
				final String strLimit = args.get(0);
				int issueLimit = 0;
				try {
					issueLimit = Integer.parseInt(strLimit);

					if (issueLimit > MAX_LIMIT_VALUE) {
						messages.addErrorMessage(getI18n().getText(
								MESSAGES_LIMIT_MAX_VALUE, MAX_LIMIT_VALUE));
					} else if (issueLimit < MIN_LIMIT_VALUE) {
						messages.addErrorMessage(getI18n().getText(
								MESSAGES_LIMIT_MIN_VALUE, MIN_LIMIT_VALUE));
					}
				} catch (final NumberFormatException e) {
					messages.addErrorMessage(getI18n().getText(
							MESSAGES_LIMIT_NUMBER));
				}
			}
		}
		return messages;
	}

	public List<QueryLiteral> getValues(
			final QueryCreationContext queryCreationContext,
			final FunctionOperand operand, TerminalClause terminalClause) {

		notNull("queryCreationContext", queryCreationContext);

		int issueLimit = DEFAULT_LIMIT_VALUE;

		if (CollectionUtils.isNotEmpty(operand.getArgs())) {
			final String strLimit = operand.getArgs().get(0);
			issueLimit = Integer.parseInt(strLimit);
		}

		final List<Issue> allIssues = findAllIssues(
				queryCreationContext.getUser(), issueLimit);

		final List<Issue> issuesWithDeletedAssigneeUser = new ArrayList<Issue>();

		for (final Issue issue : allIssues) {
			final User assignee = issue.getAssigneeUser();

			if (assignee == null
					&& StringUtils.isNotBlank(issue.getAssigneeId())) {
				issuesWithDeletedAssigneeUser.add(issue);
			}
		}

		final List<QueryLiteral> result = new ArrayList<QueryLiteral>();
		if (!issuesWithDeletedAssigneeUser.isEmpty()) {
			for (final Issue issue : issuesWithDeletedAssigneeUser) {
				result.add(new QueryLiteral(operand, issue.getKey()));
			}

		}

		return result;
	}

	public int getMinimumNumberOfExpectedArguments() {
		return MINIMUM_PARAMETERS;
	}

	public JiraDataType getDataType() {
		return JiraDataTypes.ISSUE;
	}

	private List<Issue> findAllIssues(final User searcher, final int limit) {
		List<Issue> result = Collections.emptyList();
		final ParseResult parseQuery = searchService.parseQuery(searcher, "");

		if (parseQuery.isValid()) {
			try {
				final SearchResults searchResult = searchService.search(
						searcher, parseQuery.getQuery(),
						PagerFilter.getUnlimitedFilter());
				result = searchResult.getIssues();
			} catch (final SearchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
}
