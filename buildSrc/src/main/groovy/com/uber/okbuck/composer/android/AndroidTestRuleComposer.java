package com.uber.okbuck.composer.android;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.core.util.RobolectricUtil;
import com.uber.okbuck.template.android.AndroidTestRule;
import com.uber.okbuck.template.core.Rule;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AndroidTestRuleComposer extends AndroidBuckRuleComposer {

  private static final Set<String> ANDROID_TEST_LABELS =
      ImmutableSet.of("unit", "android", "robolectric");

  private AndroidTestRuleComposer() {
    // no instance
  }

  public static Rule compose(
      AndroidLibTarget target,
      List<String> deps,
      final List<String> aidlRuleNames,
      String appClass) {

    List<String> testDeps = new ArrayList<>(deps);
    testDeps.add(":" + src(target));
    testDeps.addAll(external(getExternalDeps(target.getTest(), target.getTestProvided())));
    testDeps.addAll(targets(getTargetDeps(target.getTest(), target.getTestProvided())));

    List<String> testAptDeps = new ArrayList<>();
    testAptDeps.addAll(external(target.getTestApt().getExternalDeps()));
    testAptDeps.addAll(targets(target.getTestApt().getTargetDeps()));

    Set<String> providedDeps = new LinkedHashSet<>();
    providedDeps.addAll(
        external(getExternalProvidedDeps(target.getTest(), target.getTestProvided())));
    providedDeps.addAll(targets(getTargetProvidedDeps(target.getTest(), target.getTestProvided())));
    providedDeps.add(D8Util.RT_STUB_JAR_RULE);

    AndroidTestRule androidTest =
        new AndroidTestRule()
            .srcs(target.getTest().getSources())
            .exts(target.getTestRuleType().getSourceExtensions())
            .apPlugins(getApPlugins(target.getTestApPlugins()))
            .aptDeps(testAptDeps)
            .providedDeps(providedDeps)
            .resources(target.getTest().getJavaResources())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .exportedDeps(aidlRuleNames)
            .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
            .options(target.getMain().getJvmArgs())
            .jvmArgs(target.getTestOptions().getJvmArgs())
            .env(target.getTestOptions().getEnv())
            .robolectricManifest(fileRule(target.getManifest()))
            .runtimeDependency(RobolectricUtil.ROBOLECTRIC_CACHE);

    if (target.getTestRuleType().equals(RuleType.KOTLIN_ROBOLECTRIC_TEST)) {
      androidTest = androidTest.language("kotlin").extraKotlincArgs(target.getKotlincArguments());
    }

    return androidTest
        .ruleType(target.getTestRuleType().getBuckName())
        .defaultVisibility()
        .deps(testDeps)
        .name(test(target))
        .labels(ANDROID_TEST_LABELS)
        .extraBuckOpts(target.getExtraOpts(RuleType.ROBOLECTRIC_TEST));
  }
}
