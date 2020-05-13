# Test Reporting using Requirement Test Mapper

### Steps to generate the Requirement-Test mapping
1. Generate the Test-Story mapping:
    - Start the sbt shell by command `sbt -DgenerateStoryReport=true` and run `clean`.
    - Run the tests

2. Generate the Story-Requirement mapping from JIRA:
    - Go to the *story-requirement mapping filter* in Jira - https://tmt-project.atlassian.net/issues/?filter=17406
        select storyid and requirement from `columns`
    - Export a Google sheet by using the option shown in the image
        ![screenshot](./filter.png)
    - Remove the first row from the sheet which containing the headings of the columns.
    - Export the sheet into a CSV file (`File -> Download -> Comma-Separated values`).

3. Call the TestRequirementMapper from the bash shell by executing command with following arguments
    - test-story mapping file path (generated using test reporter)
    - story requirement mapping file path (as per above requirements)
    - output path : `./target/RTM/output.txt`
    ```
    > ./scripts/coursier launch -r jitpack -r https://jcenter.bintray.com com.github.tmtsoftware:rtm_2.13:78dd097b7a -M tmt.test.reporter.TestRequirementMapper -- <path of file containing Test-Story mapping > <path of file containing Story-Requirement mapping> <output path>
    ```
4. Import the generated Requirement-Test mapping in Google sheet.
    - Go to Google sheet and import the file (`File -> Import`) and choose the file.
    - Select a `Separator type` as `Custom` and paste a PIPE `|` in the text box.
    - Import the data.

This will generate Requirement-Story-Test mapping in a Google sheet.


##  How to update the Story-Requirement file for Jenkins builds?

 1. Follow step 2 from the [How to generate reports Manually?](#how-to-generate-reports-manually?)
 2. Copy the data of CSV file into a file `./storyRequirementMapping.csv` in the repo.
 3. Commit the changed file and push the commit to github. This will trigger the jenkins build and
data will be generated.

The generated reports for any specific build can be seen at `Test-Story-Requirement mapping` -> `testRequirementMapping.txt`.