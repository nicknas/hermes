package com.openbank.util;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.TestAnnotations;
import net.thucydides.core.model.DataTable;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

import com.openbank.util.aws_api.S3API;

import static net.thucydides.core.model.Stories.findStoryFrom;
import static net.thucydides.core.model.TestResult.IGNORED;
import static net.thucydides.core.model.TestResult.PENDING;
public class HermesStepListener implements StepListener {

    /**
     * Used to build the test outcome structure as the test step results come in.
     */
    private final List<TestOutcome> testOutcomes;
    private Story testedStory;
    /**
     * The Java class (if any) containing the tests.
     */
    private Class<?> testSuite;
    private List<String> storywideIssues;
    private List<TestTag> storywideTags;
    private static final TestOutcome UNAVAILABLE_TEST_OUTCOME = new TestOutcome("Test outcome unavailable"); // new UnavailableTestOutcome("Test outcome unavailable");
    private Map<String, TestOutcome> testsWithJiraId;

    private TestOutcome unavailableTestOutcome() {
        return UNAVAILABLE_TEST_OUTCOME;
    }

    private void clearStorywideTagsAndIssues() {
        storywideIssues.clear();
        storywideTags.clear();
    }

    private void recordNewTestOutcome(String testMethod, TestOutcome newTestOutcome) {
        newTestOutcome.setTestSource(StepEventBus.getEventBus().getTestSource());
        testOutcomes.add(newTestOutcome);
        setAnnotatedResult(testMethod);
    }

    private void setAnnotatedResult(String testMethod) {
        if (TestAnnotations.forClass(testSuite).isIgnored(testMethod)) {
            getCurrentTestOutcome().setAnnotatedResult(IGNORED);
        }
        if (TestAnnotations.forClass(testSuite).isPending(testMethod)) {
            getCurrentTestOutcome().setAnnotatedResult(PENDING);
        }
    }


    protected TestOutcome getCurrentTestOutcome() {
        return latestTestOutcome().orElse(unavailableTestOutcome());
    }

    public List<TestOutcome> getTestOutcomes() {
        return testOutcomes.stream()
                .sorted((o1, o2) -> {
                    String creationTimeAndName1 = o1.getStartTime() + "_" + o1.getName();
                    String creationTimeAndName2 = o1.getStartTime() + "_" + o1.getName();
                    return creationTimeAndName1.compareTo(creationTimeAndName2);
                })
                .collect(Collectors.toList());
    }

    public java.util.Optional<TestOutcome> latestTestOutcome() {
        if (testOutcomes.isEmpty()) {
            return java.util.Optional.empty();
        } else {
            TestOutcome latestOutcome = testOutcomes.get(testOutcomes.size() - 1);
            return java.util.Optional.of(latestOutcome);
        }
    }

    public HermesStepListener() {
        this.storywideIssues = new ArrayList<>();
        this.storywideTags = new ArrayList<>();
        this.testOutcomes = new ArrayList<>();
        this.testsWithJiraId = new HashMap<>();
    }

    /**
     * Start a test run using a test case or a user story.
     * For JUnit tests, the test case should be provided. The test case should be annotated with the
     * Story annotation to indicate what user story it tests. Otherwise, the test case itself will
     * be treated as a user story.
     * For easyb stories, the story class can be provided directly.
     *
     * @param storyClass
     */
    @Override
    public void testSuiteStarted(Class<?> storyClass) {
        testSuite = storyClass;
        testedStory = findStoryFrom(storyClass);
        clearStorywideTagsAndIssues();
    }

    /**
     * Start a test run using a specific story, without a corresponding Java class.
     *
     * @param story
     */
    @Override
    public void testSuiteStarted(Story story) {
        testSuite = null;
        testedStory = story;
        clearStorywideTagsAndIssues();
    }

