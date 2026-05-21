# Test and Quality Reports

This module publishes its quality and testing evidence in two ways:

## 1. CI artifacts

Every CI run uploads the following artifacts:

- `build/reports/tests/test` for the JUnit report
- `build/reports/jacoco/test` for the JaCoCo report
- `build/site/serenity` for the Serenity functional-test report

These artifacts are available from the GitHub Actions run summary for the `CI/CD Pipeline` workflow.

## 2. Public GitHub Pages report bundle

The `Publish Test Reports` workflow generates and publishes a static site that contains:

- `JaCoCo Coverage Report`
- `JUnit Test Report`
- `Serenity Functional Test Report`

After the workflow succeeds, open the deployed GitHub Pages environment from the workflow run. The landing page links to all three reports.

## Local generation

To generate the reports locally:

```bash
./gradlew clean test jacocoTestReport aggregate
```

Output locations:

- JaCoCo: `build/reports/jacoco/test/html/index.html`
- JUnit: `build/reports/tests/test/index.html`
- Serenity: `build/site/serenity/index.html`
