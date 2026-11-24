# Baseline test run (before any capstone change)

Command: `JAVA_HOME=<dragonwell-21> ./gradlew.bat :app:testDebugUnitTest --offline`
Date: 2026-11-24. Commit: d65a79d (baseline import, TipPool 1.2.0).

Result: 27 tests, **4 failed**, all in `CateringLogicTest` (catering features, not the tip pool):

- buildHandoffSections_otherIncludesPastaSaladsAndOtherExtrasOnly
- buildCondensedSupplyLines_omitsSaladNamesAndSoup
- buildHandoffSections_saladsSectionConsolidatesWrapsAndGardenDressings
- formatHandoffMessage_usesSectionHeadersAndHeadsUp

The tip pool (`TipPoolViewModel.calculate()`) has **zero** tests. `ExampleUnitTest` is `2+2`.

Decision: these 4 are pre-existing, unrelated to the capstone, and left as a documented
non-goal. The capstone's green bar is the tip-pool test class run on its own:
`./gradlew.bat :app:testDebugUnitTest --tests "com.tippool.TipPoolMathTest"`.