    /**
     * End of a test case or story.
     */
    @Override
    public void testSuiteFinished() {
        this.testsWithJiraId.forEach((String jiraId, TestOutcome result) -> {
            JiraXrayApi.updateTestStatus(result.getResult(), jiraId);
            String fileReportName = result.withQualifier(null).getHtmlReport();
            try {
                String filePath = DataSource.propertyReadSerenity("serenity.outputDirectory") + File.separator + fileReportName;
                File fileReport = new File(filePath);
                File fileReportBucket = new File(DataSource.propertyReadSerenity("serenity.outputDirectory") + File.separator + "report.html");
                if (fileReportBucket.exists()) {
                    fileReportBucket.delete();
                }
                if (fileReportBucket.createNewFile()) {
                    FileUtils.copyFile(fileReport, fileReportBucket);
                    String date = result.getEndTime().toString();
                    date = date.replace("/", "-");
                    URL bucketURL = new URL(DataSource.propertyReadSerenity("s3.bucket.url"));
                    Document doc = Jsoup.parse(fileReportBucket, "UTF-8");
                    Elements tagsWithHref = doc.head().getElementsByAttribute("href");
                    for (Element tagHref : tagsWithHref) {
                        tagHref.attr("href", bucketURL.toString() + tagHref.attr("href"));
                    }
                    Elements tagsWithSrc = doc.head().getElementsByAttribute("src");
                    for (Element tagSrc : tagsWithSrc) {
                        tagSrc.attr("src", bucketURL.toString() + tagSrc.attr("src"));
                    }
                    Elements tagsWithImg = doc.body().getElementsByTag("img");
                    for (Element tagSrc : tagsWithImg) {
                        if (tagSrc.hasAttr("src"))
                            tagSrc.attr("src", bucketURL.toString() + tagSrc.attr("src"));
                    }
                    FileUtils.writeStringToFile(fileReportBucket, doc.outerHtml(), "UTF-8");
                    S3API.uploadReport(jiraId + "/" + date + "/" + "report.html", DataSource.propertyReadSerenity("s3.bucket.name"), fileReportBucket);
                    JiraXrayApi.updateDescriptionWithS3Report(jiraId, bucketURL + jiraId + "/" + URLEncoder.encode(date, "UTF-8") + "/report.html");
                    FileUtils.deleteDirectory(new File(DataSource.propertyReadSerenity("serenity.tempFeaturesDirectory")));
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * A test with a given name has started.
     *
     * @param description
     */
    @Override
    public void testStarted(String description) {
        TestOutcome newTestOutcome = TestOutcome.forTestInStory(description, testSuite, testedStory);
        recordNewTestOutcome(description, newTestOutcome);
    }

    @Override
    public void testStarted(String description, String id) {
        TestOutcome newTestOutcome = TestOutcome.forTestInStory(description, testSuite, testedStory).withId(id);
        recordNewTestOutcome(description, newTestOutcome);
    }

    /**
     * Called when a test finishes.
     *
     * @param result
     */
    @Override
    public void testFinished(TestOutcome result) {
        Map<String, String> metadatas = Serenity.getCurrentSession().getMetaData();
        metadatas.forEach((String key, String value) -> {
            this.testsWithJiraId.put(key, result);
        });
    }

    /**
     * The last test run is about to be restarted
     */
    @Override
    public void testRetried() {

    }

    /**
     * Called when a test step is about to be started.
     *
     * @param description the description of the test that is about to be run
     *                    (generally a class and method name)
     */
    @Override
    public void stepStarted(ExecutedStepDescription description) {

    }

    /**
     * Called when a test step is about to be started, but this step is scheduled to be skipped.
     *
     * @param description the description of the test that is about to be run
     *                    (generally a class and method name)
     */
    @Override
    public void skippedStepStarted(ExecutedStepDescription description) {

    }

    /**
     * Called when a test step fails.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void stepFailed(StepFailure failure) {

    }

    /**
     * Declare that a step has failed after it has finished.
     *
     * @param failure
     */
    @Override
    public void lastStepFailed(StepFailure failure) {

    }

    /**
     * Called when a step will not be run, generally because a test method is annotated
     * with {@link Ignore}.
     */
    @Override
    public void stepIgnored() {

    }

    /**
     * The step is marked as pending.
     */
    @Override
    public void stepPending() {

    }

    /**
     * The step is marked as pending with a descriptive message.
     *
     * @param message
     */
    @Override
    public void stepPending(String message) {

    }

    /**
     * Called when an test step has finished successfully
     */
    @Override
    public void stepFinished() {

    }

    /**
     * The test failed, but not while executing a step.
     *
     * @param testOutcome The test outcome structure for the failing test
     * @param cause       The exception that triggered the failure
     */
    @Override
    public void testFailed(TestOutcome testOutcome, Throwable cause) {

    }

    /**
     * The test as a whole was ignored.
     */
    @Override
    public void testIgnored() {

    }

    /**
     * The test as a whole was skipped.
     */
    @Override
    public void testSkipped() {

    }

    /**
     * The test as a whole should be marked as 'pending'.
     */
    @Override
    public void testPending() {

    }

    @Override
    public void testIsManual() {

    }

    @Override
    public void notifyScreenChange() {

    }

    /**
     * The current scenario is a data-driven scenario using test data from the specified table.
     *
     * @param table
     */
    @Override
    public void useExamplesFrom(DataTable table) {

    }

    /**
     * If multiple tables are used, this method will add any new rows to the test data
     *
     * @param table
     */
    @Override
    public void addNewExamplesFrom(DataTable table) {

    }

    /**
     * A new example has just started.
     *
     * @param data
     */
    @Override
    public void exampleStarted(Map<String, String> data) {

    }

    /**
     * An example has finished.
     */
    @Override
    public void exampleFinished() {

    }

    @Override
    public void assumptionViolated(String message) {

    }

    @Override
    public void testRunFinished() {

    }
}
