package es.fjtorres.jira.function.issuesAssignedToDeletedUsers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.JqlFunctionModuleDescriptor;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

/**
 * Unit test for {@link IssuesAssignedToDeletedUsers} class.
 * 
 * @author fjtorres
 *
 */
public class IssuesAssignedToDeletedUsersTest {

	/**
	 * Instance for test.
	 */
	private IssuesAssignedToDeletedUsers testInstance;

	@Mock
	private SearchService mockSearchService;

	@Mock
	private QueryCreationContext moQueryCreationContext;

	@Mock
	private FunctionOperand mockFunctionOperand;

	@Mock
	private TerminalClause mockTerminalClause;

	@Mock
	private User mockUser;

	@BeforeTest
	public void beforeTest() {
		MockitoAnnotations.initMocks(this);
		testInstance = new IssuesAssignedToDeletedUsers(mockSearchService);

		final I18nBean i18nBean = mock(I18nBean.class);
		final JqlFunctionModuleDescriptor descriptor = mock(JqlFunctionModuleDescriptor.class);
		testInstance.init(descriptor);

		when(i18nBean.getText(anyString())).thenReturn("TEST_MESSAGE");
		when(i18nBean.getText(anyString(), any())).thenReturn("TEST_MESSAGE");
		when(
				i18nBean.getText(anyString(), anyString(), anyString(),
						anyString())).thenReturn("TEST_MESSAGE");

		when(descriptor.getI18nBean()).thenReturn(i18nBean);
	}

	@AfterTest
	public void afterTest() {
		Mockito.validateMockitoUsage();
	}

	@Test
	public void dataTypeTest() {
		Assert.assertEquals(testInstance.getDataType(), JiraDataTypes.ISSUE,
				"Function data type must be ISSUE.");
	}

	@Test
	public void validateWithoutParamTest() {
		final MessageSet result = validateTest(new ArrayList<String>());
		Assert.assertFalse(result.hasAnyErrors(),
				"Function doesn't required any arguments.");
	}

	@Test
	public void validateWithOneParamTest() {
		final MessageSet result = validateTest(Arrays.asList("100"));
		Assert.assertFalse(result.hasAnyErrors(),
				"Function can have a parameter.");
	}

	@Test
	public void validateWithTwoParamsTest() {
		final MessageSet result = validateTest(Arrays.asList("STR", "STR2"));
		Assert.assertTrue(result.hasAnyErrors(), "Function has one parameter.");
	}

	@Test
	public void validateWithMaxParamErrorTest() {
		final MessageSet result = validateTest(Arrays
				.asList(IssuesAssignedToDeletedUsers.MAX_LIMIT_VALUE + 1 + ""));
		Assert.assertTrue(result.hasAnyErrors(),
				"Function parameter must be leather than: "
						+ IssuesAssignedToDeletedUsers.MAX_LIMIT_VALUE);
	}

	@Test
	public void validateWithMinParamErrorTest() {
		final MessageSet result = validateTest(Arrays
				.asList(IssuesAssignedToDeletedUsers.MIN_LIMIT_VALUE - 1 + ""));
		Assert.assertTrue(result.hasAnyErrors(),
				"Function parameter must be greather than: "
						+ IssuesAssignedToDeletedUsers.MIN_LIMIT_VALUE);
	}

	@Test
	public void validateInvalidParamsTest() {
		final MessageSet result = validateTest(Arrays.asList("STR"));
		Assert.assertTrue(result.hasAnyErrors(),
				"Function parameter is integer value.");
	}

	private MessageSet validateTest(final List<String> args) {
		when(mockFunctionOperand.getArgs()).thenReturn(args);
		final MessageSet result = testInstance.validate(mockUser,
				mockFunctionOperand, mockTerminalClause);
		return result;
	}

	@Test
	public void getValuesWithoutParamTest() throws SearchException {
		final List<QueryLiteral> result = getValuesTest(
				new ArrayList<String>(), new ArrayList<Issue>());

		Assert.assertEquals(result.size(), 0);
	}

	@Test
	public void getValuesWitParamTest() throws SearchException {
		Issue issue = mock(Issue.class);
		when(issue.getAssigneeId()).thenReturn("ASSIGNEE_ID");

		final List<Issue> issues = new ArrayList<Issue>();
		issues.add(issue);

		final List<QueryLiteral> result = getValuesTest(Arrays.asList("100"),
				issues);

		Assert.assertEquals(result.size(), issues.size());
	}

	private List<QueryLiteral> getValuesTest(final List<String> args,
			final List<Issue> issues) throws SearchException {

		final Query query = mock(Query.class);
		final ParseResult parseResult = new ParseResult(query,
				new MessageSetImpl());
		final SearchResults searchResult = mock(SearchResults.class);
		when(mockSearchService.parseQuery(any(User.class), anyString()))
				.thenReturn(parseResult);
		when(
				mockSearchService.search(any(User.class), any(Query.class),
						any(PagerFilter.class))).thenReturn(searchResult);

		when(searchResult.getIssues()).thenReturn(issues);

		when(mockFunctionOperand.getArgs()).thenReturn(args);

		final List<QueryLiteral> result = testInstance
				.getValues(moQueryCreationContext, mockFunctionOperand,
						mockTerminalClause);

		return result;

	}

	@Test
	public void minimumParametersTest() {
		Assert.assertEquals(testInstance.getMinimumNumberOfExpectedArguments(),
				0);
	}

}
